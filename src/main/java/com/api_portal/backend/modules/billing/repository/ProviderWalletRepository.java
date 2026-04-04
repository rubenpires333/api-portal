package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.ProviderWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderWalletRepository extends JpaRepository<ProviderWallet, UUID> {
    
    Optional<ProviderWallet> findByProviderId(UUID providerId);
}
