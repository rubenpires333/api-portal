package com.api_portal.backend.modules.admin.dto;

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
public class DashboardStatsResponse {
    
    private UserStats userStats;
    private ApiStats apiStats;
    private SubscriptionStats subscriptionStats;
    private ActivityStats activityStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private Long totalUsers;
        private Long totalProviders;
        private Long totalConsumers;
        private Long totalAdmins;
        private Long newUsersLast7Days;
        private Double growthPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiStats {
        private Long totalApis;
        private Long publishedApis;
        private Long draftApis;
        private Long newApisLast7Days;
        private Double growthPercentage;
        private Map<String, Long> apisByCategory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionStats {
        private Long totalSubscriptions;
        private Long activeSubscriptions;
        private Long pendingSubscriptions;
        private Long revokedSubscriptions;
        private Double growthPercentage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityStats {
        private Long activeUsersLast30Days;
        private Double engagementRate;
        private Long totalApiCalls;
    }
}
