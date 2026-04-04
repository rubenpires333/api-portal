package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.WithdrawalRequestDTO;
import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.service.WithdrawalService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider/wallet")
@RequiredArgsConstructor
@RequiresPermission("wallet.withdraw")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawalRequest> requestWithdrawal(
            @RequestParam UUID providerId,
            @RequestBody WithdrawalRequestDTO dto) {
        
        WithdrawalRequest request = withdrawalService.requestWithdrawal(providerId, dto);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/withdraw/{id}")
    public ResponseEntity<WithdrawalRequest> getWithdrawalStatus(@PathVariable UUID id) {
        // TODO: Implementar busca por ID
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<Void> cancelWithdrawal(@PathVariable UUID id) {
        // TODO: Implementar cancelamento
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
    public ResponseEntity<Page<WithdrawalRequest>> getPendingWithdrawals(Pageable pageable) {
        Page<WithdrawalRequest> requests = withdrawalService.getPendingWithdrawals(pageable);
        return ResponseEntity.ok(requests);
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
