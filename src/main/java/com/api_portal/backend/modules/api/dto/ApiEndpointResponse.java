package com.api_portal.backend.modules.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApiEndpointResponse {
    
    private UUID id;
    private UUID versionId;
    private String path;
    private String method;
    private String summary;
    private String description;
    private List<String> tags;
    private Boolean requiresAuth;
    private String authHeadersJson;
    private String authQueryParamsJson;
    private Boolean isDeprecated;
    private String requestExample;
    private String responseExample;
    private Integer responseTime;
    private Double successRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
