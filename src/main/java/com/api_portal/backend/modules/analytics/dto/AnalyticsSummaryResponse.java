package com.api_portal.backend.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private Long totalRequests;
    private Long totalErrors;
    private Double errorRate;
    private Double averageLatency;
    private Long totalApis;
    private Long totalSubscriptions;
    private List<TopConsumerDto> topConsumers;
    private List<TopEndpointDto> topEndpoints;
    private Map<String, Long> requestsByMethod;
    private Map<String, Long> requestsByStatus;
}
