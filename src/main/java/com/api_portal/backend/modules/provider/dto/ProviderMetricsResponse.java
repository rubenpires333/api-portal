package com.api_portal.backend.modules.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMetricsResponse {
    
    private MetricsSummary summary;
    private List<DailyUsage> dailyUsage;
    private List<TopApi> topApis;
    private List<TopConsumer> topConsumers;
    private List<TopEndpoint> topEndpoints;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private Long totalCalls;
        private Long totalCallsLast30Days;
        private Double averageResponseTime;
        private Double errorRate;
        private Long activeApis;
        private Long activeSubscriptions;
        private Long uniqueConsumers;
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
    public static class TopApi {
        private UUID apiId;
        private String apiName;
        private String apiSlug;
        private Long totalCalls;
        private Double averageResponseTime;
        private Double errorRate;
        private Long activeSubscriptions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopConsumer {
        private String consumerId;
        private String consumerName;
        private String consumerEmail;
        private Long totalCalls;
        private Double averageResponseTime;
        private String lastCallAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEndpoint {
        private String endpoint;
        private String method;
        private Long totalCalls;
        private Double averageResponseTime;
        private Double errorRate;
    }
}
