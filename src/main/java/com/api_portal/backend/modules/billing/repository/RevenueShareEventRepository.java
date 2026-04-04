package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.RevenueShareEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RevenueShareEventRepository extends JpaRepository<RevenueShareEvent, UUID> {
}
