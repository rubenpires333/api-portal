package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job agendado que libera transações após o período de holdback (14 dias)
 * 
 * DESABILITADO: Usar HoldbackReleaseJob em vez deste
 */
//@Service
@RequiredArgsConstructor
@Slf4j
public class HoldbackReleaseScheduler {

    private final WalletTransactionRepository transactionRepository;
    private final WalletService walletService;

    @Scheduled(cron = "0 0 2 * * *") // Executa diariamente às 2h da manhã
    @Transactional
    public void releaseHoldbackTransactions() {
        log.info("Starting holdback release job");

        List<WalletTransaction> pendingTransactions = transactionRepository
            .findPendingTransactionsReadyForRelease(TransactionStatus.PENDING, LocalDateTime.now());

        log.info("Found {} transactions ready for release", pendingTransactions.size());

        for (WalletTransaction transaction : pendingTransactions) {
            try {
                // Mover de pending para available
                transaction.setStatus(TransactionStatus.AVAILABLE);
                transactionRepository.save(transaction);

                // Atualizar saldos da wallet
                var wallet = transaction.getWallet();
                wallet.setPendingBalance(wallet.getPendingBalance().subtract(transaction.getAmount()));
                wallet.setAvailableBalance(wallet.getAvailableBalance().add(transaction.getAmount()));
                walletService.updateBalance(wallet, transaction.getAmount(), "available");

                log.info("Released transaction: id={}, amount={}, wallet={}", 
                    transaction.getId(), transaction.getAmount(), wallet.getId());
            } catch (Exception e) {
                log.error("Error releasing transaction: id={}", transaction.getId(), e);
            }
        }

        log.info("Holdback release job completed");
    }
}
