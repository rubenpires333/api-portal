package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.WithdrawalRequestDTO;
import com.api_portal.backend.modules.billing.dto.WithdrawalRequestResponse;
import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WithdrawalFeeRule;
import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.model.WalletTransaction;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
import com.api_portal.backend.modules.billing.model.enums.TransactionType;
import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import com.api_portal.backend.modules.billing.repository.WithdrawalFeeRuleRepository;
import com.api_portal.backend.modules.billing.repository.WithdrawalRequestRepository;
import com.api_portal.backend.modules.billing.repository.WalletTransactionRepository;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.notification.service.NotificationService;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WalletService walletService;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WithdrawalFeeRuleRepository feeRuleRepository;
    private final WalletTransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Value("${billing.auto-approve-threshold:0.00}")
    private BigDecimal autoApproveThreshold;

    @Transactional
    public WithdrawalRequest requestWithdrawal(UUID providerId, WithdrawalRequestDTO dto) {
        ProviderWallet wallet = walletService.getOrCreateWallet(providerId);

        // Validar saldo disponível
        if (wallet.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient available balance");
        }

        // Validar valor mínimo
        if (dto.getAmount().compareTo(wallet.getMinimumPayout()) < 0) {
            throw new IllegalArgumentException("Amount below minimum payout threshold");
        }

        // Obter regra de taxa
        WithdrawalFeeRule feeRule = feeRuleRepository
            .findByWithdrawalMethodAndActiveTrue(dto.getMethod())
            .orElseThrow(() -> new IllegalArgumentException("Withdrawal method not supported"));

        // Calcular taxas
        BigDecimal feeAmount = dto.getAmount()
            .multiply(feeRule.getFeePercentage())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            .add(feeRule.getFixedFee());

        BigDecimal netAmount = dto.getAmount().subtract(feeAmount);

        // Determinar status inicial
        WithdrawalStatus status = dto.getAmount().compareTo(autoApproveThreshold) <= 0
            ? WithdrawalStatus.APPROVED
            : WithdrawalStatus.PENDING_APPROVAL;

        // Criar pedido
        WithdrawalRequest request = WithdrawalRequest.builder()
            .wallet(wallet)
            .requestedAmount(dto.getAmount())
            .feePercentage(feeRule.getFeePercentage())
            .feeAmount(feeAmount)
            .netAmount(netAmount)
            .method(dto.getMethod())
            .destinationDetails(dto.getDestinationDetails())
            .status(status)
            .build();

        request = withdrawalRequestRepository.save(request);

        // Criar transação de levantamento no histórico
        WalletTransaction transaction = WalletTransaction.builder()
            .wallet(wallet)
            .amount(dto.getAmount().negate()) // Negativo porque é débito
            .type(TransactionType.DEBIT_WITHDRAWAL)
            .status(TransactionStatus.RESERVED)
            .referenceId(request.getId())
            .description("Solicitação de levantamento via " + dto.getMethod())
            .availableAt(java.time.LocalDateTime.now())
            .build();
        
        transactionRepository.save(transaction);

        // Reservar saldo
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(dto.getAmount()));
        wallet.setReservedBalance(wallet.getReservedBalance().add(dto.getAmount()));
        walletService.updateBalance(wallet, BigDecimal.ZERO, "reserved");

        // Notificar administradores se estiver pendente de aprovação
        if (status == WithdrawalStatus.PENDING_APPROVAL) {
            notifyAdminsAboutWithdrawal(request);
        }

        log.info("Withdrawal request created: id={}, amount={}, status={}", 
            request.getId(), dto.getAmount(), status);

        return request;
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponse> getAllWithdrawals(Pageable pageable) {
        return withdrawalRequestRepository.findAll(pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponse> getPendingWithdrawals(Pageable pageable) {
        return withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(
            WithdrawalStatus.PENDING_APPROVAL, pageable)
            .map(this::mapToResponse);
    }

    private WithdrawalRequestResponse mapToResponse(WithdrawalRequest request) {
        User user = userRepository.findById(request.getWallet().getProviderId())
            .orElse(null);
        
        WithdrawalRequestResponse.ProviderInfo providerInfo = null;
        if (user != null) {
            providerInfo = WithdrawalRequestResponse.ProviderInfo.builder()
                .id(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
        }
        
        return WithdrawalRequestResponse.builder()
            .id(request.getId())
            .requestedAmount(request.getRequestedAmount())
            .feePercentage(request.getFeePercentage())
            .feeAmount(request.getFeeAmount())
            .netAmount(request.getNetAmount())
            .method(request.getMethod())
            .destinationDetails(request.getDestinationDetails())
            .status(request.getStatus())
            .approvedBy(request.getApprovedBy())
            .rejectionReason(request.getRejectionReason())
            .requestedAt(request.getRequestedAt())
            .processedAt(request.getProcessedAt())
            .provider(providerInfo)
            .build();
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalRequestResponse> getProviderWithdrawals(UUID providerId, Pageable pageable) {
        ProviderWallet wallet = walletService.getOrCreateWallet(providerId);
        return withdrawalRequestRepository.findByWalletOrderByRequestedAtDesc(wallet, pageable)
            .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public WithdrawalRequest getWithdrawalById(UUID withdrawalId, UUID providerId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(withdrawalId)
            .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        // Verificar se o levantamento pertence ao provider
        if (!request.getWallet().getProviderId().equals(providerId)) {
            throw new IllegalArgumentException("Unauthorized access to withdrawal request");
        }

        return request;
    }

    @Transactional
    public void cancelWithdrawal(UUID withdrawalId, UUID providerId) {
        WithdrawalRequest request = getWithdrawalById(withdrawalId, providerId);

        // Só pode cancelar se estiver pendente
        if (request.getStatus() != WithdrawalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Can only cancel pending withdrawal requests");
        }

        request.setStatus(WithdrawalStatus.CANCELLED);
        withdrawalRequestRepository.save(request);

        // Devolver saldo reservado para disponível
        ProviderWallet wallet = request.getWallet();
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(request.getRequestedAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getRequestedAmount()));
        walletService.updateBalance(wallet, BigDecimal.ZERO, "available");

        log.info("Withdrawal cancelled: id={}, providerId={}", withdrawalId, providerId);
    }

    @Transactional
    public void approveWithdrawal(UUID withdrawalId, UUID adminId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(withdrawalId)
            .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        if (request.getStatus() != WithdrawalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Withdrawal is not pending approval");
        }

        request.setStatus(WithdrawalStatus.APPROVED);
        request.setApprovedBy(adminId);
        request.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(request);

        // Remover saldo reservado (o dinheiro sai da carteira)
        ProviderWallet wallet = request.getWallet();
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(request.getRequestedAmount()));
        walletService.updateBalance(wallet, BigDecimal.ZERO, "approved");

        // Atualizar transação para COMPLETED (levantamento concluído)
        transactionRepository.findByWalletAndReferenceId(wallet, withdrawalId)
            .ifPresent(transaction -> {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(transaction);
            });

        // Notificar o provider sobre a aprovação
        notifyProviderAboutApproval(request);

        log.info("Withdrawal approved and completed: id={}, approvedBy={}, amount={}", 
            withdrawalId, adminId, request.getRequestedAmount());
    }

    @Transactional
    public void rejectWithdrawal(UUID withdrawalId, UUID adminId, String reason) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(withdrawalId)
            .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found"));

        if (request.getStatus() != WithdrawalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Withdrawal is not pending approval");
        }

        request.setStatus(WithdrawalStatus.REJECTED);
        request.setApprovedBy(adminId);
        request.setRejectionReason(reason);
        withdrawalRequestRepository.save(request);

        // Devolver saldo reservado para disponível
        ProviderWallet wallet = request.getWallet();
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(request.getRequestedAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getRequestedAmount()));
        walletService.updateBalance(wallet, BigDecimal.ZERO, "available");

        // Atualizar transação para CANCELLED
        transactionRepository.findByWalletAndReferenceId(wallet, withdrawalId)
            .ifPresent(transaction -> {
                transaction.setStatus(TransactionStatus.CANCELLED);
                transactionRepository.save(transaction);
            });

        // Notificar o provider sobre a rejeição
        notifyProviderAboutRejection(request);

        log.info("Withdrawal rejected: id={}, rejectedBy={}, reason={}", withdrawalId, adminId, reason);
    }

    @Transactional(readOnly = true)
    public long countPendingWithdrawals() {
        return withdrawalRequestRepository.countByStatus(WithdrawalStatus.PENDING_APPROVAL);
    }

    private void notifyAdminsAboutWithdrawal(WithdrawalRequest request) {
        try {
            // Buscar todos os administradores (ADMIN e SUPER_ADMIN)
            List<User> admins = new java.util.ArrayList<>();
            admins.addAll(userRepository.findByRolesContaining("ADMIN"));
            admins.addAll(userRepository.findByRolesContaining("SUPER_ADMIN"));
            
            // Remover duplicados
            admins = admins.stream().distinct().collect(java.util.stream.Collectors.toList());
            
            if (admins.isEmpty()) {
                log.warn("No admins found to notify about withdrawal request {}", request.getId());
                return;
            }
            
            // Buscar informações do provider
            User provider = userRepository.findById(request.getWallet().getProviderId())
                .orElse(null);
            
            String providerName = provider != null ? provider.getFirstName() + " " + provider.getLastName() : "Unknown";
            String providerEmail = provider != null ? provider.getEmail() : "unknown@email.com";
            
            String title = "Nova Solicitação de Levantamento";
            String message = String.format(
                "Nova solicitação de levantamento de €%.2f aguardando aprovação. Provider: %s (%s). Método: %s.",
                request.getRequestedAmount(),
                providerName,
                providerEmail,
                request.getMethod()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("withdrawalId", request.getId().toString());
            data.put("amount", "€" + request.getRequestedAmount().toString());
            data.put("method", request.getMethod().toString());
            data.put("providerId", request.getWallet().getProviderId().toString());
            data.put("providerName", providerName);
            data.put("providerEmail", providerEmail);
            
            String actionUrl = "/admin/billing/withdrawals";
            
            // Notificar cada administrador
            for (User admin : admins) {
                log.info("Notifying admin {} ({}) about withdrawal {}", 
                    admin.getUsername(), admin.getEmail(), request.getId());
                    
                notificationService.sendNotification(
                    admin.getId().toString(),
                    admin.getEmail(),
                    NotificationType.WITHDRAWAL_REQUESTED,
                    title,
                    message,
                    data,
                    actionUrl
                );
            }
            
            log.info("Notified {} admins about withdrawal request {}", admins.size(), request.getId());
        } catch (Exception e) {
            log.error("Error notifying admins about withdrawal: {}", e.getMessage(), e);
            // Não falhar a transação se a notificação falhar
        }
    }

    private void notifyProviderAboutApproval(WithdrawalRequest request) {
        try {
            User provider = userRepository.findById(request.getWallet().getProviderId())
                .orElse(null);
            
            if (provider == null) {
                log.warn("Provider not found for withdrawal {}", request.getId());
                return;
            }
            
            String title = "Levantamento Aprovado";
            String message = String.format(
                "Seu levantamento de €%.2f foi aprovado! O valor líquido de €%.2f será processado via %s.",
                request.getRequestedAmount(),
                request.getNetAmount(),
                request.getMethod()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("withdrawalId", request.getId().toString());
            data.put("amount", "€" + request.getRequestedAmount().toString());
            data.put("netAmount", "€" + request.getNetAmount().toString());
            data.put("feeAmount", "€" + request.getFeeAmount().toString());
            data.put("method", request.getMethod().toString());
            data.put("approvedAt", request.getProcessedAt().toString());
            data.put("providerName", provider.getFirstName() + " " + provider.getLastName());
            
            String actionUrl = "/provider/wallet";
            
            log.info("Notifying provider {} about withdrawal approval {}", 
                provider.getUsername(), request.getId());
                
            notificationService.sendNotification(
                provider.getId().toString(),
                provider.getEmail(),
                NotificationType.WITHDRAWAL_APPROVED,
                title,
                message,
                data,
                actionUrl
            );
            
            log.info("Provider notified about withdrawal approval {}", request.getId());
        } catch (Exception e) {
            log.error("Error notifying provider about withdrawal approval: {}", e.getMessage(), e);
        }
    }

    private void notifyProviderAboutRejection(WithdrawalRequest request) {
        try {
            User provider = userRepository.findById(request.getWallet().getProviderId())
                .orElse(null);
            
            if (provider == null) {
                log.warn("Provider not found for withdrawal {}", request.getId());
                return;
            }
            
            String title = "Levantamento Rejeitado";
            String message = String.format(
                "Seu levantamento de €%.2f foi rejeitado. Motivo: %s. O valor foi devolvido para sua carteira.",
                request.getRequestedAmount(),
                request.getRejectionReason()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("withdrawalId", request.getId().toString());
            data.put("amount", "€" + request.getRequestedAmount().toString());
            data.put("reason", request.getRejectionReason());
            data.put("method", request.getMethod().toString());
            data.put("providerName", provider.getFirstName() + " " + provider.getLastName());
            
            String actionUrl = "/provider/wallet";
            
            log.info("Notifying provider {} about withdrawal rejection {}", 
                provider.getUsername(), request.getId());
                
            notificationService.sendNotification(
                provider.getId().toString(),
                provider.getEmail(),
                NotificationType.WITHDRAWAL_REJECTED,
                title,
                message,
                data,
                actionUrl
            );
            
            log.info("Provider notified about withdrawal rejection {}", request.getId());
        } catch (Exception e) {
            log.error("Error notifying provider about withdrawal rejection: {}", e.getMessage(), e);
        }
    }
}
