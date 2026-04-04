package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.WithdrawalRequestDTO;
import com.api_portal.backend.modules.billing.dto.WithdrawalRequestResponse;
import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.service.WithdrawalService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider/wallet")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping("/withdraw")
    @RequiresPermission("wallet.withdraw")
    public ResponseEntity<WithdrawalRequest> requestWithdrawal(
            @RequestParam UUID providerId,
            @RequestBody WithdrawalRequestDTO dto) {
        
        WithdrawalRequest request = withdrawalService.requestWithdrawal(providerId, dto);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/withdrawals")
    @RequiresPermission("wallet.withdraw")
    public ResponseEntity<Page<WithdrawalRequestResponse>> getMyWithdrawals(
            @RequestParam UUID providerId,
            Pageable pageable) {
        
        Page<WithdrawalRequestResponse> requests = withdrawalService.getProviderWithdrawals(providerId, pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/withdraw/{id}")
    @RequiresPermission("wallet.withdraw")
    public ResponseEntity<WithdrawalRequest> getWithdrawalStatus(
            @PathVariable UUID id,
            @RequestParam UUID providerId) {
        
        WithdrawalRequest request = withdrawalService.getWithdrawalById(id, providerId);
        return ResponseEntity.ok(request);
    }

    @DeleteMapping("/withdraw/{id}")
    @RequiresPermission("wallet.withdraw")
    public ResponseEntity<Void> cancelWithdrawal(
            @PathVariable UUID id,
            @RequestParam UUID providerId) {
        
        withdrawalService.cancelWithdrawal(id, providerId);
        return ResponseEntity.noContent().build();
    }
}

@RestController
@RequestMapping("/api/v1/admin/withdrawals")
@RequiredArgsConstructor
@RequiresPermission("billing.manage")
class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;

    @GetMapping
    public ResponseEntity<Page<WithdrawalRequestResponse>> getAllWithdrawals(Pageable pageable) {
        Page<WithdrawalRequestResponse> requests = withdrawalService.getAllWithdrawals(pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<WithdrawalRequestResponse>> getPendingWithdrawals(Pageable pageable) {
        Page<WithdrawalRequestResponse> requests = withdrawalService.getPendingWithdrawals(pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = withdrawalService.countPendingWithdrawals();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveWithdrawal(
            @PathVariable UUID id,
            @RequestParam UUID adminId) { 
        
        withdrawalService.approveWithdrawal(id, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectWithdrawal(
            @PathVariable UUID id,
            @RequestParam UUID adminId,
            @RequestParam String reason) {
        
        withdrawalService.rejectWithdrawal(id, adminId, reason);
        return ResponseEntity.ok().build();
    }
}
