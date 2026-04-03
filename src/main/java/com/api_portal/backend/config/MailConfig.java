package com.api_portal.backend.config;

import com.api_portal.backend.modules.settings.service.PlatformSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MailConfig {

    private final PlatformSettingService platformSettingService;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String defaultHost;

    @Value("${spring.mail.port:587}")
    private int defaultPort;

    @Value("${spring.mail.username:}")
    private String defaultUsername;

    @Value("${spring.mail.password:}")
    private String defaultPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        try {
            // Tentar carregar configurações do banco de dados
            String host = platformSettingService.getSetting("mail.host", defaultHost);
            String portStr = platformSettingService.getSetting("mail.port", String.valueOf(defaultPort));
            String username = platformSettingService.getSetting("mail.username", defaultUsername);
            String password = platformSettingService.getSetting("mail.password", defaultPassword);
            
            mailSender.setHost(host);
            mailSender.setPort(Integer.parseInt(portStr));
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            
            log.info("Configurações de email carregadas do banco de dados");
        } catch (Exception e) {
            // Se falhar, usar valores padrão do application.properties
            log.warn("Não foi possível carregar configurações de email do banco. Usando valores padrão.");
            mailSender.setHost(defaultHost);
            mailSender.setPort(defaultPort);
            mailSender.setUsername(defaultUsername);
            mailSender.setPassword(defaultPassword);
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
