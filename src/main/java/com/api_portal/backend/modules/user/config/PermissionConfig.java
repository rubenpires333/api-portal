package com.api_portal.backend.modules.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.permissions")
public class PermissionConfig {
    
    private List<PermissionDefinition> custom = new ArrayList<>();
    
    @Data
    public static class PermissionDefinition {
        private String name;
        private String code;
        private String description;
        private String resource;
        private String action;
    }
}
