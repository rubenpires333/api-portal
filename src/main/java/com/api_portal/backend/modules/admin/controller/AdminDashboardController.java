package com.api_portal.backend.modules.admin.controller;

import com.api_portal.backend.modules.admin.dto.DashboardStatsResponse;
import com.api_portal.backend.modules.admin.dto.PendingSubscriptionResponse;
import com.api_portal.backend.modules.admin.dto.RecentActivityResponse;
import com.api_portal.backend.modules.admin.dto.SystemAlertsResponse;
import com.api_portal.backend.modules.admin.dto.TopRankingsResponse;
import com.api_portal.backend.modules.admin.dto.UsageMetricsResponse;
import com.api_portal.backend.modules.admin.service.AdminDashboardService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Dashboard administrativo")
@SecurityRequirement(name = "bearer-jwt")
public class AdminDashboardController {
    
    private final AdminDashboardService dashboardService;
    
    @GetMapping("/stats")
    @RequiresPermission("admin.dashboard.view")
    @Operation(summary = "Obter estatísticas do dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
    
    @GetMapping("/activities")
    @RequiresPermission("admin.dashboard.view")
    @Operation(summary = "Obter atividades recentes")
    public ResponseEntity<List<RecentActivityResponse>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentActivities(limit));
    }
    
    @GetMapping("/pending-subscriptions")
    @RequiresPermission("admin.dashboard.view")
    @Operation(summary = "Obter subscriptions pendentes")
    public ResponseEntity<List<PendingSubscriptionResponse>> getPendingSubscriptions() {
        return ResponseEntity.ok(dashboardService.getPendingSubscriptions());
    }
    
    @GetMapping("/rankings")
    @RequiresPermission("admin.dashboard.view")
    @Operation(summary = "Obter top rankings")
    public ResponseEntity<TopRankingsResponse> getTopRankings() {
        return ResponseEntity.ok(dashboardService.getTopRankings());
    }
    
    @GetMapping("/alerts")
    @RequiresPermission("admin.dashboard.view")
    @Operation(summary = "Obter alertas do sistema")
    public ResponseEntity<SystemAlertsResponse> getSystemAlerts() {
        return ResponseEntity.ok(dashboardService.getSystemAlerts());
    }
    
    @GetMapping("/usage-metrics")
    @RequiresPermission("admin.dashboard.view")
    @Operation(summary = "Obter métricas de uso e performance")
    public ResponseEntity<UsageMetricsResponse> getUsageMetrics(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getUsageMetrics(days));
    }
}
