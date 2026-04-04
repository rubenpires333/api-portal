package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.WalletSummaryDTO;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.service.WalletService;
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
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @RequiresPermission("wallet.view")
    public ResponseEntity<WalletSummaryDTO> getWalletSummary(@RequestParam UUID providerId) {
        WalletSummaryDTO summary = walletService.getWalletSummary(providerId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/transactions")
    @RequiresPermission("wallet.view")
    public ResponseEntity<Page<WalletTransaction>> getTransactionHistory(
            @RequestParam UUID providerId,
            Pageable pageable) {
        
        Page<WalletTransaction> transactions = walletService.getTransactionHistory(providerId, pageable);
        return ResponseEntity.ok(transactions);
    }
}
