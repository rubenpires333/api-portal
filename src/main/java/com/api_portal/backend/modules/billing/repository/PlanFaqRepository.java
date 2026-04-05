package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.PlanFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanFaqRepository extends JpaRepository<PlanFaq, UUID> {
    
    List<PlanFaq> findByActiveTrueOrderByDisplayOrderAsc();
    
    List<PlanFaq> findAllByOrderByDisplayOrderAsc();
}
