package com.api_portal.backend.modules.api.dto;

import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.domain.enums.ApiVisibility;
import com.api_portal.backend.modules.api.domain.enums.AuthType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApiResponse {
    
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    
    private CategorySummary category;
    
    private ApiStatus status;
    private ApiVisibility visibility;
    
    private ProviderInfo provider;
    
    private String baseUrl;
    private String documentationUrl;
    private String termsOfServiceUrl;
    
    private AuthType authType;
    
    private String logoUrl;
    private String iconUrl;
    
    private List<String> tags;
    
    private Integer rateLimit;
    private String rateLimitPeriod;
    
    private Boolean requiresApproval;
    private Boolean isActive;
    
    private List<VersionSummary> versions;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    
    @Data
    @Builder
    public static class CategorySummary {
        private UUID id;
        private String name;
        private String slug;
    }
    
    @Data
    @Builder
    public static class ProviderInfo {
        private String id;
        private String name;
        private String email;
    }
    
    @Data
    @Builder
    public static class VersionSummary {
        private UUID id;
        private String version;
        private Boolean isDefault;
        private Boolean isDeprecated;
        private ApiStatus status;
    }
}
