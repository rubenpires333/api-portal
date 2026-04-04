package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.PlatformPlanDTO;
import com.api_portal.backend.modules.billing.model.PlatformPlan;
import com.api_portal.backend.modules.billing.repository.PlatformPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformPlanService {

    private final PlatformPlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<PlatformPlan> getAllPlans() {
        return planRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<PlatformPlan> getActivePlans() {
        return planRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public PlatformPlan getPlanById(UUID id) {
        return planRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
    }

    @Transactional(readOnly = true)
    public PlatformPlan getPlanByName(String name) {
        return planRepository.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + name));
    }

    @Transactional
    public PlatformPlan createPlan(PlatformPlanDTO dto) {
        log.info("Creating platform plan: {}", dto.getName());

        // Verificar se já existe
        if (planRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Plan already exists: " + dto.getName());
        }

        // Se displayOrder não foi fornecido, usar o próximo disponível
        Integer displayOrder = dto.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = planRepository.findAll().size() + 1;
        }

        PlatformPlan plan = PlatformPlan.builder()
            .name(dto.getName())
            .displayName(dto.getDisplayName())
            .description(dto.getDescription())
            .monthlyPrice(dto.getMonthlyPrice())
            .currency(dto.getCurrency())
            .maxApis(dto.getMaxApis())
            .maxRequestsPerMonth(dto.getMaxRequestsPerMonth())
            .maxTeamMembers(dto.getMaxTeamMembers())
            .customDomain(dto.isCustomDomain())
            .prioritySupport(dto.isPrioritySupport())
            .advancedAnalytics(dto.isAdvancedAnalytics())
            .stripePriceId(dto.getStripePriceId())
            .vinti4PriceId(dto.getVinti4PriceId())
            .active(dto.isActive())
            .displayOrder(displayOrder)
            .build();

        return planRepository.save(plan);
    }

    @Transactional
    public PlatformPlan updatePlan(UUID id, PlatformPlanDTO dto) {
        log.info("Updating platform plan: {}", id);

        PlatformPlan plan = getPlanById(id);

        plan.setDisplayName(dto.getDisplayName());
        plan.setDescription(dto.getDescription());
        plan.setMonthlyPrice(dto.getMonthlyPrice());
        plan.setCurrency(dto.getCurrency());
        plan.setMaxApis(dto.getMaxApis());
        plan.setMaxRequestsPerMonth(dto.getMaxRequestsPerMonth());
        plan.setMaxTeamMembers(dto.getMaxTeamMembers());
        plan.setCustomDomain(dto.isCustomDomain());
        plan.setPrioritySupport(dto.isPrioritySupport());
        plan.setAdvancedAnalytics(dto.isAdvancedAnalytics());
        plan.setStripePriceId(dto.getStripePriceId());
        plan.setVinti4PriceId(dto.getVinti4PriceId());
        plan.setActive(dto.isActive());
        
        if (dto.getDisplayOrder() != null) {
            plan.setDisplayOrder(dto.getDisplayOrder());
        }

        return planRepository.save(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        log.info("Deleting platform plan: {}", id);

        PlatformPlan plan = getPlanById(id);

        // TODO: Verificar se há assinaturas ativas antes de deletar
        
        planRepository.delete(plan);
        log.info("Plan {} deleted successfully", id);
    }

    @Transactional
    public void togglePlanStatus(UUID id) {
        log.info("Toggling plan status: {}", id);

        PlatformPlan plan = getPlanById(id);
        plan.setActive(!plan.isActive());
        planRepository.save(plan);

        log.info("Plan {} status toggled to: {}", id, plan.isActive());
    }
}
