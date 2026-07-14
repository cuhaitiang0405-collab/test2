package com.mdt.auth.security;

/** 凭证错误（401）：用户名或密码不正确，不区分账户是否存在以防枚举。 */
public class BadCredentialsException extends AuthException {
    public BadCredentialsException() {
        super(401, "AUTH_INVALID_CREDENTIALS", "用户名或密码错误");
    }
}
