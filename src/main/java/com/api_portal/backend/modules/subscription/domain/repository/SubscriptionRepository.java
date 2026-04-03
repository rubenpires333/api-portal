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
    
    // Analytics queries
    @Query("SELECT s.api.id, SUM(s.requestsUsed) FROM Subscription s WHERE s.api.providerId = :providerId AND s.status = 'ACTIVE' GROUP BY s.api.id")
    List<Object[]> findProviderSubscriptionStats(@Param("providerId") String providerId);
    
    @Query("SELECT s.consumerEmail, s.consumerName, SUM(s.requestsUsed) FROM Subscription s WHERE s.api.providerId = :providerId AND s.status = 'ACTIVE' GROUP BY s.consumerEmail, s.consumerName ORDER BY SUM(s.requestsUsed) DESC")
    List<Object[]> findTopConsumersByProvider(@Param("providerId") String providerId);
    
    // Métodos para dashboard
    long countByStatus(SubscriptionStatus status);
    
    long countByCreatedAtBefore(java.time.LocalDateTime date);
    
    List<Subscription> findTop5ByOrderByCreatedAtDesc();
    
    List<Subscription> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(java.time.LocalDateTime date);
    
    List<Subscription> findByStatusOrderByCreatedAtAsc(SubscriptionStatus status);
    
    // Métodos para rankings e alertas
    long countByApiId(UUID apiId);
    
    List<Subscription> findByStatusAndCreatedAtBefore(SubscriptionStatus status, java.time.LocalDateTime date);
}
