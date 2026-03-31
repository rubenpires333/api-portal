package com.api_portal.backend.modules.gateway.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.service.SubscriptionService;
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
    private final SubscriptionService subscriptionService;
    
    public ResponseEntity<?> proxyRequest(String slug, HttpServletRequest request, String body) {
        log.info("=== GATEWAY REQUEST ===");
        log.info("Slug: {}", slug);
        log.info("Method: {}", request.getMethod());
        log.info("URI: {}", request.getRequestURI());
        log.info("Query: {}", request.getQueryString());
        
        // 1. Validar API Key
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("API Key não fornecida");
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"Unauthorized\",\"message\":\"API Key é obrigatória. Adicione o header X-API-Key.\"}");
        }
        
        // 2. Buscar API pelo slug primeiro (para verificar se é teste do provider)
        Api api = apiRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException("API não encontrada: " + slug));
        
        log.info("API encontrada: {} ({})", api.getName(), api.getBaseUrl());
        
        // 3. Verificar se é uma API Key de teste do provider
        boolean isProviderTest = false;
        String consumerId = null;
        String consumerEmail = null;
        
        // API Key de teste do provider tem formato: test_provider_{providerId}_{apiId}
        if (apiKey.startsWith("test_provider_")) {
            String[] parts = apiKey.split("_");
            if (parts.length >= 4) {
                String providerId = parts[2];
                // Verificar se o provider é dono da API
                if (api.getProviderId().equals(providerId)) {
                    isProviderTest = true;
                    consumerId = providerId;
                    consumerEmail = api.getProviderEmail();
                    log.info("Provider test mode: {} testando sua própria API", api.getProviderName());
                } else {
                    log.error("Provider {} tentando testar API de outro provider", providerId);
                    return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Forbidden\",\"message\":\"Você não tem permissão para testar esta API.\"}");
                }
            }
        }
        
        // 4. Se não for teste do provider, validar subscrição normal
        if (!isProviderTest) {
            Subscription subscription;
            try {
                log.info("Validando API Key de subscription: {}", apiKey);
                subscription = subscriptionService.validateApiKey(apiKey);
                log.info("API Key válida para consumer: {} (Status: {})", subscription.getConsumerEmail(), subscription.getStatus());
                
                consumerId = subscription.getConsumerId();
                consumerEmail = subscription.getConsumerEmail();
                
                // Adicionar informações da subscrição ao request para audit log
                request.setAttribute("subscriptionId", subscription.getId().toString());
                request.setAttribute("consumerId", subscription.getConsumerId());
                request.setAttribute("consumerEmail", subscription.getConsumerEmail());
                
                // Verificar se a subscrição é para esta API
                if (!subscription.getApi().getId().equals(api.getId())) {
                    log.error("API Key não autorizada para esta API");
                    return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Forbidden\",\"message\":\"API Key não autorizada para esta API.\"}");
                }
            } catch (IllegalArgumentException e) {
                log.error("API Key inválida: {} - Motivo: {}", apiKey, e.getMessage());
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Unauthorized\",\"message\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                log.error("Erro inesperado ao validar API Key: {}", apiKey, e);
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Unauthorized\",\"message\":\"API Key inválida ou inativa.\"}");
            }
        } else {
            // Para teste do provider, adicionar informações ao request
            request.setAttribute("isProviderTest", "true");
            request.setAttribute("consumerId", consumerId);
            request.setAttribute("consumerEmail", consumerEmail);
        }
        
        // 5. Verificar se API está ativa
        if (!api.getIsActive()) {
            log.error("API inativa: {}", slug);
            throw new ApiException("API está inativa");
        }
        
        // 6. Construir URL de destino
        String path = extractPath(request.getRequestURI(), slug);
        String targetUrl = api.getBaseUrl() + path;
        
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }
        
        log.info("Target URL: {}", targetUrl);
        
        // 7. Copiar headers
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
        
        // 8. Fazer requisição
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
            
            // Validar tamanho da resposta (limite: 5MB)
            final int MAX_RESPONSE_SIZE = 5 * 1024 * 1024; // 5MB
            if (responseBody != null && responseBody.length() > MAX_RESPONSE_SIZE) {
                log.warn("Response too large: {} bytes (max: {})", responseBody.length(), MAX_RESPONSE_SIZE);
                return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Response too large\",\"message\":\"A resposta da API excede o limite de 5MB. Use paginação ou filtre os dados.\",\"size\":" + responseBody.length() + ",\"maxSize\":" + MAX_RESPONSE_SIZE + "}");
            }
            
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
