package com.mdt.auth.config;

import com.mdt.auth.security.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final PermissionInterceptor permInterceptor;

    public WebConfig(PermissionInterceptor permInterceptor) {
        this.permInterceptor = permInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**"); // 登录不需要权限
    }
}
