package com.api_portal.backend.modules.user.service;

import com.api_portal.backend.modules.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakSyncService {
    
    private final UserService userService;
    
    @Transactional
    public User syncUserFromToken(Authentication authentication, String ipAddress) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            log.warn("Autenticação inválida para sincronização");
            return null;
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        
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
            firstName = email.split("@")[0];
        }
        if (lastName == null || lastName.isEmpty()) {
            lastName = "";
        }
        
        log.info("Sincronizando usuário do Keycloak: {} ({})", email, keycloakId);
        log.info("Roles extraídas do JWT: {}", roleCodes);
        
        User user = userService.createOrUpdateUser(
            keycloakId, 
            email, 
            firstName, 
            lastName, 
            username, 
            emailVerified,
            roleCodes
        );
        
        // Atualizar último login
        if (ipAddress != null) {
            userService.updateLastLogin(user.getId(), ipAddress);
        }
        
        return user;
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
    
    public String extractKeycloakId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getSubject();
        }
        return null;
    }
    
    public String extractEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("email");
        }
        return null;
    }
}
