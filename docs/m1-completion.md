# M1 里程碑完成报告：脚手架与合规底座

> 目标：搭起六域微服务骨架、API 网关、gRPC 权限服务、TraceId 全链路追踪、日志脱敏与审计，并交付医疗蓝 UI 登录+工作台预览。

---

## 1. 设计方案说明（M1 聚焦，≤200字）

M1 先建**合规底座**而非一次性堆功能：Spring Boot 父工程切出 6 域模块（auth/gateway + 5 个 stub），对外统一走 API 网关（REST），域间预留 gRPC（如 ⑤ 权限服务已实现 `AuthService`）。TraceId 通过网关响应式 GlobalFilter + Servlet Filter + gRPC ServerInterceptor 三层透传；审计日志与日志门面统一脱敏（姓名/身份证→掩码）。Vue3+TS 前端走医疗蓝设计系统，登录→工作台经 Vite 代理直连网关。Postgres 作为开发库，由 Docker 承载。此举保证后续 M2~M6 每域叠加时都不破坏边界与合规。

---

## 2. 核心接口定义

### 2.1 对外 REST（经网关路由）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | JWT 登录，响应头携带 `X-Mdt-TraceId` |
| GET  | `/api/auth/me` | 当前登录用户信息（校验 Bearer） |
| POST | `/api/patient-visit` | 患者就诊索引写入（字段更新自测） |
| GET  | `/api/patient-visit/{uid}` | 回读患者索引 |

### 2.2 gRPC 权限服务（`mdt-auth/src/main/proto/auth.proto`）

```protobuf
service AuthService {
  rpc CheckFieldPermission(FieldPermissionRequest) returns (FieldPermissionResponse);
  rpc WriteAudit(AuditRecord) returns (Ack);
}
message FieldPermissionRequest { string trace_id=1; string tenant_id=2; string role=3; string resource=4; string field=5; }
message FieldPermissionResponse { bool allowed=1; }
message AuditRecord { string trace_id=1; string tenant_id=2; string operator_id=3; string patient_visit_uid=4; string action=5; int64 ts=6; string detail=7; }
message Ack { bool ok=1; string message=2; }
```

Java 桩代码已预生成到 `mdt-auth/src/main/java/com/mdt/auth/grpc/`，构建不再依赖 `protobuf-maven-plugin`。

---

## 3. 关键代码片段

### 3.1 网关 TraceId 注入/透传（响应式 WebFlux）

```java
// mdt-gateway/src/main/java/com/mdt/gateway/GatewayTraceGlobalFilter.java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst("X-Mdt-TraceId");
    final String tid = (incoming == null || incoming.isBlank())
            ? UUID.randomUUID().toString().replace("-", "") : incoming;
    MDC.put("X-Mdt-TraceId", tid);
    exchange.getResponse().getHeaders().add("X-Mdt-TraceId", tid);
    var mutated = exchange.mutate()
            .request(r -> r.headers(h -> h.add("X-Mdt-TraceId", tid)))
            .build();
    return chain.filter(mutated).then(Mono.fromRunnable(() -> MDC.remove("X-Mdt-TraceId")));
}
```

### 3.2 gRPC TraceId 透传（跨服务端线程）

```java
// mdt-common/src/main/java/com/mdt/common/trace/GrpcTraceInterceptor.java
public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(...) {
    String traceId = headers.get(TRACE_KEY);
    if (traceId == null || traceId.isBlank()) traceId = TraceContext.newTraceId();
    Context ctx = Context.current().withValue(TRACE_CTX, traceId);  // gRPC Context 跨线程 attach
    return Contexts.interceptCall(ctx, call, headers, next);
}
public static String currentTraceId() { ... }  // 审计日志中读取
```

### 3.3 日志脱敏（仅保留 PatientID + 检查号）

```java
// mdt-common/src/main/java/com/mdt/common/security/Desensitizer.java
public static String mask(String raw) {
    String s = raw.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2"); // 身份证
    s = s.replaceAll("(?<=[^\\u4e00-\\u9fa5])([\\u4e00-\\u9fa5]{2,4})(?=[^\\u4e00-\\u9fa5])", "**"); // 姓名
    return s;
}
```

### 3.4 字段级 RBAC（支持通配前缀）

```java
// mdt-auth/src/main/java/com/mdt/auth/security/RbacService.java
public boolean canReadField(String role, String resource, String field) {
    Map<String, Set<String>> res = acl.get(role);
    // ... 精确匹配 / study.* 通配前缀 / 字段匹配
}
```

---

## 4. 单元/压测用例（M1 已落地）

| 用例 | 目标 | 结果 |
|---|---|---|
| `AuthGrpcIntegrationTest.ecgTechDeniedOnPathology` | ECG_TECH 不可读病理结论 | ✅ 通过 |
| `AuthGrpcIntegrationTest.doctorAllowedOnPathology` | DOCTOR 可读全部字段 | ✅ 通过 |
| `DesensitizerTest.masksIdCardAndName` | 身份证/姓名被掩码 | ✅ 通过 |
| `DesensitizerTest.keepsPatientIdAndAccession` | 合法字段保留 | ✅ 通过 |

**压测建议**：用 `wrk`/`k6` 压 `/api/auth/login`，验证网关 8080→权限服务 8081 在 1000 RPS 下 TraceId 头仍回写正确；gRPC 用 `ghz` 压 50054，验证字段级 RBAC 决策 P99 < 1ms。

---

## 5. 部署注意事项

### 5.1 端口占用

| 服务 | 端口 | 说明 |
|---|---|---|
| API 网关 | 8080 | 对外统一 RESTful 入口 |
| 权限服务 | 8081 / gRPC 50054 | 登录、字段级 RBAC、审计 |
| 前端 dev | 5173 | Vite dev server，代理 `/api` → 8080 |
| Postgres | 5432 | Docker 开发库 |

### 5.2 环境变量/配置

```bash
# 权限服务 application.yml
DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mdt
DSPRING_DATASOURCE_USERNAME=mdt_app
DSPRING_DATASOURCE_PASSWORD=mdt_pass
# JWT 密钥（生产必须改，走 KMS）
MDT_JWT_SECRET=mdt-dev-secret-change-me-please-32bytes!!
```

### 5.3 启动命令（M1 本地）

```bash
# 1) 数据库
docker run -d --name mdt-postgres -e POSTGRES_DB=mdt -e POSTGRES_USER=mdt_app -e POSTGRES_PASSWORD=mdt_pass -p 5432:5432 postgres:15

# 2) 后端
mvn -q -pl mdt-auth -am package -DskipTests
mvn -q -pl mdt-gateway -am package -DskipTests
java -jar mdt-auth/target/mdt-auth-1.0.0.jar &
java -jar mdt-gateway/target/mdt-gateway-1.0.0.jar &

# 3) 前端
npm install
npm run dev
```

### 5.4 已知约束

- 当前 `mdt-auth` 预生成 gRPC 桩代码，若 `auth.proto` 变更，需重新运行 `frontend/shot.mjs` 旁的 protoc 脚本（或等 M2 接入 `protobuf-maven-plugin` 镜像）。
- 登录为内存演示用户（doctor/doctor123），M6 权限里程碑会接入医院统一认证/多租户数据隔离。

---

## 6. 预览截图

- `docs/m1-preview/login.png` — 医疗蓝登录页
- `docs/m1-preview/workbench.png` — 工作台（六域导航 + M1 自测面板）
- `docs/m1-preview/selftest.png` — 字段更新自测成功（回读一致）

> **当前状态**：M1 完成，所有服务已后台运行。请确认此预览，许可后进入 **M2 数据接入层**（DICOM SCP/SCU + HIS/EMR Mock 适配）。
