package com.api_portal.backend.modules.analytics.controller;

import com.api_portal.backend.modules.analytics.dto.AnalyticsSummaryResponse;
import com.api_portal.backend.modules.analytics.dto.ApiAnalyticsResponse;
import com.api_portal.backend.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Estatísticas e métricas de uso das APIs")
@SecurityRequirement(name = "bearer-jwt")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/provider/summary")
    @Operation(summary = "Resumo geral de estatísticas do provider")
    public ResponseEntity<AnalyticsSummaryResponse> getProviderSummary(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endDate,
            Authentication authentication) {
        
        // Valores padrão: últimos 30 dias
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        AnalyticsSummaryResponse summary = analyticsService.getProviderSummary(
            authentication, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/provider/apis/{apiId}")
    @Operation(summary = "Estatísticas detalhadas de uma API específica")
    public ResponseEntity<ApiAnalyticsResponse> getApiAnalytics(
            @PathVariable UUID apiId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endDate,
            Authentication authentication) {
        
        // Valores padrão: últimos 30 dias
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        ApiAnalyticsResponse analytics = analyticsService.getApiAnalytics(
            apiId, authentication, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
}
