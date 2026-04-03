package com.api_portal.backend.modules.provider.controller;

import com.api_portal.backend.modules.provider.dto.ProviderMetricsResponse;
import com.api_portal.backend.modules.provider.service.ProviderMetricsService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/provider/metrics")
@RequiredArgsConstructor
@Tag(name = "Provider Metrics", description = "Métricas para providers")
@SecurityRequirement(name = "Bearer Authentication")
public class ProviderMetricsController {
    
    private final ProviderMetricsService metricsService;
    
    @GetMapping
    @RequiresPermission("provider.metrics.read")
    @Operation(summary = "Obter métricas do provider", description = "Retorna métricas de uso das APIs do provider")
    public ResponseEntity<ProviderMetricsResponse> getProviderMetrics(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        
        String providerId = authentication.getName();
        log.info("Buscando métricas do provider: {} para {} dias", providerId, days);
        
        ProviderMetricsResponse metrics = metricsService.getProviderMetrics(providerId, days);
        return ResponseEntity.ok(metrics);
    }
}
