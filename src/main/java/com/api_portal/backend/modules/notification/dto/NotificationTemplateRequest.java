package com.api_portal.backend.modules.notification.dto;

import com.api_portal.backend.modules.notification.domain.enums.NotificationChannel;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {
    
    @NotNull(message = "Tipo de notificação é obrigatório")
    private NotificationType type;
    
    @NotNull(message = "Canal é obrigatório")
    private NotificationChannel channel;
    
    @NotBlank(message = "Idioma é obrigatório")
    private String language;
    
    @NotBlank(message = "Assunto é obrigatório")
    private String subject;
    
    @NotBlank(message = "Template é obrigatório")
    private String template;
    
    private List<String> variables;
}
