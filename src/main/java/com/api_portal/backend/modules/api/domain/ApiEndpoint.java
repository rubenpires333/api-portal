package com.api_portal.backend.modules.api.domain;

import com.api_portal.backend.shared.domain.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "api_endpoints")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpoint extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private ApiVersion version;
    
    @Column(nullable = false)
    private String path;
    
    @Column(nullable = false)
    private String method;
    
    @Column(nullable = false)
    private String summary;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ElementCollection
    @CollectionTable(name = "endpoint_tags", joinColumns = @JoinColumn(name = "endpoint_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresAuth = true;
    
    @Column(columnDefinition = "TEXT")
    private String authHeadersJson;
    
    @Column(columnDefinition = "TEXT")
    private String authQueryParamsJson;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeprecated = false;
    
    @Column(columnDefinition = "TEXT")
    private String requestExample;
    
    @Column(columnDefinition = "TEXT")
    private String responseExample;
    
    private Integer responseTime;
    
    private Double successRate;
}
