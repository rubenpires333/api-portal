package com.api_portal.backend.shared.config;

import com.api_portal.backend.modules.audit.interceptor.AuditInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final AuditInterceptor auditInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/v1/audit/**",
                "/swagger-ui/**",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/actuator/**"
            );
    }
}
