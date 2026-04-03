package com.api_portal.backend.modules.notification.dto;

import com.api_portal.backend.modules.notification.domain.enums.NotificationChannel;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
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
public class NotificationTemplateResponse {
    private UUID id;
    private NotificationType type;
    private NotificationChannel channel;
    private String language;
    private String subject;
    private String template;
    private List<String> variables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
