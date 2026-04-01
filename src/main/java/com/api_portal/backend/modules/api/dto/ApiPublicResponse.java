package com.api_portal.backend.modules.api.dto;

import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.domain.enums.ApiVisibility;
import com.api_portal.backend.modules.api.domain.enums.AuthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiPublicResponse {
    private UUID id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private CategorySummary category;
    private ApiVisibility visibility;
    private ProviderInfo provider;
    private List<String> tags;
    private String baseUrl;
    private String documentationUrl;
    private String termsOfServiceUrl;
    private AuthType authType;
    private Integer rateLimit;
    private String rateLimitPeriod;
    private List<EndpointPublicDto> endpoints;
    private List<VersionSummary> versions;
    private Boolean isSubscribed;
    private UUID subscriptionId;
    private String subscriptionStatus;
    private Boolean requiresApproval;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private UUID id;
        private String name;
        private String slug;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo {
        private String id;
        private String name;
        private String email;
        private String bio;
        private String company;
        private String website;
        private String avatarUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionSummary {
        private UUID id;
        private String version;
        private Boolean isDefault;
        private Boolean isDeprecated;
        private ApiStatus status;
    }
}
