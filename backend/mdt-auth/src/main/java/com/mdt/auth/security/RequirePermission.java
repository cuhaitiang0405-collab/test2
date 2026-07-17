package com.mdt.auth.security;

import java.lang.annotation.*;

/** 标记 REST 方法需要特定权限 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value(); // 权限 code，如 "USER_MANAGE"
}
