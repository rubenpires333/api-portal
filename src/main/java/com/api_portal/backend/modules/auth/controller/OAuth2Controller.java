package com.api_portal.backend.modules.auth.controller;

import com.api_portal.backend.modules.auth.dto.TokenResponse;
import com.api_portal.backend.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Authentication", description = "Endpoints para autenticação via provedores externos (Google, LinkedIn, etc)")
public class OAuth2Controller {
    
    private final RestTemplate restTemplate;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    
    @Value("${keycloak.url}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.client-id:apicv-backend}")
    private String clientId;
    
    @Value("${keycloak.client-secret:change-me-in-production}")
    private String clientSecret;
    
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
    
    @PostMapping("/callback")
    @Operation(summary = "Callback OAuth2 - Trocar código por token")
    public ResponseEntity<TokenResponse> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String redirectUri,
            HttpServletRequest request) {
        
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);
            body.add("redirect_uri", redirectUri != null ? redirectUri : frontendUrl + "/auth/callback");
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");
                
                // Decodificar o token e sincronizar usuário
                try {
                    Jwt jwt = jwtDecoder.decode(accessToken);
                    String ipAddress = request.getRemoteAddr();
                    syncUserFromJwt(jwt, ipAddress);
                } catch (Exception e) {
                    log.warn("Erro ao sincronizar usuário após OAuth2: {}", e.getMessage());
                }
                
                return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken((String) responseBody.get("refresh_token"))
                    .tokenType("Bearer")
                    .expiresIn(((Number) responseBody.get("expires_in")).longValue())
                    .build());
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (Exception e) {
            log.error("Erro ao processar callback OAuth2: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Sincroniza usuário do JWT para a base de dados
     */
    private void syncUserFromJwt(Jwt jwt, String ipAddress) {
        try {
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            String username = jwt.getClaimAsString("preferred_username");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            
            // Extrair roles do JWT
            List<String> roleCodes = extractRoles(jwt);
            
            // Se não tiver nome, usar o nome do email
            if (firstName == null || firstName.isEmpty()) {
                firstName = email != null ? email.split("@")[0] : "User";
            }
            if (lastName == null || lastName.isEmpty()) {
                lastName = "";
            }
            
            log.info("Sincronizando usuário após OAuth2: {} ({})", email, keycloakId);
            log.info("Roles extraídas do JWT: {}", roleCodes);
            
            var user = userService.createOrUpdateUser(
                keycloakId, 
                email, 
                firstName, 
                lastName, 
                username, 
                emailVerified,
                roleCodes
            );
            
            // Atualizar último login
            if (ipAddress != null && user.getId() != null) {
                userService.updateLastLogin(user.getId(), ipAddress);
            }
            
        } catch (Exception e) {
            log.error("Erro ao sincronizar usuário do JWT: {}", e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            // Tentar extrair de realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                log.debug("Roles encontradas em realm_access: {}", roles);
                return roles;
            }
            
            // Tentar extrair de resource_access
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> resource = (Map<String, Object>) entry.getValue();
                        if (resource.containsKey("roles")) {
                            List<String> roles = (List<String>) resource.get("roles");
                            log.debug("Roles encontradas em resource_access.{}: {}", entry.getKey(), roles);
                            return roles;
                        }
                    }
                }
            }
            
            log.warn("Nenhuma role encontrada no JWT");
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Erro ao extrair roles do JWT: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
