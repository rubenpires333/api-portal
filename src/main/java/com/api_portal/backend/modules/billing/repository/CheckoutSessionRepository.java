package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.CheckoutSession;
import com.api_portal.backend.modules.billing.model.enums.CheckoutSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
    
    Optional<CheckoutSession> findByStripeSessionId(String stripeSessionId);
    
    Optional<CheckoutSession> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    List<CheckoutSession> findByStatusAndCreatedAtBefore(CheckoutSessionStatus status, LocalDateTime createdBefore);
    
    List<CheckoutSession> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
