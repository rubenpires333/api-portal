package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.PaymentWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, UUID> {
    
    boolean existsByEventId(String eventId);
}
