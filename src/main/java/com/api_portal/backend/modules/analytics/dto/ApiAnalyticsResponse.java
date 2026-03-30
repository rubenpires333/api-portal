package com.api_portal.backend.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAnalyticsResponse {
    private UUID apiId;
    private String apiName;
    private String apiSlug;
    private Long totalRequests;
    private Long totalErrors;
    private Double errorRate;
    private Double averageLatency;
    private Long totalSubscriptions;
    private List<TopEndpointDto> topEndpoints;
    private Map<String, Long> requestsByDay;
    private Map<String, Long> requestsByMethod;
}
