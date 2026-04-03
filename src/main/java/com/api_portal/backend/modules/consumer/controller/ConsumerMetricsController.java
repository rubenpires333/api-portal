package com.api_portal.backend.modules.consumer.controller;

import com.api_portal.backend.modules.consumer.dto.ConsumerMetricsResponse;
import com.api_portal.backend.modules.consumer.service.ConsumerMetricsService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/metrics")
@RequiredArgsConstructor
@Tag(name = "Consumer Metrics", description = "Métricas de uso para consumers")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsumerMetricsController {
    
    private final ConsumerMetricsService metricsService;
    
    @GetMapping
    @RequiresPermission("consumer.metrics.read")
    @Operation(summary = "Obter métricas do consumer", description = "Retorna métricas de uso das APIs pelo consumer")
    public ResponseEntity<ConsumerMetricsResponse> getConsumerMetrics(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        
        UUID consumerId = UUID.fromString(authentication.getName());
        log.info("Buscando métricas do consumer: {} para {} dias", consumerId, days);
        
        ConsumerMetricsResponse metrics = metricsService.getConsumerMetrics(consumerId, days);
        return ResponseEntity.ok(metrics);
    }
}
