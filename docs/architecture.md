# 多学科会诊中心系统 — 详细架构设计

> 严格按 6 大核心领域拆分，域间**禁止直接耦合**，一律经 **API 网关 + gRPC** 通信。
> 本文档覆盖：领域边界、DDD 分层、全链路追踪/脱敏、合规安全、部署注意事项。

---

## 0. 总体架构

```
┌──────────────────────── 客户端（浏览器 / 移动 WebView）────────────────────────┐
│  HTML5 + WASM 解码  │  WebGL2 GPU 阅片(Worker)  │  WebRTC 协同(白板/桌面共享)      │
└───────────────────────────────┬───────────────────────────────────────────────┘
                                 │ HTTPS / WADO-RS
┌────────────────────────── API 网关（鉴权/限流/路由/TraceId 注入）──────────────────┐
└───┬──────────┬───────────┬────────────┬────────────┬─────────────┬────────────┘
    │ gRPC     │ gRPC      │ gRPC       │ gRPC       │ gRPC        │ gRPC(SPI)
┌───┴───┐ ┌────┴────┐ ┌────┴─────┐ ┌────┴─────┐ ┌────┴──────┐ ┌────┴────────┐
│①数据  │ │②影像   │ │③协同    │ │④业务    │ │⑤权限租户│ │⑥外部扩展  │
│接入层 │ │核心引擎 │ │通讯层   │ │流程层   │ │管理     │ │(云影像WADO)│
└───┬───┘ └────┬────┘ └────┬─────┘ └────┬─────┘ └────┬──────┘ └────┬────────┘
    │DICOM TLS  │           │WebRTC/SFU  │短信MQ      │OAuth2/字段RBAC│
    ▼           ▼           ▼            ▼            ▼              ▼
 HIS/EMR/LIS  PACS/文件存储  Redis PubSub  MQ(短信)   统一身份/审计库  影像云引擎
 (适配器)    (中间表/视图)  (信令/标注)   (异步队列)  (多租户隔离)    (SPI可插拔)
```

**跨域铁律**：①~⑥ 互不直连；前端只认 API 网关；后端域间通过 gRPC Client 调用，且调用方不感知被调方数据库。

---

## 1. ① 数据接入层（适配器模式）

**目标**：异构 HIS/EMR/LIS/PACS 统一适配为内部标准化事件，屏蔽协议差异。

- **HIS/EMR/LIS**：RESTful（WebService 风格）+ 数据库中间表/视图两种模式。适配器轮询视图或消费消息。
- **PACS（放射/超声/内镜/病理/心电）**：
  - **SCP（接收）**：作为 DICOM Storage SCP，接收设备 C-STORE 推送全序列影像。
  - **SCU（拉取）**：主动 `C-FIND`（查询）→ `C-MOVE`（拉取）来自设备的影像与报告。
  - 非 DICOM 模态（超声/内镜/病理 JPEG、PDF，心电 JPEG/PDF）走 FTP/LAN 直连采集。
- **统一索引**：所有采集数据落库时强制写入 `PatientVisitUID`，影像文件与索引/报告共用该唯一键。
- **生命周期**：存储服务定时任务执行归档（冷存储）/清理（超期销毁），符合留存策略。

## 2. ② 影像核心引擎

- **零下载 Web 阅片**：WADO-RS / WADO-URI 流式取帧，WASM 解码（如 dicom-py / openjpeg），主线程只做呈现。
- **WebGL2 GPU 加速**：MPR 多平面重建、窗宽窗位（Window/Level）、长度/角度测量、序列对比、旋转/镜像均在 GPU 着色器完成；重计算（体绘制、重建）放入 **Web Worker**，经 `OffscreenCanvas`/`SharedArrayBuffer` 回传，避免阻塞主线程。
- **存储形态**：索引/诊断信息入关系库；影像文件入对象存储（S3 兼容），路径经中间表关联。

## 3. ③ 协同通讯层

- **WebRTC SFU**：多方会诊由 SFU 转发音视频，终端仅上行一路、下行 N-1 路，节省带宽。
- **电子白板 / 桌面共享 / 标注**：以**增量消息**形式经 WebSocket/DataChannel 广播；标注对象序列化为 Protobuf/JSON 后由 `AnnotationSerializer` 编码，支持回放与持久化。
- **音视频质量自适应**：根据带宽动态切换 simulcast 层级（高/中/低）。

## 4. ④ 业务流程层

**会诊状态机闭环**：`申请(APPLIED) → 通知(NOTIFIED) → 确认(CONFIRMED) → 进行中(IN_PROGRESS) → 总结(COMPLETED)`，另含 `已取消(CANCELLED)`。

- 状态迁移由 `ConsultationStateMachine` 守卫，非法迁移抛 `IllegalStateTransitionException`。
- 进入 `通知` 时投递**短信异步队列**（MQ），专家确认前会诊不开始（保证全员确认）。
- 进入 `总结` 后落库多方专家意见，后续可随时调阅。

## 5. ⑤ 权限租户管理

- **多租户隔离**：以 `TenantId`（医疗机构）为所有表分区/行级过滤条件；跨机构默认不可见，区域会诊需显式授权。
- **字段级 RBAC**：角色→资源→字段 三级授权；如"心电技士"可见心电数据但不可见病理结论字段。
- **统一身份与审计**：OAuth2/JWT 鉴权；所有关键操作（调阅、下载、标注、状态迁移）写审计表，含 `TraceId`、`OperatorId`、`TenantId`、`PatientVisitUID`（脱敏后仅 PatientID+检查号）。

## 6. ⑥ 外部扩展层（SPI）

- **影像云引擎**：定义 `CloudImageProvider` SPI 接口，实现类经 ServiceLoader 注册；支持 CT/MRI/CR/DR/DSA/RF/US 即时加载运算。
- **WADO 云扩展**：实现 `WadoGateway` SPI，任意地点经 Web WADO 调阅；后端经**负载均衡策略**并发运算（轮询/加权轮询/最少连接/源地址散列）。

---

## 7. 全链路追踪 / 日志脱敏 / 合规

- **TraceId**：网关入口生成 `TraceId`，经 gRPC metadata / HTTP header 透传至所有域与日志 MDC。
- **脱敏规则**：日志与审计中**禁止**出现姓名、身份证号；仅记录 `PatientID` + `检查号(AccessionNumber)`。
  - 实现：统一日志门面 `MdtLogger`，在 append 前调用 `Desensitizer.mask()`。
- **传输**：对外 HTTPS（≥TLS1.2）；DICOM 走 DICOM-TLS（TLS）；内部 gRPC 可 mTLS。
- **留存/清理**：存储生命周期定时任务按法规留存期归档/清理，清理留审计痕迹。

---

## 8. 部署注意事项

### 8.1 端口占用

| 服务 | 端口 | 协议 | 说明 |
|---|---|---|---|
| API 网关 | 443 / 8443 | HTTPS | 对外统一入口 |
| 集成服务 | 50051 | gRPC | 对内 |
| DICOM SCP | 11112 | DICOM-TLS | 设备 C-STORE 推送（AE Title: `MDT_SCP`） |
| 影像核心引擎 | 50052 | gRPC | WADO 取帧 |
| 协同服务 | 50053 + 3478/udp(STUN) + 5004/udp(TURN) | gRPC/WebRTC | SFU 媒体 |
| 权限服务 | 50054 | gRPC | 鉴权/审计 |
| 外部扩展 | 50055 | gRPC(SPI) | 云影像 |

### 8.2 环境变量

```bash
# 数据传输
DICOM_SCP_AE_TITLE=MDT_SCP          # 本系统 AE Title
DICOM_SCU_CALLED_AE=PACS_SERVER      # 对端 PACS AE Title
DICOM_SCU_REMOTE_HOST=10.20.0.10
DICOM_SCU_REMOTE_PORT=11112
DICOM_TLS_ENABLED=true               # DICOM-TLS 开关
WADO_BASE_URL=https://pacs.internal/wado-rs

# 数据库（Oracle / SQL Server 兼容，通过中间表/视图接入）
DB_DRIVER=oracle|sqlserver
DB_JDBC_URL=jdbc:oracle:thin:@//db:1521/ORCL
DB_USER=mdt_app
DB_PASSWORD=***                      # 走密钥管理，勿明文

# 多租户 / 权限
TENANT_STRICT_ISOLATION=true         # 严格行级隔离
RBAC_FIELD_LEVEL=true

# 可观测
TRACE_ID_HEADER=X-Mdt-TraceId
LOG_DESENSITIZE=true                 # 日志脱敏总开关
AUDIT_ENABLED=true

# 负载均衡（影像云运算调度）
LB_STRATEGY=LEAST_CONNECTIONS       # ROUND_ROBIN|WEIGHTED_ROUND_ROBIN|LEAST_CONNECTIONS|SOURCE_HASH

# 短信 MQ
SMS_MQ_TOPIC=mdt.sms.notify
```

### 8.3 AE Title 配置要点

- 本系统 SCP 固定 `MDT_SCP`，需在 PACS/设备侧互信列表登记，并交换 DICOM-TLS 证书。
- SCU 拉取时 `Called AE Title` 必须与对端 PACS 配置一致，否则 C-FIND/C-MOVE 被拒。
- 多院区部署时建议 `MDT_SCP_<HOSPITAL>`，避免 AE Title 冲突。

### 8.4 高并发与合规提示

- 影像取帧走 WADO 流式 + 边缘缓存，避免重复解码。
- 协同信令走 Redis Pub/Sub 分片；SFU 媒体独立扩容，与业务域解耦。
- 审计写入异步批量落库，不阻塞主流程；敏感字段落库前加密（字段级）。
- 多租户下所有 SQL 必须带 `TenantId` 过滤并建组合索引（见 `storage/schema.sql`）。

---

## 9. 单元 / 压测用例建议（汇总，详见各代码文件头部）

| 域 | 用例 |
|---|---|
| 负载均衡 | 各策略权重/散列正确性；节点上下线后路由收敛 |
| DICOM 适配器 | C-FIND/C-MOVE 成功与超时；SCP 接收非法 AE 拒绝 |
| 状态机 | 合法迁移通过；非法迁移抛异常；全员确认门控 |
| 字段级 RBAC | 越权字段访问被拦截；跨租户不可见 |
| 影像引擎 | 大数据序列解码不阻塞主线程（Worker）；窗宽窗位 GPU 着色正确 |
| 协同 | 标注序列化/反序列化一致；SFU 断线重连 |
| 审计 | TraceId 全链路贯通；日志无姓名/身份证泄漏 |
