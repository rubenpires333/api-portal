package com.api_portal.backend.modules.billing.job;

import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import com.api_portal.backend.modules.billing.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job para liberar fundos após período de holdback
 * Executa a cada hora
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HoldbackReleaseJob {

    private final WalletTransactionRepository transactionRepository;
    private final WalletService walletService;

    @Scheduled(cron = "0 0 * * * *") // A cada minuto (para testes)
    @Transactional
    public void releaseHoldbackFunds() {
        log.info("=== INICIANDO JOB DE LIBERAÇÃO DE HOLDBACK ===");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Buscar transações pendentes que já passaram do período de holdback
        List<WalletTransaction> pendingTransactions = transactionRepository
            .findByStatusAndAvailableAtBefore(TransactionStatus.PENDING, now);
        
        if (pendingTransactions.isEmpty()) {
            log.info("Nenhuma transação para liberar");
            return;
        }
        
        log.info("Encontradas {} transações para liberar", pendingTransactions.size());
        
        for (WalletTransaction transaction : pendingTransactions) {
            try {
                log.info("Liberando transação: id={}, amount={}, wallet={}", 
                         transaction.getId(), transaction.getAmount(), transaction.getWallet().getId());
                
                // Atualizar status da transação
                transaction.setStatus(TransactionStatus.AVAILABLE);
                transactionRepository.save(transaction);
                
                // Mover saldo de pending para available
                var wallet = transaction.getWallet();
                wallet.setPendingBalance(wallet.getPendingBalance().subtract(transaction.getAmount()));
                wallet.setAvailableBalance(wallet.getAvailableBalance().add(transaction.getAmount()));
                walletService.updateBalance(wallet, transaction.getAmount(), "available");
                
                log.info("✅ Transação liberada: id={}, amount={}", 
                         transaction.getId(), transaction.getAmount());
                
            } catch (Exception e) {
                log.error("❌ Erro ao liberar transação: id={}", transaction.getId(), e);
            }
        }
        
        log.info("=== FIM JOB DE LIBERAÇÃO DE HOLDBACK: {} transações processadas ===", 
                 pendingTransactions.size());
    }
}
