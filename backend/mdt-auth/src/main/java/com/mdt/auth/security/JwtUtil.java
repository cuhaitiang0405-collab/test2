package com.mdt.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具：签发与解析。
 * 密钥来源：环境变量 MDT_JWT_SECRET（生产必配，≥32 字节），缺失时回退 dev 占位串（仅本地/测试）。
 * 生产应通过 KMS/密钥轮换注入，严禁提交真实密钥到仓库。
 */
@Component
public class JwtUtil {
    @Value("${mdt.jwt.secret}")
    private String secret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String issue(String username, String tenantId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 1000 * 60 * 60 * 8))   // 8h
                .signWith(key())
                .compact();
    }

    public io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }
}
