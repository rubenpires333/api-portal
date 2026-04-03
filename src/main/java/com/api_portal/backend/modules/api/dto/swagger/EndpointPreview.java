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
public class EndpointPreview {
    
    private String id;
    private String path;
    private String method;
    private String summary;
    private String description;
    
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Builder.Default
    private Boolean requiresAuth = true;
    
    @Builder.Default
    private Boolean isDeprecated = false;
    
    private String requestExample;
    private String responseExample;
    
    @Builder.Default
    private Boolean selected = true;
}
