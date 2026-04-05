package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.PlatformSubscriptionDTO;
import com.api_portal.backend.modules.billing.dto.PlatformTransactionDTO;
import com.api_portal.backend.modules.billing.dto.PlatformWalletSummaryDTO;
import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.TransactionType;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
import com.api_portal.backend.modules.billing.repository.ProviderPlatformSubscriptionRepository;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import com.api_portal.backend.modules.billing.repository.WithdrawalRequestRepository;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformWalletService {

    private final WalletTransactionRepository transactionRepository;
    private final ProviderPlatformSubscriptionRepository subscriptionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;

    /**
     * Retorna resumo financeiro da plataforma
     */
    @Transactional(readOnly = true)
    public PlatformWalletSummaryDTO getPlatformSummary() {
        log.info("Generating platform wallet summary");

        // Receita de subscriptions (100% para plataforma) - DEBIT_PLATFORM_FEE
        BigDecimal subscriptionRevenue = transactionRepository
            .sumByType(TransactionType.DEBIT_PLATFORM_FEE)
            .orElse(BigDecimal.ZERO);

        // Receita de comissões de API (20% de cada pagamento) - CREDIT_REVENUE
        BigDecimal apiCommissionRevenue = transactionRepository
            .sumByType(TransactionType.CREDIT_REVENUE)
            .orElse(BigDecimal.ZERO);

        // Taxas de levantamento
        BigDecimal withdrawalFees = withdrawalRequestRepository
            .sumFeesByStatus(WithdrawalStatus.COMPLETED)
            .orElse(BigDecimal.ZERO);

        // Total geral
        BigDecimal totalRevenue = subscriptionRevenue
            .add(apiCommissionRevenue)
            .add(withdrawalFees);

        // Subscriptions ativas
        Long activeSubscriptions = subscriptionRepository
            .countByStatus("active");

        // MRR - Receita mensal recorrente
        BigDecimal monthlyRecurringRevenue = subscriptionRepository
            .sumAmountByStatus("active")
            .orElse(BigDecimal.ZERO);

        // Levantamentos pendentes
        Long pendingWithdrawals = withdrawalRequestRepository
            .countByStatus(WithdrawalStatus.PENDING_APPROVAL);
        
        BigDecimal pendingWithdrawalsAmount = withdrawalRequestRepository
            .sumAmountByStatus(WithdrawalStatus.PENDING_APPROVAL)
            .orElse(BigDecimal.ZERO);

        // Levantamentos concluídos
        Long completedWithdrawals = withdrawalRequestRepository
            .countByStatus(WithdrawalStatus.COMPLETED);
        
        BigDecimal completedWithdrawalsAmount = withdrawalRequestRepository
            .sumAmountByStatus(WithdrawalStatus.COMPLETED)
            .orElse(BigDecimal.ZERO);

        // Providers
        Long totalProviders = userRepository.countByRolesContaining("PROVIDER");
        Long activeProviders = subscriptionRepository.countDistinctProvidersByStatus("active");

        return PlatformWalletSummaryDTO.builder()
            .totalSubscriptionRevenue(subscriptionRevenue)
            .totalApiCommissionRevenue(apiCommissionRevenue)
            .totalWithdrawalFees(withdrawalFees)
            .totalRevenue(totalRevenue)
            .activeSubscriptions(activeSubscriptions)
            .monthlyRecurringRevenue(monthlyRecurringRevenue)
            .pendingWithdrawals(pendingWithdrawals)
            .pendingWithdrawalsAmount(pendingWithdrawalsAmount)
            .completedWithdrawals(completedWithdrawals)
            .completedWithdrawalsAmount(completedWithdrawalsAmount)
            .totalProviders(totalProviders)
            .activeProviders(activeProviders)
            .period("Todos os tempos")
            .build();
    }

    /**
     * Retorna todas as transações da plataforma (receitas)
     */
    @Transactional(readOnly = true)
    public Page<PlatformTransactionDTO> getPlatformTransactions(Pageable pageable) {
        log.info("Fetching platform transactions: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());

        // Buscar apenas transações de receita da plataforma
        List<TransactionType> platformRevenueTypes = List.of(
            TransactionType.DEBIT_PLATFORM_FEE,
            TransactionType.CREDIT_REVENUE
        );

        Page<WalletTransaction> transactions = transactionRepository
            .findByTypeInOrderByCreatedAtDesc(platformRevenueTypes, pageable);

        return transactions.map(this::mapToTransactionDTO);
    }

    /**
     * Retorna todas as subscriptions ativas
     */
    @Transactional(readOnly = true)
    public Page<PlatformSubscriptionDTO> getActiveSubscriptions(Pageable pageable) {
        log.info("Fetching active subscriptions: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());

        Page<ProviderPlatformSubscription> subscriptions = subscriptionRepository
            .findByStatusOrderByCreatedAtDesc("active", pageable);

        return subscriptions.map(this::mapToSubscriptionDTO);
    }

    /**
     * Retorna todas as subscriptions (qualquer status)
     */
    @Transactional(readOnly = true)
    public Page<PlatformSubscriptionDTO> getAllSubscriptions(Pageable pageable) {
        log.info("Fetching all subscriptions: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());

        Page<ProviderPlatformSubscription> subscriptions = subscriptionRepository
            .findAllByOrderByCreatedAtDesc(pageable);

        return subscriptions.map(this::mapToSubscriptionDTO);
    }

    /**
     * Retorna transações de um período específico
     */
    @Transactional(readOnly = true)
    public Page<PlatformTransactionDTO> getTransactionsByPeriod(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        
        log.info("Fetching platform transactions by period: start={}, end={}", startDate, endDate);

        List<TransactionType> platformRevenueTypes = List.of(
            TransactionType.DEBIT_PLATFORM_FEE,
            TransactionType.CREDIT_REVENUE
        );

        Page<WalletTransaction> transactions = transactionRepository
            .findByTypeInAndCreatedAtBetweenOrderByCreatedAtDesc(
                platformRevenueTypes, startDate, endDate, pageable);

        return transactions.map(this::mapToTransactionDTO);
    }

    /**
     * Mapeia WalletTransaction para DTO
     */
    private PlatformTransactionDTO mapToTransactionDTO(WalletTransaction transaction) {
        User provider = userRepository.findById(transaction.getWallet().getProviderId())
            .orElse(null);

        PlatformTransactionDTO.ProviderInfo providerInfo = null;
        if (provider != null) {
            providerInfo = PlatformTransactionDTO.ProviderInfo.builder()
                .id(provider.getId())
                .name(provider.getFirstName() + " " + provider.getLastName())
                .email(provider.getEmail())
                .username(provider.getUsername())
                .build();
        }

        // Tentar obter nome do plano se for subscription
        String planName = null;
        if (transaction.getType() == TransactionType.DEBIT_PLATFORM_FEE 
            && transaction.getReferenceId() != null) {
            planName = subscriptionRepository.findById(transaction.getReferenceId())
                .map(sub -> sub.getPlan().getDisplayName())
                .orElse(null);
        }

        return PlatformTransactionDTO.builder()
            .id(transaction.getId())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .description(transaction.getDescription())
            .createdAt(transaction.getCreatedAt())
            .provider(providerInfo)
            .planName(planName)
            .referenceId(transaction.getReferenceId())
            .build();
    }

    /**
     * Mapeia PlatformSubscription para DTO
     */
    private PlatformSubscriptionDTO mapToSubscriptionDTO(ProviderPlatformSubscription subscription) {
        User provider = userRepository.findById(subscription.getProviderId())
            .orElse(null);

        PlatformSubscriptionDTO.ProviderInfo providerInfo = null;
        if (provider != null) {
            providerInfo = PlatformSubscriptionDTO.ProviderInfo.builder()
                .id(provider.getId())
                .name(provider.getFirstName() + " " + provider.getLastName())
                .email(provider.getEmail())
                .username(provider.getUsername())
                .build();
        }

        return PlatformSubscriptionDTO.builder()
            .id(subscription.getId())
            .planName(subscription.getPlan().getDisplayName())
            .status(subscription.getStatus())
            .amount(subscription.getPlan().getMonthlyPrice())
            .currency(subscription.getPlan().getCurrency())
            .currentPeriodStart(subscription.getCurrentPeriodStart())
            .currentPeriodEnd(subscription.getCurrentPeriodEnd())
            .createdAt(subscription.getCreatedAt())
            .cancelledAt(subscription.getCancelAtPeriodEnd() ? subscription.getCurrentPeriodEnd() : null)
            .provider(providerInfo)
            .stripeSubscriptionId(subscription.getStripeSubscriptionId())
            .stripeCustomerId(subscription.getStripeCustomerId())
            .build();
    }
}
