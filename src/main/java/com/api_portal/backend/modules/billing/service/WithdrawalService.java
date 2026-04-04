package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.WithdrawalRequestDTO;
import com.api_portal.backend.modules.billing.model.ProviderWallet;
import com.api_portal.backend.modules.billing.model.WithdrawalFeeRule;
import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
import com.api_portal.backend.modules.billing.repository.WithdrawalFeeRuleRepository;
import com.api_portal.backend.modules.billing.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WalletService walletService;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WithdrawalFeeRuleRepository feeRuleRepository;

    @Value("${billing.auto-approve-threshold:50.00}")
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

        // Reservar saldo
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(dto.getAmount()));
        wallet.setReservedBalance(wallet.getReservedBalance().add(dto.getAmount()));
        walletService.updateBalance(wallet, BigDecimal.ZERO, "reserved");

        log.info("Withdrawal request created: id={}, amount={}, status={}", 
            request.getId(), dto.getAmount(), status);

        return request;
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalRequest> getPendingWithdrawals(Pageable pageable) {
        return withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(
            WithdrawalStatus.PENDING_APPROVAL, pageable);
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
        withdrawalRequestRepository.save(request);

        log.info("Withdrawal approved: id={}, approvedBy={}", withdrawalId, adminId);
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

        log.info("Withdrawal rejected: id={}, rejectedBy={}, reason={}", withdrawalId, adminId, reason);
    }
}
