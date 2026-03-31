package com.api_portal.backend.modules.auth.filter;

import com.api_portal.backend.modules.auth.model.ApiKey;
import com.api_portal.backend.modules.auth.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    
    private final ApiKeyService apiKeyService;
    private static final String API_KEY_HEADER = "X-API-Key";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Ignorar filtro para endpoints públicos do Swagger
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") || 
            path.startsWith("/api-docs") || 
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/webjars") ||
            path.startsWith("/gateway/")) {  // Ignorar gateway - tem sua própria validação
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            if (apiKeyService.validateApiKey(apiKey)) {
                Optional<ApiKey> apiKeyOpt = apiKeyService.getApiKeyByValue(apiKey);
                
                if (apiKeyOpt.isPresent()) {
                    ApiKey key = apiKeyOpt.get();
                    
                    // Criar autenticação baseada na API Key
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            key.getUserId(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_USER"))
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Utilizador autenticado via API Key: {}", key.getUserId());
                }
            } else {
                log.warn("API Key inválida recebida: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
