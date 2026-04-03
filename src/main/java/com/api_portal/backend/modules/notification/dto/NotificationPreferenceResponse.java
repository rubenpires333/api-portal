package com.api_portal.backend.modules.notification.dto;

import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private UUID id;
    private NotificationType notificationType;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
}
