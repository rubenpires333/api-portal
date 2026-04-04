package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.WalletSummaryDTO;
import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.repository.ProviderWalletRepository;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final ProviderWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public ProviderWallet getOrCreateWallet(UUID providerId) {
        return walletRepository.findByProviderId(providerId)
            .orElseGet(() -> {
                ProviderWallet wallet = ProviderWallet.builder()
                    .providerId(providerId)
                    .build();
                return walletRepository.save(wallet);
            });
    }

    @Transactional(readOnly = true)
    public WalletSummaryDTO getWalletSummary(UUID providerId) {
        ProviderWallet wallet = getOrCreateWallet(providerId);
        return WalletSummaryDTO.builder()
            .availableBalance(wallet.getAvailableBalance())
            .pendingBalance(wallet.getPendingBalance())
            .reservedBalance(wallet.getReservedBalance())
            .lifetimeEarned(wallet.getLifetimeEarned())
            .currency(wallet.getCurrency())
            .minimumPayout(wallet.getMinimumPayout())
            .build();
    }

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(UUID providerId, Pageable pageable) {
        ProviderWallet wallet = getOrCreateWallet(providerId);
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable);
    }

    @Transactional
    public void updateBalance(ProviderWallet wallet, BigDecimal amount, String balanceType) {
        switch (balanceType) {
            case "available" -> wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            case "pending" -> wallet.setPendingBalance(wallet.getPendingBalance().add(amount));
            case "reserved" -> wallet.setReservedBalance(wallet.getReservedBalance().add(amount));
        }
        walletRepository.save(wallet);
    }
}
