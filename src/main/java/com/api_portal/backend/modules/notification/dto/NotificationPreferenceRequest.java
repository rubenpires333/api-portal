package com.api_portal.backend.modules.notification.dto;

import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {
    
    @NotNull(message = "Tipo de notificação é obrigatório")
    private NotificationType notificationType;
    
    @NotNull(message = "inAppEnabled é obrigatório")
    private Boolean inAppEnabled;
    
    @NotNull(message = "emailEnabled é obrigatório")
    private Boolean emailEnabled;
}
