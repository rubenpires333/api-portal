package com.api_portal.backend.modules.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDTO {
    private UUID id;
    private String invoiceId;
    private String invoiceNumber; // Número legível da invoice
    private String planName;
    private BigDecimal amount;
    private String currency;
    private String status; // PAID, FAILED, PENDING
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String receiptUrl;
    private String invoicePdf;
    private String failureReason;
    
    // Detalhes do método de pagamento
    private String paymentMethodType; // card, sepa_debit, etc
    private String cardBrand; // visa, mastercard, amex
    private String cardLast4; // Últimos 4 dígitos
}
