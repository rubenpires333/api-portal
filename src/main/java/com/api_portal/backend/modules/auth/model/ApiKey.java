package com.api_portal.backend.modules.auth.model;

import com.api_portal.backend.shared.domain.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 64)
    private String keyValue;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 255)
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime lastUsedAt;
}
