package com.api_portal.backend.modules.api.domain;

import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.domain.enums.ApiVisibility;
import com.api_portal.backend.modules.api.domain.enums.AuthType;
import com.api_portal.backend.shared.domain.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "apis")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Api extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 500)
    private String shortDescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ApiCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApiStatus status = ApiStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApiVisibility visibility = ApiVisibility.PUBLIC;
    
    @Column(nullable = false)
    private String providerId;
    
    @Column(nullable = false)
    private String providerName;
    
    @Column(nullable = false)
    private String providerEmail;
    
    @Column(nullable = false)
    private String baseUrl;
    
    private String documentationUrl;
    
    private String termsOfServiceUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthType authType = AuthType.API_KEY;
    
    private String logoUrl;
    
    private String iconUrl;
    
    @ElementCollection
    @CollectionTable(name = "api_tags", joinColumns = @JoinColumn(name = "api_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    private Integer rateLimit;
    
    private String rateLimitPeriod;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "api", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<ApiVersion> versions = new ArrayList<>();
    
    private LocalDateTime publishedAt;
    
    // Campos de aprovação
    private LocalDateTime requestedApprovalAt;
    
    private LocalDateTime approvedAt;
    
    private String approvedBy; // keycloakId do admin que aprovou
    
    private LocalDateTime rejectedAt;
    
    private String rejectedBy; // keycloakId do admin que rejeitou
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
}
