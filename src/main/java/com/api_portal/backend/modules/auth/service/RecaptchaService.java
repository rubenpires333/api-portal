package com.api_portal.backend.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaService {
    
    private final RestTemplate restTemplate;
    
    @Value("${recaptcha.secret-key:}")
    private String secretKey;
    
    @Value("${recaptcha.enabled:false}")
    private boolean recaptchaEnabled;
    
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    
    /**
     * Valida o token do reCAPTCHA
     */
    public boolean validateCaptcha(String captchaResponse) {
        // Se reCAPTCHA estiver desabilitado, sempre retornar true
        if (!recaptchaEnabled || secretKey == null || secretKey.isEmpty()) {
            log.debug("reCAPTCHA desabilitado, pulando validação");
            return true;
        }
        
        if (captchaResponse == null || captchaResponse.isEmpty()) {
            log.warn("Token reCAPTCHA vazio");
            return false;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", captchaResponse);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                RECAPTCHA_VERIFY_URL,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Boolean success = (Boolean) responseBody.get("success");
                
                if (Boolean.FALSE.equals(success)) {
                    log.warn("Validação reCAPTCHA falhou: {}", responseBody.get("error-codes"));
                }
                
                return Boolean.TRUE.equals(success);
            }
            
            log.error("Resposta inesperada do Google reCAPTCHA: {}", response.getStatusCode());
            return false;
            
        } catch (Exception e) {
            log.error("Erro ao validar reCAPTCHA: {}", e.getMessage());
            // Em caso de erro na validação, permitir o login (fail-open)
            return true;
        }
    }
}
