package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.model.WithdrawalRequest;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalStatus;
import com.api_portal.backend.modules.billing.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job agendado que processa levantamentos aprovados automaticamente
 * Executa a cada 5 minutos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalProcessingService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletService walletService;

    @Scheduled(cron = "0 */5 * * * *") // A cada 5 minutos
    @Transactional
    public void processApprovedWithdrawals() {
        log.info("Starting withdrawal processing job");

        List<WithdrawalRequest> approvedRequests = withdrawalRequestRepository
            .findByStatus(WithdrawalStatus.APPROVED);

        log.info("Found {} approved withdrawals to process", approvedRequests.size());

        for (WithdrawalRequest request : approvedRequests) {
            try {
                processWithdrawal(request);
            } catch (Exception e) {
                log.error("Error processing withdrawal: id={}", request.getId(), e);
            }
        }

        log.info("Withdrawal processing job completed");
    }

    @Transactional
    public void processWithdrawal(WithdrawalRequest request) {
        log.info("Processing withdrawal: id={}, method={}, amount={}", 
            request.getId(), request.getMethod(), request.getNetAmount());

        // Atualizar status para PROCESSING
        request.setStatus(WithdrawalStatus.PROCESSING);
        withdrawalRequestRepository.save(request);

        try {
            // Aqui seria a integração com o gateway de pagamento
            // Por enquanto, simula processamento bem-sucedido
            boolean paymentSuccess = processPayment(request);

            if (paymentSuccess) {
                // Marcar como concluído
                request.setStatus(WithdrawalStatus.COMPLETED);
                request.setProcessedAt(LocalDateTime.now());
                withdrawalRequestRepository.save(request);

                // Debitar do saldo reservado
                var wallet = request.getWallet();
                wallet.setReservedBalance(wallet.getReservedBalance().subtract(request.getRequestedAmount()));
                walletService.updateBalance(wallet, request.getRequestedAmount().negate(), "debited");

                log.info("Withdrawal completed successfully: id={}", request.getId());
                
                // TODO: Enviar notificação ao provider
            } else {
                // Falha no pagamento - reverter para aprovado para tentar novamente
                request.setStatus(WithdrawalStatus.APPROVED);
                withdrawalRequestRepository.save(request);
                
                log.warn("Payment failed for withdrawal: id={}", request.getId());
            }
        } catch (Exception e) {
            // Em caso de erro, reverter para aprovado
            request.setStatus(WithdrawalStatus.APPROVED);
            withdrawalRequestRepository.save(request);
            
            log.error("Error processing payment for withdrawal: id={}", request.getId(), e);
            throw e;
        }
    }

    /**
     * Processa o pagamento através do gateway apropriado
     * TODO: Implementar integração real com gateways (Stripe, PayPal, etc.)
     */
    private boolean processPayment(WithdrawalRequest request) {
        log.info("Processing payment via {}: amount={}", 
            request.getMethod(), request.getNetAmount());

        // Simulação de processamento
        // Em produção, aqui seria chamada a API do gateway
        switch (request.getMethod()) {
            case VINTI4:
                return processVinti4Payment(request);
            case BANK_TRANSFER:
                return processBankTransfer(request);
            case PAYPAL:
                return processPayPalPayment(request);
            case WISE:
                return processWisePayment(request);
            case PLATFORM_CREDIT:
                return processPlatformCredit(request);
            default:
                log.error("Unsupported withdrawal method: {}", request.getMethod());
                return false;
        }
    }

    private boolean processVinti4Payment(WithdrawalRequest request) {
        // TODO: Integrar com API Vinti4
        log.info("Processing Vinti4 payment: {}", request.getDestinationDetails());
        return true; // Simulação
    }

    private boolean processBankTransfer(WithdrawalRequest request) {
        // TODO: Integrar com sistema bancário
        log.info("Processing bank transfer: {}", request.getDestinationDetails());
        return true; // Simulação
    } 

    private boolean processPayPalPayment(WithdrawalRequest request) {
        // TODO: Integrar com PayPal Payouts API
        log.info("Processing PayPal payment: {}", request.getDestinationDetails());
        return true; // Simulação
    }

    private boolean processWisePayment(WithdrawalRequest request) {
        // TODO: Integrar com Wise API
        log.info("Processing Wise payment: {}", request.getDestinationDetails());
        return true; // Simulação
    }

    private boolean processPlatformCredit(WithdrawalRequest request) {
        // Crédito na plataforma é instantâneo
        log.info("Processing platform credit: {}", request.getDestinationDetails());
        return true;
    }
}
