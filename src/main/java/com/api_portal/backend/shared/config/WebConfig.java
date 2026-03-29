package com.api_portal.backend.shared.config;

import com.api_portal.backend.modules.audit.interceptor.AuditInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

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
                "/actuator/**",
                "/gateway/**"
            );
    }
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setRemoveSemicolonContent(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure gateway paths are not treated as static resources
        registry.setOrder(Integer.MAX_VALUE);
    }
}
