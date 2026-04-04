package com.api_portal.backend.modules.billing.repository;

import com.api_portal.backend.modules.billing.model.WithdrawalFeeRule;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalFeeRuleRepository extends JpaRepository<WithdrawalFeeRule, UUID> {
    
    Optional<WithdrawalFeeRule> findByWithdrawalMethodAndActiveTrue(WithdrawalMethod method);
}
