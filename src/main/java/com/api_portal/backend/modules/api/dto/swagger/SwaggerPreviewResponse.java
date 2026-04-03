package com.api_portal.backend.modules.api.dto.swagger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwaggerPreviewResponse {
    
    private String title;
    private String description;
    private String version;
    private String baseUrl;
    private String termsOfServiceUrl;
    private String documentationUrl;
    
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Builder.Default
    private List<EndpointPreview> endpoints = new ArrayList<>();
    
    private Integer totalEndpoints;
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    private String openApiVersion;
    private String originalSpec;
}
