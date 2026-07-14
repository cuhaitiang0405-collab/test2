# M1-A 安全加固报告（A 方案已落地）

> 范围：在 M1 脚手架/底座基础上，于进入 M2 前补齐三处安全债务（依据 `docs/m1-code-review.md` 选项 A）。
> 状态：**已自测通过、服务存活验证通过，待用户确认后进 M2。**

## 1. 设计说明（≤200 字）
M1 评审暴露三处中危债务：无全局异常处理器、JWT 密钥硬编码、登录明文比对。本步补齐：①新增 `@RestControllerAdvice` 全局异常处理器，统一回合规错误体 `{code,message,traceId}` 并落审计，绝不泄露堆栈；②JWT 密钥改为环境变量 `MDT_JWT_SECRET`（带 dev 回退），杜绝仓库内硬编码；③登录改内存用户表 + BCrypt 加盐哈希，禁止明文比较，防账户枚举。三项均为零业务侵入的底座加固。

## 2. 接口定义
| 项 | 说明 |
|---|---|
| `POST /api/auth/login` | body `{username,password}` → 成功 `200 {token,username,tenantId,role}`；失败 `401 {code:"AUTH_INVALID_CREDENTIALS",message,traceId}` |
| `GET /api/auth/me` | Header `Authorization: Bearer <jwt>` → `200 {username,tenantId,role}`；缺/错令牌 `401 AUTH_INVALID_CREDENTIALS` |
| 错误响应体 | `ErrorBody{code:String, message:String, traceId:String}`（record，无堆栈字段） |
| 异常体系 | `AuthException(httpStatus,code,message)` → `BadCredentialsException(401)` / `ResourceNotFoundException(404)` |

## 3. 关键代码片段
```java
// UserService：内存用户表 + BCrypt（生产接统一认证）
public UserRecord authenticate(String username, String rawPassword) {
    UserRecord u = users.get(username);
    if (u == null || rawPassword == null || !encoder.matches(rawPassword, u.passwordHash()))
        throw new BadCredentialsException();   // 不区分账户是否存在
    return u;
}
```
```java
// JwtUtil：密钥来自环境变量，缺失回退 dev 串（application.yml: secret: ${MDT_JWT_SECRET:mdt-dev-...}）
@Value("${mdt.jwt.secret}") private String secret;
```
```java
// GlobalExceptionHandler：合规错误体 + 失败审计，堆栈不外泄
@ExceptionHandler(AuthException.class)
public ResponseEntity<ErrorBody> handleBiz(AuthException e) {
    audit.log("UNKNOWN","-",null,"AUTH_EXCEPTION","code="+e.getCode());
    return ResponseEntity.status(e.getHttpStatus())
            .body(ErrorBody.of(e.getCode(), e.getMessage(), resolveTrace()));
}
```

## 4. 测试建议
- 单测 `UserServiceTest`：正确口令登录、错误口令/未知账户抛 401、BCrypt 同明文不同盐、角色种子齐全（已覆盖）。
- 集成 `AuthControllerTest`：登录成功返回 token、错误密码返回 401 且无 `stackTrace/cause` 字段、缺 Bearer 返回 401、`/me` 解析 profile（已覆盖）。
- 安全回归：①设 `MDT_JWT_SECRET` 后重启，`/me` 可验签即通过；②用旧 fallback 密钥签发的 token 在新密钥实例应验签失败（证明密钥确来自环境变量）。
- 审计：401 失败应落 `AUDIT_LOG`（action=AUTH_EXCEPTION，detail 脱敏）。

## 5. 部署说明
- 环境变量：生产**必须**注入 `MDT_JWT_SECRET`（≥32 字节，建议 KMS/密钥轮换），否则回退 dev 串（仅本地可用，禁止生产提交）。
- 依赖：新增 `spring-security-crypto`（仅 BCrypt，无 Spring Security 过滤器链，不阻断既有端点）。
- 演示账户：`doctor/doctor123`、`ecg_tech/ecg123`、`pathologist/path123`（租户 T001），生产改为医院统一认证源。
- 启动：`MDT_JWT_SECRET=xxx java -jar mdt-auth-1.0.0.jar`；网关/前端无变动。
- 验证基线：14 个测试全绿，`mvn install` 全 8 模块 BUILD SUCCESS；网关→auth 登录 200、错误口令 401、TraceId 透传一致。
