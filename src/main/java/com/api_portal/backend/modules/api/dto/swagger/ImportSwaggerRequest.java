package com.api_portal.backend.modules.api.dto.swagger;

import com.api_portal.backend.modules.api.domain.enums.ApiVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportSwaggerRequest {
    
    private String title;
    private String description;
    private String version;
    private String baseUrl;
    private String termsOfServiceUrl;
    private String documentationUrl;
    
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Builder.Default
    private List<String> selectedEndpointIds = new ArrayList<>();
    
    @Builder.Default
    private List<EndpointPreview> selectedEndpoints = new ArrayList<>();
    
    private UUID categoryId;
    
    @Builder.Default
    private ApiVisibility visibility = ApiVisibility.PUBLIC;
    
    private String originalSpec;
}
