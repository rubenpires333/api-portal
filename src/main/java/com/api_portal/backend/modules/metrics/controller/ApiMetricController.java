package com.api_portal.backend.modules.metrics.controller;

import com.api_portal.backend.modules.metrics.dto.ApiCallMetricRequest;
import com.api_portal.backend.modules.metrics.service.ApiMetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class ApiMetricController {
    
    private final ApiMetricService metricService;
    
    /**
     * Endpoint para registrar uma chamada de API
     * Este endpoint seria chamado por um API Gateway ou proxy
     */
    @PostMapping("/record")
    public ResponseEntity<Void> recordApiCall(@Valid @RequestBody ApiCallMetricRequest request) {
        metricService.recordApiCall(request);
        return ResponseEntity.accepted().build();
    }
    
    /**
     * Endpoint para forçar agregação de métricas (apenas para testes/admin)
     */
    @PostMapping("/aggregate")
    public ResponseEntity<String> aggregateMetrics(@RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now().minusDays(1);
        metricService.aggregateDailyMetrics(targetDate);
        return ResponseEntity.ok("Métricas agregadas para: " + targetDate);
    }
}
