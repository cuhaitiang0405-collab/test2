# M1 代码审查报告：脚手架与合规底座

> 审查目标：确认 M1 脚手架与底座无编译/运行级异常，并找出需修复的真实问题。
> 方法：实跑校验（测试/构建/健康检查）+ 针对性人工走读。

---

## ✅ 选项 A 已落地（2025-07-14）
用户选择「现在就修」，已在进入 M2 前完成三处中危加固，详见 [`docs/m1-a-hardening.md`](./m1-a-hardening.md)：
1. 新增 `@RestControllerAdvice` 全局异常处理器 → 合规错误体 `{code,message,traceId}` + 失败审计，堆栈不外泄。
2. JWT 密钥改为环境变量 `MDT_JWT_SECRET`（带 dev 回退），删除硬编码。
3. 登录改内存用户表 + BCrypt 加盐哈希，禁明文比对、防账户枚举。

验证：auth 模块 **14/14 测试通过**（原 4 + 新增 10），`mvn install` 全 8 模块 BUILD SUCCESS；网关→auth 登录 200、错误口令 401（合规体）、TraceId 透传一致；进程环境已确认含 `MDT_JWT_SECRET`。


## 一、实跑校验结果（全绿）

| 校验项 | 命令/动作 | 结果 |
|---|---|---|
| 后端单测 | `mvn -pl mdt-auth -am test` | ✅ 4/4 通过（gRPC RBAC ×2 + 脱敏 ×2） |
| 全量编译 | `mvn install -DskipTests`（父工程 8 模块） | ✅ 全部产出 jar |
| 前端生产构建 | `vite build` | ✅ 37 模块转译成功 |
| 实时健康 | gateway / auth-login / frontend | ✅ 均为 200 |

**结论先行**：脚手架与底座**无编译/运行级异常**，三层调用链路（前端→网关→权限服务）跑通，TraceId 透传与日志脱敏行为正确。

---

## 二、发现的真实问题（按严重度）

### 🔴 中危（建议进生产前必修）

1. **缺全局异常处理（@RestControllerAdvice）**
   `AuthController` 直接抛 `RuntimeException`（如密码错误），Spring 默认返回 500 + 堆栈。生产前需统一异常处理器，返回合规错误体并补审计。
   - 位置：`mdt-auth/.../rest/AuthController.java`、`mdt-common` 缺 `GlobalExceptionHandler`。

2. **登录凭证硬编码 + 明文比对**
   `if (!"doctor".equals(username) || !"doctor123".equals(password))` 为明文比较，无密码哈希、无失败锁定、无限流。属 M1 演示债，**绝不可进生产**。
   - 位置：`mdt-auth/.../rest/AuthController.java:29`

3. **JWT 密钥为演示值**
   `mdt.jwt.secret=mdt-dev-secret-change-me-please-32bytes!!` 写死在 `application.yml`。若上线不覆盖，token 可被伪造。
   - 必须改为环境变量/密钥管理（Vault/KMS）注入。

### 🟡 低危（代码整洁 / 健壮性）

4. **`api.ts` 冗余与弱类型**
   `data.token = data.token` 为无效语句；返回用 `as any` 再 `as LoginResult` 弱化了类型。建议清理并显式声明 `LoginResult & { traceId?: string }`。

5. **审计粒度过粗**
   每次 RBAC 校验都写 `AUDIT_LOG` 一行，高频下易暴涨。生产建议异步批量 + 采样。

6. **`npm run build` 含 `vue-tsc -b` 未纳入 CI**
   本次用 `vite build` 验证通过，但严格 TS 检查（noUnusedLocals 等）未在流水线校验，存在类型债累积风险。

### 🟢 备注（非异常，M1 设计内）

7. **多租户隔离 / HTTPS / DICOM-TLS** 按里程碑规划在 M6 / 生产部署阶段落地，非 M1 缺陷。
8. **`RestTraceFilter`（Servlet）仅作用于权限服务**，网关用响应式 `GatewayTraceGlobalFilter`，二者不冲突；当前同步 servlet 下 MDC 传播无虞。

---

## 三、合规性走查（关键项达标）

| 合规项 | 状态 | 证据 |
|---|---|---|
| 全链路 TraceId | ✅ | 网关响应式 + Servlet Filter + gRPC Context 三层透传，单测/接口验证 |
| 日志脱敏（姓名/身份证→掩码） | ✅ | `Desensitizer.mask` + 单测通过 |
| 统一就诊标识 PatientVisitUID | ✅ | `PatientVisit` 实体 + 写入/回读自测 |
| 字段级 RBAC | ✅ | `RbacService` + 单测通过 |
| 六域边界 / 无跨域耦合 | ✅ | 仅权限服务暴露 REST+gRPC，网关路由；5 个 stub 仅依赖 `spring-boot-starter`，不触发 datasource |
| 多租户隔离 | ⏳ | M6 |
| 传输加密 HTTPS/DICOM-TLS | ⏳ | 生产部署 |

---

## 四、审查结论

**脚手架与底座无异常**，可放心作为后续里程碑地基。需在**生产前置（或 M5/M6）**前清偿的中危安全债：全局异常处理、凭证/密钥外置、密码哈希。这些不阻塞进入 M2。

---

## 五、待你定夺（grill 式确认）

针对上面"中危安全项"，你希望我如何处理后再进 M2？

- **A. 现在就修**：在 M1 内补 `@RestControllerAdvice` + 把 JWT 密钥改为环境变量 + 登录改内存用户表（BCrypt），再进 M2。
- **B. 记技术债、直接进 M2**：把 3 项中危列为"生产前置清单"，M2 开始照常推进。
- **C. 折中**：只修低危代码整洁（#4）+ 将中危写入 `docs/tech-debt.md`，直接进 M2。

请选 A / B / C，或给出你的指示。
