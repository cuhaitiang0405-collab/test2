package com.mdt.auth.rest;

import com.mdt.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 登录流程 + 全局异常处理器端到端验证（真实 UserService/BCrypt/JwtUtil，H2 内存库）。
 * 成功返回 token；失败返回 401 合规错误体且绝不泄露堆栈/内部 cause。
 */
@SpringBootTest(classes = com.mdt.auth.AuthApplication.class,
        properties = {"grpc.server.port=0"})   // 避免与 AuthGrpcIntegrationTest 的 50054 端口冲突
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;

    @Test
    void loginSuccessReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"doctor\",\"password\":\"doctor123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.tenantId").value("T001"));
    }

    @Test
    void loginWrongPasswordReturns401NoStack() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"doctor\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.cause").doesNotExist());
    }

    @Test
    void unknownUserReturns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"nobody\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void meWithoutBearerReturns401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void meWithBearerReturnsProfile() throws Exception {
        String token = jwt.issue("doctor", "T001", "DOCTOR");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("doctor"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }
}
