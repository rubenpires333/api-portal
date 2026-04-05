package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderPlatformSubscriptionRepository extends JpaRepository<ProviderPlatformSubscription, UUID> {
    
    Optional<ProviderPlatformSubscription> findByProviderId(UUID providerId);
    
    Optional<ProviderPlatformSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    boolean existsByProviderId(UUID providerId);
}
