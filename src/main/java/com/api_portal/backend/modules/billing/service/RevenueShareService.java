package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.RevenueShareEvent;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import com.api_portal.backend.modules.billing.model.enums.TransactionType;
import com.api_portal.backend.modules.billing.repository.RevenueShareEventRepository;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueShareService {

    private final WalletService walletService;
    private final WalletTransactionRepository transactionRepository;
    private final RevenueShareEventRepository revenueShareEventRepository;

    @Value("${billing.platform-commission-percentage:20.00}")
    private BigDecimal platformCommissionPercentage;

    @Value("${billing.holdback-days:14}")
    private int holdbackDays;

    @Transactional
    public void processPayment(UUID subscriptionId, UUID providerId, BigDecimal totalAmount, String currency) {
        log.info("Processing payment: subscription={}, provider={}, amount={}", 
            subscriptionId, providerId, totalAmount);

        // Calcular comissão da plataforma
        BigDecimal platformCommission = totalAmount
            .multiply(platformCommissionPercentage)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal providerShare = totalAmount.subtract(platformCommission);

        // Criar evento de revenue share
        RevenueShareEvent event = RevenueShareEvent.builder()
            .subscriptionId(subscriptionId)
            .providerId(providerId)
            .totalAmount(totalAmount)
            .platformCommissionPercentage(platformCommissionPercentage)
            .platformCommission(platformCommission)
            .providerShare(providerShare)
            .currency(currency)
            .build();

        // Obter ou criar wallet do provider
        ProviderWallet wallet = walletService.getOrCreateWallet(providerId);

        // Criar transação com holdback
        LocalDateTime availableAt = LocalDateTime.now().plusDays(holdbackDays);
        WalletTransaction transaction = WalletTransaction.builder()
            .wallet(wallet)
            .amount(providerShare)
            .type(TransactionType.CREDIT_REVENUE)
            .status(TransactionStatus.PENDING)
            .referenceId(subscriptionId)
            .description("Revenue from subscription " + subscriptionId)
            .availableAt(availableAt)
            .build();

        transaction = transactionRepository.save(transaction);
        event.setWalletTransactionId(transaction.getId());
        revenueShareEventRepository.save(event);

        // Atualizar saldo pendente
        wallet.setPendingBalance(wallet.getPendingBalance().add(providerShare));
        wallet.setLifetimeEarned(wallet.getLifetimeEarned().add(providerShare));
        walletService.updateBalance(wallet, BigDecimal.ZERO, "pending");

        log.info("Payment processed: platformCommission={}, providerShare={}, availableAt={}", 
            platformCommission, providerShare, availableAt);
    }
}
