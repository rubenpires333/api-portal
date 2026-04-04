package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.WithdrawalFeeRuleDTO;
import com.api_portal.backend.modules.billing.model.WithdrawalFeeRule;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalMethod;
import com.api_portal.backend.modules.billing.service.WithdrawalFeeRuleService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/billing/fee-rules")
@RequiredArgsConstructor
@RequiresPermission("billing.manage")
public class AdminFeeRuleController {

    private final WithdrawalFeeRuleService feeRuleService;

    @GetMapping
    public ResponseEntity<List<WithdrawalFeeRule>> getAllFeeRules() {
        return ResponseEntity.ok(feeRuleService.getAllFeeRules());
    }

    @GetMapping("/{method}")
    public ResponseEntity<WithdrawalFeeRule> getFeeRuleByMethod(@PathVariable WithdrawalMethod method) {
        return ResponseEntity.ok(feeRuleService.getFeeRuleByMethod(method));
    }

    @PostMapping
    public ResponseEntity<WithdrawalFeeRule> createFeeRule(
            @RequestBody WithdrawalFeeRuleDTO dto,
            @RequestParam UUID adminId) {
        return ResponseEntity.ok(feeRuleService.createFeeRule(dto, adminId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WithdrawalFeeRule> updateFeeRule(
            @PathVariable UUID id,
            @RequestBody WithdrawalFeeRuleDTO dto,
            @RequestParam UUID adminId) {
        return ResponseEntity.ok(feeRuleService.updateFeeRule(id, dto, adminId));
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleFeeRuleStatus(@PathVariable UUID id) {
        feeRuleService.toggleFeeRuleStatus(id);
        return ResponseEntity.ok().build();
    }
}
