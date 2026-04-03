package com.api_portal.backend.modules.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerMetricsResponse {
    
    private MetricsSummary summary;
    private List<DailyUsage> dailyUsage;
    private List<ApiUsage> apiUsage;
    private List<EndpointUsage> endpointUsage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private Long totalCalls;
        private Long totalCallsLast30Days;
        private Double averageResponseTime;
        private Double errorRate;
        private Long activeSubscriptions;
        private Long totalApis;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsage {
        private LocalDate date;
        private Long totalCalls;
        private Long successCalls;
        private Long errorCalls;
        private Double averageResponseTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiUsage {
        private String apiId;
        private String apiName;
        private String apiSlug;
        private Long totalCalls;
        private Double averageResponseTime;
        private Double errorRate;
        private String subscriptionStatus;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointUsage {
        private String apiName;
        private String endpoint;
        private String method;
        private Long totalCalls;
        private Double averageResponseTime;
        private Double errorRate;
    }
}
