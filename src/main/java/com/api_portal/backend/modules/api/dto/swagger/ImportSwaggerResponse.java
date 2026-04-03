package com.api_portal.backend.modules.api.dto.swagger;

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
public class ImportSwaggerResponse {
    
    private UUID apiId;
    private String apiName;
    private UUID versionId;
    private String version;
    private Integer endpointsCreated;
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
