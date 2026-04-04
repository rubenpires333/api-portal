package com.api_portal.backend.modules.billing.model;

import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "gateway_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private GatewayType gatewayType;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String displayName;

    private String logoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> settings; // api_key, webhook_secret, etc.

    private String supportedCurrencies; // 'USD,EUR' ou 'CVE'

    @Column(nullable = false)
    private boolean supportsSubscriptions;

    @Column(nullable = false)
    private boolean supportsRefunds;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
