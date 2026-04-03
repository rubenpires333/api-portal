package com.api_portal.backend.modules.notification.dto;

import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, Object> data;
    private Boolean isRead;
    private String actionUrl;
    private LocalDateTime createdAt;
}
