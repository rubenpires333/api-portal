package com.api_portal.backend.modules.api.domain;

import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
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
@Table(name = "api_versions")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiVersion extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private Api api;
    
    @Column(nullable = false)
    private String version;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApiStatus status = ApiStatus.DRAFT;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeprecated = false;
    
    private LocalDateTime deprecatedAt;
    
    private String deprecationMessage;
    
    private String baseUrl;
    
    @Column(columnDefinition = "TEXT")
    private String openApiSpec;
    
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApiEndpoint> endpoints = new ArrayList<>();
}
