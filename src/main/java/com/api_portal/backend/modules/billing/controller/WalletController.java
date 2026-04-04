package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.WalletSummaryDTO;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<WalletSummaryDTO> getWalletSummary(@RequestParam UUID providerId) {
        WalletSummaryDTO summary = walletService.getWalletSummary(providerId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Page<WalletTransaction>> getTransactionHistory(
            @RequestParam UUID providerId,
            Pageable pageable) {
        
        Page<WalletTransaction> transactions = walletService.getTransactionHistory(providerId, pageable);
        return ResponseEntity.ok(transactions);
    }
}
