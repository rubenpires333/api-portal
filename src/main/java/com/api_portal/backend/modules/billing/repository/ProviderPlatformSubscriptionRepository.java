package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.ProviderPlatformSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderPlatformSubscriptionRepository extends JpaRepository<ProviderPlatformSubscription, UUID> {
    
    Optional<ProviderPlatformSubscription> findByProviderId(UUID providerId);
    
    Optional<ProviderPlatformSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    boolean existsByProviderId(UUID providerId);
    
    // Métodos para Platform Wallet
    Long countByStatus(String status);
    
    @Query("SELECT COALESCE(SUM(s.plan.monthlyPrice), 0) FROM ProviderPlatformSubscription s WHERE s.status = :status")
    Optional<BigDecimal> sumAmountByStatus(String status);
    
    @Query("SELECT COUNT(DISTINCT s.providerId) FROM ProviderPlatformSubscription s WHERE s.status = :status")
    Long countDistinctProvidersByStatus(String status);
    
    Page<ProviderPlatformSubscription> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    Page<ProviderPlatformSubscription> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
