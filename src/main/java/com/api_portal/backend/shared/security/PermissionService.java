package com.api_portal.backend.shared.security;

import com.api_portal.backend.modules.user.domain.Permission;
import com.api_portal.backend.modules.user.domain.Role;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final UserRepository userRepository;
    
    /**
     * Verifica se o usuário autenticado tem a permissão especificada.
     */
    public boolean hasPermission(String permissionCode) {
        try {
            Set<String> userPermissions = getCurrentUserPermissions();
            boolean hasPermission = userPermissions.contains(permissionCode);
            
            if (!hasPermission) {
                log.warn("Usuário não possui permissão: {}", permissionCode);
                log.debug("Permissões do usuário: {}", userPermissions);
            }
            
            return hasPermission;
        } catch (Exception e) {
            log.error("Erro ao verificar permissão: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se o usuário tem TODAS as permissões especificadas.
     */
    public boolean hasAllPermissions(String... permissionCodes) {
        Set<String> userPermissions = getCurrentUserPermissions();
        
        for (String code : permissionCodes) {
            if (!userPermissions.contains(code)) {
                log.warn("Usuário não possui permissão: {}", code);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verifica se o usuário tem PELO MENOS UMA das permissões especificadas.
     */
    public boolean hasAnyPermission(String... permissionCodes) {
        Set<String> userPermissions = getCurrentUserPermissions();
        
        for (String code : permissionCodes) {
            if (userPermissions.contains(code)) {
                return true;
            }
        }
        
        log.warn("Usuário não possui nenhuma das permissões: {}", String.join(", ", permissionCodes));
        return false;
    }
    
    /**
     * Obtém todas as permissões do usuário autenticado.
     */
    public Set<String> getCurrentUserPermissions() {
        String keycloakId = getCurrentUserKeycloakId();
        
        if (keycloakId == null) {
            log.warn("Usuário não autenticado");
            return Set.of();
        }
        
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElse(null);
        
        if (user == null) {
            log.warn("Usuário não encontrado no banco: {}", keycloakId);
            return Set.of();
        }
        
        // Coletar todas as permissões de todas as roles do usuário
        return user.getRoles().stream()
            .filter(Role::getActive)
            .flatMap(role -> role.getPermissions().stream())
            .filter(Permission::getActive)
            .map(Permission::getCode)
            .collect(Collectors.toSet());
    }
    
    /**
     * Obtém o Keycloak ID do usuário autenticado.
     */
    private String getCurrentUserKeycloakId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getSubject();
            }
            
            return null;
        } catch (Exception e) {
            log.error("Erro ao obter Keycloak ID: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifica se o usuário é SUPER_ADMIN (tem todas as permissões).
     */
    public boolean isSuperAdmin() {
        try {
            String keycloakId = getCurrentUserKeycloakId();
            
            if (keycloakId == null) {
                log.warn("❌ isSuperAdmin: keycloakId é null");
                return false;
            }
            
            log.debug("🔍 Verificando SUPER_ADMIN para keycloakId: {}", keycloakId);
            
            User user = userRepository.findByKeycloakId(keycloakId)
                .orElse(null);
            
            if (user == null) {
                log.warn("❌ isSuperAdmin: Usuário não encontrado no banco para keycloakId: {}", keycloakId);
                return false;
            }
            
            log.debug("👤 Usuário encontrado: {} ({})", user.getEmail(), user.getId());
            log.debug("📋 Roles do usuário: {}", user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList()));
            
            boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()) && role.getActive());
            
            if (isSuperAdmin) {
                log.debug("✅ Usuário é SUPER_ADMIN");
            } else {
                log.warn("❌ Usuário NÃO é SUPER_ADMIN");
            }
            
            return isSuperAdmin;
                
        } catch (Exception e) {
            log.error("❌ Erro ao verificar SUPER_ADMIN: {}", e.getMessage(), e);
            return false;
        }
    }
}
