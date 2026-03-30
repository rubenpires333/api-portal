package com.api_portal.backend.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopEndpointDto {
    private String endpoint;
    private String method;
    private Long requestCount;
    private Double averageLatency;
}
