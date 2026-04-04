package com.api_portal.backend.modules.billing.model;

import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_webhooks", indexes = {
    @Index(name = "idx_event_id", columnList = "eventId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String eventId; // ID do evento do gateway (para idempotência)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GatewayType gatewayType;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
