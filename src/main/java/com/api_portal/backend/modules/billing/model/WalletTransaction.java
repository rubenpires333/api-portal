package com.api_portal.backend.modules.billing.model;

import com.api_portal.backend.modules.billing.model.enums.TransactionStatus;
import com.api_portal.backend.modules.billing.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private ProviderWallet wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    private UUID referenceId; // invoiceId, withdrawalId, subscriptionId

    @Column(columnDefinition = "TEXT")
    private String description;
    
    // ===== NOVOS CAMPOS PARA DETALHES DO PAGAMENTO =====
    
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId; // ID do Payment Intent do Stripe
    
    @Column(name = "stripe_invoice_id")
    private String stripeInvoiceId; // Número da Invoice do Stripe
    
    @Column(name = "stripe_invoice_number")
    private String stripeInvoiceNumber; // Número legível da invoice (ex: INV-1234)
    
    @Column(name = "payment_method_type")
    private String paymentMethodType; // card, sepa_debit, etc
    
    @Column(name = "card_brand")
    private String cardBrand; // visa, mastercard, amex, etc
    
    @Column(name = "card_last4")
    private String cardLast4; // Últimos 4 dígitos do cartão
    
    @Column(name = "receipt_url")
    private String receiptUrl; // URL do recibo do Stripe
    
    @Column(name = "invoice_pdf_url")
    private String invoicePdfUrl; // URL do PDF da invoice

    @Column(nullable = false)
    private LocalDateTime availableAt; // createdAt + holdback period

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
