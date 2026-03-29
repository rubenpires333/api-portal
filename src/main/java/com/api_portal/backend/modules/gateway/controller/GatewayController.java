package com.api_portal.backend.modules.gateway.controller;

import com.api_portal.backend.modules.gateway.service.GatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Gateway", description = "Gateway intermediário para APIs")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class GatewayController {
    
    private final GatewayService gatewayService;
    
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
}
