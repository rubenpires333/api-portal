package com.api_portal.backend.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopRankingsResponse {
    
    private List<TopApi> topApis;
    private List<TopProvider> topProviders;
    private List<TopConsumer> topConsumers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopApi {
        private UUID id;
        private String name;
        private String slug;
        private Long subscriptionCount;
        private String providerName;
        private String categoryName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProvider {
        private String providerId;
        private String providerName;
        private String providerEmail;
        private Long apiCount;
        private Long totalSubscriptions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopConsumer {
        private String consumerId;
        private String consumerName;
        private String consumerEmail;
        private Long subscriptionCount;
        private Long activeSubscriptions;
    }
}
