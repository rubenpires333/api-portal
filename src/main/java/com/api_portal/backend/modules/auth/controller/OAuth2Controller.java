package com.api_portal.backend.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Authentication", description = "Endpoints para autenticação via provedores externos (Google, LinkedIn, etc)")
public class OAuth2Controller {
    
    @Value("${keycloak.url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    @GetMapping("/providers")
    @Operation(summary = "Listar provedores OAuth2 disponíveis")
    public ResponseEntity<Map<String, Object>> getProviders() {
        Map<String, Object> providers = new HashMap<>();
        
        // Google
        Map<String, String> google = new HashMap<>();
        google.put("name", "Google");
        google.put("authUrl", String.format("%s/realms/%s/protocol/openid-connect/auth?client_id=portal-api&redirect_uri=%s/auth/callback&response_type=code&scope=openid&kc_idp_hint=google", 
            keycloakUrl, realm, frontendUrl));
        providers.put("google", google);
        
        // LinkedIn
        Map<String, String> linkedin = new HashMap<>();
        linkedin.put("name", "LinkedIn");
        linkedin.put("authUrl", String.format("%s/realms/%s/protocol/openid-connect/auth?client_id=portal-api&redirect_uri=%s/auth/callback&response_type=code&scope=openid&kc_idp_hint=linkedin", 
            keycloakUrl, realm, frontendUrl));
        providers.put("linkedin", linkedin);
        
        // GitHub (opcional)
        Map<String, String> github = new HashMap<>();
        github.put("name", "GitHub");
        github.put("authUrl", String.format("%s/realms/%s/protocol/openid-connect/auth?client_id=portal-api&redirect_uri=%s/auth/callback&response_type=code&scope=openid&kc_idp_hint=github", 
            keycloakUrl, realm, frontendUrl));
        providers.put("github", github);
        
        return ResponseEntity.ok(providers);
    }
    
    @GetMapping("/login/{provider}")
    @Operation(summary = "Obter URL de autenticação para um provedor específico")
    public ResponseEntity<Map<String, String>> getLoginUrl(@PathVariable String provider) {
        String authUrl = String.format(
            "%s/realms/%s/protocol/openid-connect/auth?client_id=portal-api&redirect_uri=%s/auth/callback&response_type=code&scope=openid&kc_idp_hint=%s",
            keycloakUrl, realm, frontendUrl, provider
        );
        
        Map<String, String> response = new HashMap<>();
        response.put("provider", provider);
        response.put("authUrl", authUrl);
        
        return ResponseEntity.ok(response);
    }
}
