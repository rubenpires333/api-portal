package com.api_portal.backend.modules.gateway.controller;

import com.api_portal.backend.modules.gateway.service.GatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Gateway", description = "Gateway intermediário para APIs")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class GatewayController {
    
    private final GatewayService gatewayService;
    private final RestTemplate restTemplate;
    
    @RequestMapping(value = "/gateway/api/**", method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH,
        RequestMethod.OPTIONS
    })
    @Operation(summary = "Proxy para API do provider")
    public ResponseEntity<?> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {
        
        // Tratar OPTIONS (CORS preflight)
        if (request.getMethod().equals("OPTIONS")) {
            return ResponseEntity.ok().build();
        }
        
        // Extrair slug do path
        String path = request.getRequestURI();
        String prefix = "/gateway/api/";
        
        if (!path.startsWith(prefix)) {
            log.error("Path inválido: {}", path);
            return ResponseEntity.badRequest().body("Path inválido");
        }
        
        String remainingPath = path.substring(prefix.length());
        String slug = remainingPath.split("/")[0];
        
        log.info("Gateway request: {} {} for API: {}", 
            request.getMethod(), 
            request.getRequestURI(), 
            slug);
        
        return gatewayService.proxyRequest(slug, request, body);
    }
    
    @PostMapping("/api/v1/gateway/test")
    @Operation(summary = "Endpoint de teste para providers testarem suas APIs sem CORS")
    public ResponseEntity<String> testEndpoint(@RequestBody Map<String, Object> testRequest) {
        try {
            String url = (String) testRequest.get("url");
            String method = (String) testRequest.get("method");
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) testRequest.get("headers");
            Object bodyObj = testRequest.get("body");
            String body = bodyObj != null ? bodyObj.toString() : null;
            
            if (url == null || method == null) {
                return ResponseEntity.badRequest().body("URL e método são obrigatórios");
            }
            
            log.info("Provider test request: {} {}", method, url);
            log.info("Headers: {}", headers);
            
            // Preparar headers
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            // Fazer requisição
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method),
                entity,
                String.class
            );
            
            return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
                
        } catch (Exception e) {
            log.error("Erro ao testar endpoint: ", e);
            
            // Tentar extrair resposta do erro
            if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
                org.springframework.web.client.HttpStatusCodeException httpError = 
                    (org.springframework.web.client.HttpStatusCodeException) e;
                return ResponseEntity.status(httpError.getStatusCode())
                    .body(httpError.getResponseBodyAsString());
            }
            
            return ResponseEntity.status(500).body("Erro ao testar endpoint: " + e.getMessage());
        }
    }
}
