package com.api_portal.backend.shared.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                String userId = jwt.getSubject();
                String email = jwt.getClaimAsString("email");
                return Optional.of(email != null ? email : userId);
            }
            
            return Optional.of(authentication.getName());
            
        } catch (Exception e) {
            return Optional.of("system");
        }
    }
}
