package com.api_portal.backend.modules.audit.interceptor;

import com.api_portal.backend.modules.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {
    
    private final AuditService auditService;
    private static final String START_TIME_ATTRIBUTE = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            // Ignorar endpoints de auditoria, swagger e actuator
            String uri = request.getRequestURI();
            if (uri.startsWith("/api/v1/audit") || 
                uri.startsWith("/swagger-ui") || 
                uri.startsWith("/api-docs") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/actuator")) {
                return;
            }
            
            Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
            Long executionTime = startTime != null ? System.currentTimeMillis() - startTime : null;
            
            // Extrair dados do request ANTES de chamar método assíncrono
            String method = request.getMethod();
            String queryParams = request.getQueryString();
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Extrair userId e userEmail do JWT ANTES de chamar método assíncrono
            String userId = extractUserId(request);
            String userEmail = extractUserEmail(request);
            
            String requestBody = null;
            String responseBody = null;
            
            if (request instanceof ContentCachingRequestWrapper requestWrapper) {
                byte[] content = requestWrapper.getContentAsByteArray();
                if (content.length > 0) {
                    requestBody = new String(content, StandardCharsets.UTF_8);
                }
            }
            
            if (response instanceof ContentCachingResponseWrapper responseWrapper) {
                byte[] content = responseWrapper.getContentAsByteArray();
                if (content.length > 0) {
                    responseBody = new String(content, StandardCharsets.UTF_8);
                }
            }
            
            String errorMessage = null;
            String stackTrace = null;
            
            if (ex != null) {
                errorMessage = ex.getMessage();
                stackTrace = getStackTrace(ex);
            }
            
            // Chamar método assíncrono com dados já extraídos
            auditService.logRequest(
                userId,
                userEmail,
                method,
                uri,
                queryParams,
                ipAddress,
                userAgent,
                response.getStatus(),
                executionTime,
                requestBody,
                responseBody,
                errorMessage,
                stackTrace
            );
            
        } catch (Exception e) {
            log.error("Erro no interceptor de auditoria: {}", e.getMessage());
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 3000) break; // Limitar tamanho
        }
        return sb.toString();
    }
    
    private String extractUserId(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getSubject();
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair userId: {}", e.getMessage());
        }
        return null;
    }
    
    private String extractUserEmail(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("email");
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair userEmail: {}", e.getMessage());
        }
        return null;
    }
}
