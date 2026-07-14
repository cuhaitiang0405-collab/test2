package com.mdt.auth.security;

/** 用户记录（内存表用）。密码仅以 BCrypt 哈希形态存在，禁止回传明文。 */
public record UserRecord(String username, String tenantId, String role, String passwordHash) {
}
