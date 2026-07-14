package com.mdt.auth.security;

/** 资源未找到（404）。 */
public class ResourceNotFoundException extends AuthException {
    public ResourceNotFoundException(String message) {
        super(404, "RESOURCE_NOT_FOUND", message);
    }
}
