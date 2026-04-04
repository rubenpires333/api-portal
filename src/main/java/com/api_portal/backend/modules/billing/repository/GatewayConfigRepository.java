package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.GatewayConfig;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GatewayConfigRepository extends JpaRepository<GatewayConfig, UUID> {
    
    Optional<GatewayConfig> findByGatewayType(GatewayType gatewayType);
    
    Optional<GatewayConfig> findByActiveTrue();
}
