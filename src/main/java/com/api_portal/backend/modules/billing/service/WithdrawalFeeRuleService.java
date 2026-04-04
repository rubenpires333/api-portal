package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.WithdrawalFeeRuleDTO;
import com.api_portal.backend.modules.billing.model.WithdrawalFeeRule;
import com.api_portal.backend.modules.billing.model.enums.WithdrawalMethod;
import com.api_portal.backend.modules.billing.repository.WithdrawalFeeRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalFeeRuleService {

    private final WithdrawalFeeRuleRepository feeRuleRepository;

    @Transactional(readOnly = true)
    public List<WithdrawalFeeRule> getAllFeeRules() {
        return feeRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public WithdrawalFeeRule getFeeRuleByMethod(WithdrawalMethod method) {
        return feeRuleRepository.findByWithdrawalMethodAndActiveTrue(method)
            .orElseThrow(() -> new IllegalArgumentException("Fee rule not found for method: " + method));
    }

    @Transactional
    public WithdrawalFeeRule createFeeRule(WithdrawalFeeRuleDTO dto, UUID adminId) {
        log.info("Creating fee rule for method: {}", dto.getWithdrawalMethod());

        // Verificar se já existe
        if (feeRuleRepository.findByWithdrawalMethodAndActiveTrue(dto.getWithdrawalMethod()).isPresent()) {
            throw new IllegalArgumentException("Fee rule already exists for method: " + dto.getWithdrawalMethod());
        }

        WithdrawalFeeRule rule = WithdrawalFeeRule.builder()
            .withdrawalMethod(dto.getWithdrawalMethod())
            .feePercentage(dto.getFeePercentage())
            .fixedFee(dto.getFixedFee())
            .fixedFeeCurrency(dto.getFixedFeeCurrency())
            .minimumAmount(dto.getMinimumAmount())
            .maximumAmount(dto.getMaximumAmount())
            .active(dto.isActive())
            .updatedBy(adminId)
            .build();

        return feeRuleRepository.save(rule);
    }

    @Transactional
    public WithdrawalFeeRule updateFeeRule(UUID id, WithdrawalFeeRuleDTO dto, UUID adminId) {
        log.info("Updating fee rule: {}", id);

        WithdrawalFeeRule rule = feeRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fee rule not found: " + id));

        rule.setFeePercentage(dto.getFeePercentage());
        rule.setFixedFee(dto.getFixedFee());
        rule.setFixedFeeCurrency(dto.getFixedFeeCurrency());
        rule.setMinimumAmount(dto.getMinimumAmount());
        rule.setMaximumAmount(dto.getMaximumAmount());
        rule.setActive(dto.isActive());
        rule.setUpdatedBy(adminId);

        return feeRuleRepository.save(rule);
    }

    @Transactional
    public void toggleFeeRuleStatus(UUID id) {
        WithdrawalFeeRule rule = feeRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fee rule not found: " + id));
        
        rule.setActive(!rule.isActive());
        feeRuleRepository.save(rule);
        
        log.info("Fee rule {} status changed to: {}", rule.getWithdrawalMethod(), rule.isActive());
    }
}
