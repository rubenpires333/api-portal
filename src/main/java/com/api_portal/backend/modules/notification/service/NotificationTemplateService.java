package com.api_portal.backend.modules.notification.service;

import com.api_portal.backend.modules.notification.domain.entity.NotificationTemplate;
import com.api_portal.backend.modules.notification.domain.enums.NotificationChannel;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.notification.domain.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {
    
    private final NotificationTemplateRepository templateRepository;
    
    /**
     * Renderizar template com variáveis
     */
    public String renderTemplate(
            NotificationType type, 
            NotificationChannel channel, 
            String language,
            Map<String, Object> variables) {
        
        NotificationTemplate template = templateRepository
            .findByTypeAndChannelAndLanguage(type, channel, language)
            .orElseGet(() -> getDefaultTemplate(type, channel, language));
        
        String rendered = template.getTemplate();
        
        // Substituir variáveis
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(placeholder, value);
        }
        
        return rendered;
    }
    
    /**
     * Obter subject do template
     */
    public String getSubject(
            NotificationType type, 
            NotificationChannel channel, 
            String language,
            Map<String, Object> variables) {
        
        NotificationTemplate template = templateRepository
            .findByTypeAndChannelAndLanguage(type, channel, language)
            .orElseGet(() -> getDefaultTemplate(type, channel, language));
        
        String subject = template.getSubject();
        
        // Substituir variáveis no subject
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            subject = subject.replace(placeholder, value);
        }
        
        return subject;
    }
    
    /**
     * Templates padrão (fallback)
     */
    private NotificationTemplate getDefaultTemplate(
            NotificationType type, 
            NotificationChannel channel, 
            String language) {
        
        String subject = getDefaultSubject(type, language);
        String template = getDefaultTemplateContent(type, channel, language);
        
        return NotificationTemplate.builder()
            .type(type)
            .channel(channel)
            .language(language)
            .subject(subject)
            .template(template)
            .build();
    }
    
    private String getDefaultSubject(NotificationType type, String language) {
        if ("pt".equals(language)) {
            return switch (type) {
                case SUBSCRIPTION_REQUESTED -> "Nova Solicitação de Subscription";
                case SUBSCRIPTION_APPROVED -> "Subscription Aprovada";
                case SUBSCRIPTION_REVOKED -> "Subscription Revogada";
                case API_VERSION_RELEASED -> "Nova Versão de API Disponível";
                case API_DEPRECATED -> "API Marcada como Deprecated";
                case RATE_LIMIT_WARNING -> "Aviso: Limite de Requisições Próximo";
                case RATE_LIMIT_EXCEEDED -> "Limite de Requisições Excedido";
                case API_MAINTENANCE -> "Manutenção Programada";
                case API_INCIDENT -> "Incidente Reportado";
                default -> "Notificação";
            };
        }
        return "Notification";
    }
    
    private String getDefaultTemplateContent(
            NotificationType type, 
            NotificationChannel channel, 
            String language) {
        
        if (channel == NotificationChannel.EMAIL) {
            return getDefaultEmailTemplate(type, language);
        }
        return getDefaultInAppTemplate(type, language);
    }
    
    private String getDefaultInAppTemplate(NotificationType type, String language) {
        if ("pt".equals(language)) {
            return switch (type) {
                case SUBSCRIPTION_REQUESTED -> 
                    "{{consumerName}} solicitou subscription para a API {{apiName}}";
                case SUBSCRIPTION_APPROVED -> 
                    "Sua subscription para a API {{apiName}} foi aprovada!";
                case SUBSCRIPTION_REVOKED -> 
                    "Sua subscription para a API {{apiName}} foi revogada. Motivo: {{reason}}";
                case API_VERSION_RELEASED -> 
                    "Nova versão {{version}} da API {{apiName}} foi publicada";
                case API_DEPRECATED -> 
                    "A API {{apiName}} foi marcada como deprecated";
                case RATE_LIMIT_WARNING -> 
                    "Você usou {{usedRequests}} de {{limitRequests}} requisições ({{percentage}}%)";
                case RATE_LIMIT_EXCEEDED -> 
                    "Limite de requisições excedido para a API {{apiName}}";
                default -> "{{message}}";
            };
        }
        return "{{message}}";
    }
    
    private String getDefaultEmailTemplate(NotificationType type, String language) {
        // Templates HTML básicos
        String header = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background: #4F46E5; color: white; text-decoration: none; border-radius: 4px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>API Portal</h2>
                    </div>
                    <div class="content">
            """;
        
        String footer = """
                    </div>
                    <div class="footer">
                        <p>Esta é uma notificação automática do API Portal.</p>
                        <p>Para gerenciar suas preferências de notificação, acesse Configurações.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        
        if ("pt".equals(language)) {
            String content = switch (type) {
                case SUBSCRIPTION_REQUESTED -> """
                    <h3>Nova Solicitação de Subscription</h3>
                    <p>Olá {{providerName}},</p>
                    <p><strong>{{consumerName}}</strong> ({{consumerEmail}}) solicitou subscription para sua API <strong>{{apiName}}</strong>.</p>
                    <p><a href="{{actionUrl}}" class="button">Ver Solicitação</a></p>
                    """;
                case SUBSCRIPTION_APPROVED -> """
                    <h3>Subscription Aprovada!</h3>
                    <p>Olá {{consumerName}},</p>
                    <p>Sua subscription para a API <strong>{{apiName}}</strong> foi aprovada!</p>
                    <p>Sua API Key: <code>{{apiKey}}</code></p>
                    <p><a href="{{actionUrl}}" class="button">Testar API</a></p>
                    """;
                case SUBSCRIPTION_REVOKED -> """
                    <h3>Subscription Revogada</h3>
                    <p>Olá {{consumerName}},</p>
                    <p>Sua subscription para a API <strong>{{apiName}}</strong> foi revogada.</p>
                    <p>Motivo: {{reason}}</p>
                    """;
                default -> "<p>{{message}}</p>";
            };
            return header + content + footer;
        }
        
        return header + "<p>{{message}}</p>" + footer;
    }
}
