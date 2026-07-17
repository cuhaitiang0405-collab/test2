# MDT 外部对接模拟服务器 — 产品需求文档

> 版本：v1.0 | 运行环境：Windows 10 LTSC | 形态：独立可执行 jar

---

## 一、产品概述

`mdt-mock-server` 是一个独立的外部系统模拟工具，用于在 MDT 系统没有真实 PACS/HIS/认证中心等外部依赖时，提供完整的仿真交互环境。支持 HTTP REST 和 DICOM 双协议，YAML 规则配置 + Web 管理面板。

---

## 二、模拟的外部系统清单

| 优先级 | 外部系统 | 模拟协议 | 端口 | 需要第三方付费 API |
|--------|---------|---------|------|-------------------|
| P0 | PACS 影像归档系统 | DICOM (C-ECHO / C-FIND / C-MOVE) | 11112 | ❌ |
| P0 | HIS/EMR 临床信息系统 | HTTP REST | 9876 | ❌ |
| P1 | 统一认证中心 | HTTP (OAuth2 / JWT) | 9876 | ❌ |
| P2 | 云影像区域平台 | HTTP REST | 9876 | ❌ |
| P1 | **短信网关** | HTTP REST | 9876 | **🟡 阿里云 SMS / 腾讯云 SMS** |

---

## 三、功能需求

### 3.1 规则配置系统

- **YAML 配置文件**：启动时加载 `rules/` 目录下所有 `.yml`，定义默认匹配规则
- **Web 管理面板**（端口 9877）：运行时查看规则命中统计、热修改规则
- 每条规则结构：

```yaml
id: "rule-ct-abdomen"
protocol: DICOM            # DICOM | HTTP
method: C-FIND             # C-FIND | C-MOVE | C-ECHO | GET | POST | PUT
match:                     # 匹配条件（正则）
  patientId: "P.*"
  modality: "CT"
response:                  # 响应内容
  status: SUCCESS           # SUCCESS | ERROR
  studyCount: 3
  delay: 1500               # 模拟延迟(ms)，默认 0
```

- 支持异常场景规则：`status: ERROR` + 错误码（模拟 PACS 宕机、认证失败、HIS 超时等）

### 3.2 DICOM 协议模拟

| 操作 | 模拟行为 |
|------|---------|
| C-ECHO | 返回成功确认或模拟离线 |
| C-FIND | 按 PatientID / AccessionNumber 匹配规则，返回 Study 列表 |
| C-MOVE | 按 StudyInstanceUID 传输合成 DICOM 数据集 |

**不需要 SCP（C-STORE 接收推送）**——SCP 是 MDT 自身行为，不在外部模拟范围。

### 3.3 HTTP REST 模拟

- 可配置 URL 路径匹配（`/api/his/patient/*`）
- 支持 GET / POST / PUT / DELETE 方法
- 响应内容为 JSON，可从 YAML 内联或引用外部 `responses/` 目录文件
- 模拟认证端点：`POST /oauth2/token` → 返回 JWT
- 模拟短信端点：`POST /sms/send` → 返回成功 + 第三方 API 调用占位

### 3.4 Web 管理面板

- **仪表盘**：请求总数、规则命中/未命中比例、最近 20 条请求摘要
- **规则列表**：展示所有已加载规则，每条显示命中次数、最近命中时间
- **规则编辑**：运行时修改匹配条件或响应内容，无需重启
- **异常注入**：一键切换某规则的响应状态（正常 → 错误），用于测试异常处理

### 3.5 审计日志

- 格式：每行 JSON，写入 `.\logs\mock-YYYY-MM-DD.json`
- 字段：时间戳、协议、方法、远端 IP、命中规则 ID、PatientID（脱敏）、响应状态码、耗时(ms)
- 示例：

```json
{"ts":"2026-07-17T10:00:01","proto":"DICOM","method":"C-FIND","remote":"127.0.0.1",
 "ruleId":"rule-ct-abdomen","patientId":"P1001","status":"SUCCESS","latencyMs":12}
```

- 不记录完整 payload（DICOM 数据集 / HIS JSON body），避免日志膨胀

---

## 四、非功能需求

| 指标 | 要求 |
|------|------|
| 运行环境 | Windows 10 LTSC，JDK 20 |
| 启动方式 | `java -jar mdt-mock-server-1.0.0.jar`，或 `startup.bat` |
| 启动时间 | < 3 秒 |
| C-FIND 响应 | < 50ms（不含模拟延迟） |
| C-MOVE 传输 | 模拟 1-3 秒延迟 |
| 并发 | 单实例（测试人员单人操作，无需集群） |
| 端口 | DICOM 11112 / HTTP 9876 / Web 管理 9877 |
| 存储 | 无外部数据库依赖，YAML + 内存 + 滚动日志文件 |

---

## 五、项目结构

```
mock-server/
├── pom.xml
├── startup.bat                     # Windows 一键启动
├── src/main/resources/
│   ├── application.yml             # 端口、日志路径
│   └── rules/
│       ├── pacs.yml                # DICOM 模拟规则
│       ├── his-emr.yml             # HIS/EMR 规则
│       ├── auth.yml                # 认证规则
│       ├── sms.yml                 # 短信网关规则
│       └── cloud-image.yml         # 云影像规则
├── src/main/java/com/mdt/mock/
│   ├── MockServerApp.java          # Spring Boot 入口
│   ├── engine/                     # RuleEngine：YAML 解析 + 匹配
│   │   ├── RuleEngine.java
│   │   └── RuleDefinition.java
│   ├── protocol/                   # 协议适配层
│   │   ├── http/HttpMockController.java
│   │   └── dicom/DicomMockServer.java
│   ├── audit/AuditLogger.java      # JSON 日志写文件
│   └── web/DashboardController.java # Web 管理面板 API
```

---

## 六、与 MDT 主系统的对接方式

MDT 的 `mdt-integration` 模块（端口 8082）将外部调用目标指向 mock-server：

| MDT 原本对接 | 指向 mock-server |
|-------------|-----------------|
| PACS AE_TITLE | `localhost:11112` |
| HIS/EMR base URL | `http://localhost:9876` |
| 认证中心 | `http://localhost:9876/oauth2` |
| 云影像 WADO | `http://localhost:9876/wado` |

配置通过 MDT 各模块的 `application.yml` 或环境变量切换，不修改 mock-server 代码。

---

## 七、风险与限制

| 项 | 说明 |
|----|------|
| DICOM 库依赖 | 不引入 dcm4che3（太重）。用纯 Java socket 实现 C-ECHO/C-FIND/C-MOVE 最小子集 |
| 短信网关 | 仅模拟接口响应，不实际调用第三方 API——规则标注 `🟡 需付费` |
| 影像数据 | 返回合成 DICOM 数据集（复用 MDT 的 `DicomSimulator` 逻辑） |
| HTTPS | 本版仅 HTTP/DICOM 明文；生产若需 TLS，通过 nginx 反向代理加证书 |

---

## 八、验收标准

| 验收项 | 通过条件 |
|--------|---------|
| 启动 | `java -jar` 后 3 秒内 HTTP 和 DICOM 端口可访问 |
| C-ECHO | MDT 发 C-ECHO 到 mock-server，返回成功 |
| C-FIND 匹配 | MDT 按规则定义的 patientId 查询，返回匹配的 Study 列表 |
| HTTP HIS 查询 | MDT 调 `GET /api/his/patient/P1001`，返回 YAML 预定义的 JSON |
| 异常模拟 | 切换某规则为 ERROR，MDT 收到对应错误码 |
| 日志 | `.\logs\` 下生成当日 JSON 日志文件，内容格式合规 |
| Web 面板 | 浏览器访问 `http://localhost:9877`，可查看请求统计 |
