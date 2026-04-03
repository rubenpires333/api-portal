package com.api_portal.backend.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAlertsResponse {
    
    private List<SystemAlert> alerts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemAlert {
        private UUID id;
        private AlertType type;
        private AlertSeverity severity;
        private String title;
        private String message;
        private String entityId;
        private String entityName;
        private LocalDateTime createdAt;
        private String actionUrl;
    }
    
    public enum AlertType {
        API_NO_SUBSCRIPTIONS,
        SUBSCRIPTION_PENDING_LONG,
        USER_INACTIVE,
        API_DRAFT_LONG,
        SYSTEM_ERROR
    }
    
    public enum AlertSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
