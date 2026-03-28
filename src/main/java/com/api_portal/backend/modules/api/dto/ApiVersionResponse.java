package com.api_portal.backend.modules.api.dto;

import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApiVersionResponse {
    
    private UUID id;
    private UUID apiId;
    private String version;
    private String description;
    private ApiStatus status;
    private Boolean isDefault;
    private Boolean isDeprecated;
    private LocalDateTime deprecatedAt;
    private String deprecationMessage;
    private String baseUrl;
    private String openApiSpec;
    private List<EndpointSummary> endpoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    public static class EndpointSummary {
        private UUID id;
        private String path;
        private String method;
        private String summary;
        private Boolean requiresAuth;
        private Boolean isDeprecated;
    }
}
