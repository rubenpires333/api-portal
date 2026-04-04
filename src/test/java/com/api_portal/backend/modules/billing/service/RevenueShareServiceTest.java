package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import com.api_portal.backend.modules.billing.model.enums.TransactionType;
import com.api_portal.backend.modules.billing.repository.RevenueShareEventRepository;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueShareServiceTest {

    @Mock
    private WalletService walletService;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private RevenueShareEventRepository revenueShareEventRepository;

    @InjectMocks
    private RevenueShareService revenueShareService;

    private UUID subscriptionId;
    private UUID providerId;
    private ProviderWallet wallet;

    @BeforeEach
    void setUp() {
        subscriptionId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        
        wallet = ProviderWallet.builder()
            .id(UUID.randomUUID())
            .providerId(providerId)
            .availableBalance(BigDecimal.ZERO)
            .pendingBalance(BigDecimal.ZERO)
            .lifetimeEarned(BigDecimal.ZERO)
            .currency("USD")
            .build();
        
        // Configurar valores padrão via reflection
        ReflectionTestUtils.setField(revenueShareService, "platformCommissionPercentage", new BigDecimal("20.00"));
        ReflectionTestUtils.setField(revenueShareService, "holdbackDays", 14);
    }

    @Test
    void testProcessPayment_ShouldCalculateCorrectCommission() {
        // Given
        BigDecimal totalAmount = new BigDecimal("100.00");
        String currency = "USD";
        
        when(walletService.getOrCreateWallet(providerId)).thenReturn(wallet);
        when(transactionRepository.save(any(WalletTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        revenueShareService.processPayment(subscriptionId, providerId, totalAmount, currency);

        // Then
        verify(transactionRepository).save(argThat(transaction ->
            transaction.getAmount().compareTo(new BigDecimal("80.00")) == 0 && // 100 - 20% = 80
            transaction.getType() == TransactionType.CREDIT_REVENUE &&
            transaction.getStatus() == TransactionStatus.PENDING
        ));
        
        verify(revenueShareEventRepository).save(argThat(event ->
            event.getPlatformCommission().compareTo(new BigDecimal("20.00")) == 0 &&
            event.getProviderShare().compareTo(new BigDecimal("80.00")) == 0
        ));
    }

    @Test
    void testProcessPayment_ShouldUpdateWalletBalances() {
        // Given
        BigDecimal totalAmount = new BigDecimal("100.00");
        String currency = "USD";
        
        when(walletService.getOrCreateWallet(providerId)).thenReturn(wallet);
        when(transactionRepository.save(any(WalletTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        revenueShareService.processPayment(subscriptionId, providerId, totalAmount, currency);

        // Then
        verify(walletService).updateBalance(eq(wallet), eq(BigDecimal.ZERO), eq("pending"));
    }
}
