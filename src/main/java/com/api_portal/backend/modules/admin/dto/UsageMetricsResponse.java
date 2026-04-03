package com.api_portal.backend.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMetricsResponse {
    
    private UsageSummary summary;
    private List<DailyUsage> dailyUsage;
    private List<TopApiUsage> topApis;
    private Map<String, PerformanceMetrics> apiPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageSummary {
        private Long totalCalls;
        private Long totalCallsLast30Days;
        private Long totalCallsToday;
        private Double averageResponseTime;
        private Double errorRate;
        private Long activeApis;
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
    public static class TopApiUsage {
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
    public static class PerformanceMetrics {
        private String apiName;
        private Double averageResponseTime;
        private Double minResponseTime;
        private Double maxResponseTime;
        private Double errorRate;
        private Long totalRequests;
        private Long successRequests;
        private Long errorRequests;
    }
}
