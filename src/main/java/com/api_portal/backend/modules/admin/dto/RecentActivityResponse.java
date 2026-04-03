package com.api_portal.backend.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    
    private String id;
    private ActivityType type;
    private String title;
    private String description;
    private String userName;
    private String userEmail;
    private LocalDateTime timestamp;
    private String icon;
    private String iconColor;
    
    public enum ActivityType {
        USER_REGISTERED,
        API_PUBLISHED,
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_APPROVED,
        SUBSCRIPTION_REVOKED,
        API_UPDATED,
        USER_UPDATED
    }
}
