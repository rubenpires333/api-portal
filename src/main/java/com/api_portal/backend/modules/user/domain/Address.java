package com.api_portal.backend.modules.user.domain;

import com.api_portal.backend.shared.domain.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends Auditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column
    private String street;
    
    @Column(name = "number")
    private String number;
    
    @Column
    private String complement;
    
    @Column
    private String neighborhood;
    
    @Column
    private String city;
    
    @Column(length = 50)
    private String state;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Column
    @Builder.Default
    private String country = "Cabo Verde";
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
}
