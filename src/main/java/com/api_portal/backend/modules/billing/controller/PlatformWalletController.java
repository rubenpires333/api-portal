package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.PlatformSubscriptionDTO;
import com.api_portal.backend.modules.billing.dto.PlatformTransactionDTO;
import com.api_portal.backend.modules.billing.dto.PlatformWalletSummaryDTO;
import com.api_portal.backend.modules.billing.service.PlatformWalletService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/platform-wallet")
@RequiredArgsConstructor
@Slf4j
@RequiresPermission("billing.manage")
public class PlatformWalletController {

    private final PlatformWalletService platformWalletService;

    /**
     * GET /api/v1/admin/platform-wallet/summary
     * Retorna resumo financeiro da plataforma
     */
    @GetMapping("/summary")
    public ResponseEntity<PlatformWalletSummaryDTO> getPlatformSummary() {
        log.info("Admin requesting platform wallet summary");
        PlatformWalletSummaryDTO summary = platformWalletService.getPlatformSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/admin/platform-wallet/transactions
     * Retorna todas as transações de receita da plataforma
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<PlatformTransactionDTO>> getPlatformTransactions(Pageable pageable) {
        log.info("Admin requesting platform transactions: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        Page<PlatformTransactionDTO> transactions = platformWalletService.getPlatformTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/v1/admin/platform-wallet/transactions/period
     * Retorna transações de um período específico
     */
    @GetMapping("/transactions/period")
    public ResponseEntity<Page<PlatformTransactionDTO>> getTransactionsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        
        log.info("Admin requesting platform transactions by period: start={}, end={}", startDate, endDate);
        Page<PlatformTransactionDTO> transactions = platformWalletService
            .getTransactionsByPeriod(startDate, endDate, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/v1/admin/platform-wallet/subscriptions
     * Retorna todas as subscriptions (qualquer status)
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<Page<PlatformSubscriptionDTO>> getAllSubscriptions(Pageable pageable) {
        log.info("Admin requesting all subscriptions: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        Page<PlatformSubscriptionDTO> subscriptions = platformWalletService.getAllSubscriptions(pageable);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * GET /api/v1/admin/platform-wallet/subscriptions/active
     * Retorna apenas subscriptions ativas
     */
    @GetMapping("/subscriptions/active")
    public ResponseEntity<Page<PlatformSubscriptionDTO>> getActiveSubscriptions(Pageable pageable) {
        log.info("Admin requesting active subscriptions: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        Page<PlatformSubscriptionDTO> subscriptions = platformWalletService.getActiveSubscriptions(pageable);
        return ResponseEntity.ok(subscriptions);
    }
}
