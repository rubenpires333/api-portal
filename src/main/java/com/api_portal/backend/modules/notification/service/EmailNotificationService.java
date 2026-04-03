package com.api_portal.backend.modules.notification.service;

import com.api_portal.backend.modules.notification.domain.enums.NotificationChannel;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.settings.service.PlatformSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
public class EmailNotificationService {
    
    private final NotificationTemplateService templateService;
    private final PlatformSettingService platformSettingService;
    
    public EmailNotificationService(
            NotificationTemplateService templateService,
            PlatformSettingService platformSettingService) {
        this.templateService = templateService;
        this.platformSettingService = platformSettingService;
    }
    
    @Value("${spring.mail.enabled:false}")
    private boolean defaultMailEnabled;
    
    @Value("${mail.from.email:noreply@apiportal.com}")
    private String defaultFromEmail;
    
    @Value("${mail.from.name:API Portal}")
    private String defaultFromName;
    
    /**
     * Criar JavaMailSender dinamicamente com configurações do banco
     */
    private JavaMailSender createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        try {
            String host = platformSettingService.getSetting("mail.host", "smtp.gmail.com");
            int port = platformSettingService.getIntSetting("mail.port", 587);
            String username = platformSettingService.getSetting("mail.username", "");
            String password = platformSettingService.getSetting("mail.password", "");
            
            mailSender.setHost(host);
            mailSender.setPort(port);
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
            props.put("mail.debug", "false");
            
        } catch (Exception e) {
            log.error("Erro ao criar JavaMailSender: {}", e.getMessage());
            throw new RuntimeException("Erro ao configurar servidor de email: " + e.getMessage());
        }
        
        return mailSender;
    }
    
    /**
     * Enviar notificação por email
     */
    public void sendNotificationEmail(
            String userEmail,
            NotificationType type,
            Map<String, Object> variables) {
        
        // Verificar se email está habilitado (do banco ou application.properties)
        boolean mailEnabled = platformSettingService.getBooleanSetting("mail.enabled", defaultMailEnabled);
        
        if (!mailEnabled) {
            return;
        }
        
        try {
            String language = "pt"; // TODO: Obter do perfil do usuário
            
            String subject = templateService.getSubject(type, NotificationChannel.EMAIL, language, variables);
            String htmlContent = templateService.renderTemplate(type, NotificationChannel.EMAIL, language, variables);
            
            // Obter configurações de remetente do banco
            String fromEmail = platformSettingService.getSetting("mail.from.email", defaultFromEmail);
            String fromName = platformSettingService.getSetting("mail.from.name", defaultFromName);
            
            // Criar mail sender dinamicamente
            JavaMailSender mailSender = createMailSender();
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(userEmail);
            helper.setSubject(subject);
            
            // Processar imagens Base64 e converter para CID (inline attachments)
            String processedHtml = processBase64Images(htmlContent, helper);
            
            helper.setText(processedHtml, true);
            helper.setFrom(fromEmail, fromName);
            
            mailSender.send(message);
            
            log.info("Email enviado com sucesso para: {}", userEmail);
            
        } catch (Exception e) {
            log.error("Erro ao enviar email para {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Erro ao enviar email: " + e.getMessage());
        }
    }
    
    /**
     * Processar imagens Base64 no HTML e converter para anexos inline (CID)
     */
    private String processBase64Images(String html, MimeMessageHelper helper) {
        if (html == null || !html.contains("data:image")) {
            return html;
        }
        
        try {
            String processedHtml = html;
            int imageCounter = 0;
            
            // Regex para encontrar imagens Base64
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<img[^>]+src=\"(data:image/([^;]+);base64,([^\"]+))\"[^>]*>",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            
            java.util.regex.Matcher matcher = pattern.matcher(html);
            
            while (matcher.find()) {
                String fullDataUri = matcher.group(1);
                String imageType = matcher.group(2);
                String base64Data = matcher.group(3);
                
                // Decodificar Base64
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                
                // Criar CID único
                String cid = "image" + (++imageCounter);
                
                // Adicionar como anexo inline
                helper.addInline(cid, new jakarta.mail.util.ByteArrayDataSource(imageBytes, "image/" + imageType));
                
                // Substituir data URI por CID
                String cidReference = "cid:" + cid;
                processedHtml = processedHtml.replace(fullDataUri, cidReference);
            }
            
            return processedHtml;
            
        } catch (Exception e) {
            log.warn("Erro ao processar imagens Base64: {}", e.getMessage());
            return html;
        }
    }
    
    /**
     * Enviar email de teste
     */
    public void sendTestEmail(String toEmail) {
        // Verificar se email está habilitado
        boolean mailEnabled = platformSettingService.getBooleanSetting("mail.enabled", defaultMailEnabled);
        
        if (!mailEnabled) {
            throw new RuntimeException("Envio de email está desabilitado nas configurações");
        }
        
        try {
            String fromEmail = platformSettingService.getSetting("mail.from.email", defaultFromEmail);
            String fromName = platformSettingService.getSetting("mail.from.name", defaultFromName);
            
            // Criar mail sender dinamicamente
            JavaMailSender mailSender = createMailSender();
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Teste de Configuração de Email - API Portal");
            helper.setText(
                "<html><body>" +
                "<h2>Teste de Email</h2>" +
                "<p>Este é um email de teste do Konekta Dev - API Portal.</p>" +
                "<p>Se você recebeu este email, suas configurações de SMTP estão funcionando corretamente!</p>" +
                "<hr>" +
                "<p><small>Enviado em: " + java.time.LocalDateTime.now() + "</small></p>" +
                "</body></html>",
                true
            );
            helper.setFrom(fromEmail, fromName);
            
            mailSender.send(message);
            
            log.info("Email de teste enviado com sucesso para: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Erro ao enviar email de teste para {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Erro ao enviar email de teste: " + e.getMessage());
        }
    }
}
