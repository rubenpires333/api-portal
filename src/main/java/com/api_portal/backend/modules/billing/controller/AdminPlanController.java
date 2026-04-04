package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.PlatformPlanDTO;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.service.PlatformPlanService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/billing/plans")
@RequiredArgsConstructor
@RequiresPermission("billing.manage")
public class AdminPlanController {

    private final PlatformPlanService planService;

    @GetMapping
    public ResponseEntity<List<PlatformPlan>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @GetMapping("/active")
    public ResponseEntity<List<PlatformPlan>> getActivePlans() {
        return ResponseEntity.ok(planService.getActivePlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlatformPlan> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @PostMapping
    public ResponseEntity<PlatformPlan> createPlan(@RequestBody PlatformPlanDTO dto) {
        return ResponseEntity.ok(planService.createPlan(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatformPlan> updatePlan(
            @PathVariable UUID id,
            @RequestBody PlatformPlanDTO dto) {
        return ResponseEntity.ok(planService.updatePlan(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> togglePlanStatus(@PathVariable UUID id) {
        planService.togglePlanStatus(id);
        return ResponseEntity.ok().build();
    }
}
