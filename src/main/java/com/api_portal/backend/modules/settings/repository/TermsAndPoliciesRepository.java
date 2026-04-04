package com.api_portal.backend.modules.settings.repository;

import com.api_portal.backend.modules.settings.domain.TermsAndPolicies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermsAndPoliciesRepository extends JpaRepository<TermsAndPolicies, UUID> {
    Optional<TermsAndPolicies> findFirstByIsActiveTrueOrderByCreatedAtDesc();
}
