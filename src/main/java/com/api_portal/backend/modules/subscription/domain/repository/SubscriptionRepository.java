package com.api_portal.backend.modules.subscription.domain.repository;

import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    
    // Consumer queries
    Page<Subscription> findByConsumerId(String consumerId, Pageable pageable);
    
    List<Subscription> findByConsumerId(String consumerId);
    
    Optional<Subscription> findByConsumerIdAndApiId(String consumerId, UUID apiId);
    
    Optional<Subscription> findByConsumerIdAndApiIdAndStatus(String consumerId, UUID apiId, SubscriptionStatus status);
    
    boolean existsByConsumerIdAndApiIdAndStatus(String consumerId, UUID apiId, SubscriptionStatus status);
    
    // Provider queries
    @Query("SELECT s FROM Subscription s WHERE s.api.providerId = :providerId")
    Page<Subscription> findByProviderId(@Param("providerId") String providerId, Pageable pageable);
    
    @Query("SELECT s FROM Subscription s WHERE s.api.id = :apiId")
    Page<Subscription> findByApiId(@Param("apiId") UUID apiId, Pageable pageable);
    
    @Query("SELECT s FROM Subscription s WHERE s.api.providerId = :providerId AND s.status = :status")
    Page<Subscription> findByProviderIdAndStatus(
        @Param("providerId") String providerId, 
        @Param("status") SubscriptionStatus status, 
        Pageable pageable
    );
    
    // API Key validation
    Optional<Subscription> findByApiKey(String apiKey);
    
    @Query("SELECT s FROM Subscription s WHERE s.apiKey = :apiKey AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveByApiKey(@Param("apiKey") String apiKey);
    
    // Status queries
    List<Subscription> findByStatus(SubscriptionStatus status);
    
    // Statistics
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.api.providerId = :providerId AND s.status = 'ACTIVE'")
    long countActiveByProviderId(@Param("providerId") String providerId);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.api.providerId = :providerId AND s.status = :status")
    long countByProviderIdAndStatus(@Param("providerId") String providerId, @Param("status") SubscriptionStatus status);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.api.id = :apiId AND s.status = 'ACTIVE'")
    long countActiveByApiId(@Param("apiId") UUID apiId);
}
