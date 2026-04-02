package com.api_portal.backend.modules.settings.repository;

import com.api_portal.backend.modules.settings.domain.TermsAndPolicies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TermsAndPoliciesRepository extends JpaRepository<TermsAndPolicies, Long> {
    Optional<TermsAndPolicies> findFirstByIsActiveTrueOrderByCreatedAtDesc();
}
