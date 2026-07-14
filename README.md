# 多学科会诊中心系统（MDT）— 架构设计与核心实现参考

> 融合 HIS / EMR / LIS 与多模态 PACS（放射 / 超声 / 内镜 / 病理 / 心电）的多学科会诊平台。
> 统一索引存储全序列影像与报告，前端 HTML5 + WebAssembly 零下载阅片，后端微服务 DDD + 全链路审计，分级合规运行。

---

## 一、设计方案说明（≤200 字）

采用**微服务 + DDD 分层**：集成/存储/协同/权限四域经 API 网关对外 RESTful、对内 gRPC。
数据接入层用**适配器模式**对接 HIS/EMR/LIS/PACS 异构接口，并以 DICOM SCP/SCU 拉取全序列影像、数据库中间表/视图归集报告；影像与非影像统一以 `PatientVisitUID` 索引。
前端影像解码用 WebAssembly、渲染用 WebGL/GPU 离主线程（Worker）避免卡顿；协同用 WebRTC SFU + 标注序列化。
权限采用多租户隔离 + 字段级 RBAC，全程 TraceId 审计与日志脱敏（仅留 PatientID + 检查号），满足等保与互联互通标准。

---

## 二、需求 → 6 大核心领域映射

| 文档需求项 | 归属领域 | 说明 |
|---|---|---|
| 数据接口集成（HIS/EMR/LIS/PACS） | ① 数据接入层 | 适配器模式对接异构接口 |
| 影像采集（SCP 接收 / SCU C-Find/Move） | ① 数据接入层 + ② 影像核心引擎 | DICOM 传输与集中存储 |
| 临床数据查阅 / 诊疗调阅 | ① 数据接入层 + ④ 业务流程层 | URL 集成、一键调阅 |
| 患者数据存储（统一索引） | ② 影像核心引擎 + ⑥ 外部扩展层 | PatientVisitUID 统一索引、生命周期 |
| 影像云计算 / 负载均衡 | ⑥ 外部扩展层 | 轮询/加权轮询/最少连接/源地址散列 + WADO |
| 影像调阅与处理（MPR/窗宽窗位/测量） | ② 影像核心引擎 | WebGL GPU 加速、Worker 离主线程 |
| 会诊协同 / 电子白板 / 桌面共享 | ③ 协同通讯层 | WebRTC SFU 转发、标注序列化 |
| 音视频交互 | ③ 协同通讯层 | 多方音视频、质量自适应 |
| 会诊状态机（申请→通知→确认→进行中→总结） | ④ 业务流程层 | 短信异步队列通知 |
| 系统/医疗单位/专家管理 | ⑤ 权限租户管理 | 多机构隔离、字段级 RBAC |
| 区域专家标记 / 影像云引擎 | ⑥ 外部扩展层 | SPI 可插拔扩展 |

---

## 三、技术选型理由

| 维度 | 选型 | 理由 |
|---|---|---|
| 前端影像解码 | WebAssembly (WASM) | 零下载、跨端（Chrome90+/Firefox/Safari/移动 WebView），禁用 ActiveX/NPAPI |
| 前端渲染 | WebGL2 + GPU | MPR/窗宽窗位/测量 GPU 加速，渲染放 Worker 避免阻塞主线程 |
| 协同传输 | WebRTC + SFU | 多方音视频低延迟；SFU 节省端上行带宽，优于 Mesh |
| 后端语言 | Java / .NET Core | DDD（XxxService/XxxRepository）成熟生态，团队复用 |
| 内部通信 | gRPC（Protobuf） | 强类型、低延迟、流式（影像进度/信令） |
| 外部通信 | RESTful + HTTPS | 与 HIS/EMR 集成平台标准对接 |
| 负载均衡 | 自研策略枚举 | 轮询/加权轮询/最少连接/源地址散列，可热切换 |
| 数据隔离 | 多租户 + PatientVisitUID | 影像与非影像共用统一就诊唯一标识 |
| 可观测 | TraceId + 审计门面 | 全链路追踪、关键操作留痕、合规审计 |

---

## 四、目录结构

```
/workspace
├── README.md                 # 本文件：总览 + 设计方案
├── docs/
│   └── architecture.md       # 6 域详细架构、DDD 分层、部署注意
├── proto/
│   └── mdt.proto             # 微服务 gRPC/REST 接口定义
├── backend/                  # 后端参考实现（Java / DDD）
│   ├── loadbalancer/         # 4 种负载均衡策略
│   ├── integration/          # DICOM SCP/SCU 适配器 + HIS/EMR 适配
│   ├── workflow/             # 会诊状态机 + 短信异步队列
│   ├── security/             # 字段级 RBAC + 多租户 + TraceId 审计
│   ├── storage/              # 中间表/视图 DDL + 索引
│   └── ext/                  # SPI 云影像 WADO 扩展
└── frontend/                 # 前端参考实现（Vue/React + TS）
    ├── types/                # 强类型接口定义
    ├── viewer/               # WebGL MPR/窗宽窗位（Worker 离主线程）
    └── collab/               # 标注序列化 + WebRTC SFU 信令
```

---

## 五、快速开始（参考）

```bash
# 1) 启动依赖（PostgreSQL/Oracle 兼容存储、Redis、消息队列）
docker compose -f deploy/docker-compose.yml up -d

# 2) 生成 gRPC 代码
protoc -I proto --java_out=backend --ts_out=frontend proto/mdt.proto

# 3) 各微服务经 API 网关暴露（见 docs/architecture.md 端口表）
```

> 本仓库为**架构与核心实现参考骨架**，聚焦最难、最易被低估的跨域与合规要点（DICOM 传输、负载均衡、状态机、字段级权限、WebGL 离主线程、全链路审计）。业务 CRUD 与 UI 细节按团队规范扩展即可。
