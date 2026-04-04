package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.PlatformPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformPlanRepository extends JpaRepository<PlatformPlan, UUID> {
    
    Optional<PlatformPlan> findByName(String name);
    
    List<PlatformPlan> findByActiveTrueOrderByDisplayOrderAsc();
    
    List<PlatformPlan> findAllByOrderByDisplayOrderAsc();
    
    boolean existsByName(String name);
}
