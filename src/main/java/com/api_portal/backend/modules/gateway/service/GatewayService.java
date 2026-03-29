package com.api_portal.backend.modules.gateway.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Enumeration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {
    
    private final ApiRepository apiRepository;
    private final RestTemplate restTemplate;
    
    public ResponseEntity<?> proxyRequest(String slug, HttpServletRequest request, String body) {
        log.info("=== GATEWAY REQUEST ===");
        log.info("Slug: {}", slug);
        log.info("Method: {}", request.getMethod());
        log.info("URI: {}", request.getRequestURI());
        log.info("Query: {}", request.getQueryString());
        
        // 1. Buscar API pelo slug
        Api api = apiRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException("API não encontrada: " + slug));
        
        log.info("API encontrada: {} ({})", api.getName(), api.getBaseUrl());
        
        // 2. Verificar se API está ativa (permite testar antes de publicar)
        if (!api.getIsActive()) {
            log.error("API inativa: {}", slug);
            throw new ApiException("API está inativa");
        }
        
        // 3. Construir URL de destino
        String path = extractPath(request.getRequestURI(), slug);
        String targetUrl = api.getBaseUrl() + path;
        
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }
        
        log.info("Target URL: {}", targetUrl);
        
        // 4. Copiar headers
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Não copiar headers de host, X-API-Key (interno) e cache/encoding headers
            String lowerName = headerName.toLowerCase();
            if (!lowerName.equals("host") && 
                !lowerName.equals("x-api-key") &&
                !lowerName.equals("if-none-match") &&
                !lowerName.equals("if-modified-since") &&
                !lowerName.equals("accept-encoding")) { // Não copiar accept-encoding
                headers.put(headerName, Collections.list(request.getHeaders(headerName)));
            }
        }
        
        log.info("Headers copiados: {}", headers.keySet());
        
        // 5. Fazer requisição
        try {
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            
            log.info("Enviando requisição {} para {}", method, targetUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                method,
                entity,
                String.class
            );
            
            log.info("Gateway response: {} from {}", response.getStatusCode(), targetUrl);
            String responseBody = response.getBody();
            log.info("Response body length: {}", responseBody != null ? responseBody.length() : 0);
            log.info("Response body first 100 chars: {}", responseBody != null && responseBody.length() > 0 ? responseBody.substring(0, Math.min(100, responseBody.length())) : "EMPTY");
            
            // Criar headers limpos (sem duplicar CORS)
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            String finalBody = responseBody != null ? responseBody : "{}";
            
            return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(finalBody);
                
        } catch (Exception e) {
            log.error("Error proxying request to {}: {}", targetUrl, e.getMessage(), e);
            
            String errorMessage = "Erro ao acessar API: " + e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " | Causa: " + e.getCause().getMessage();
            }
            
            return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorMessage);
        }
    }
    
    private String extractPath(String requestUri, String slug) {
        String prefix = "/gateway/api/" + slug;
        if (requestUri.startsWith(prefix)) {
            return requestUri.substring(prefix.length());
        }
        return "";
    }
}
