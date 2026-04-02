package com.api_portal.backend.modules.settings.service;

import com.api_portal.backend.modules.settings.domain.TermsAndPolicies;
import com.api_portal.backend.modules.settings.dto.TermsAndPoliciesDTO;
import com.api_portal.backend.modules.settings.dto.UpdateTermsAndPoliciesRequest;
import com.api_portal.backend.modules.settings.repository.TermsAndPoliciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermsAndPoliciesService {

    private final TermsAndPoliciesRepository repository;

    @Transactional(readOnly = true)
    public TermsAndPoliciesDTO getActiveTermsAndPolicies() {
        return repository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .map(this::toDTO)
                .orElse(createDefaultTermsAndPolicies());
    }

    @Transactional
    public TermsAndPoliciesDTO updateTermsAndPolicies(UpdateTermsAndPoliciesRequest request, String userId) {
        // Desativar versões anteriores
        repository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .ifPresent(existing -> {
                    existing.setIsActive(false);
                    repository.save(existing);
                });

        // Criar nova versão
        TermsAndPolicies newTerms = new TermsAndPolicies();
        newTerms.setTermsOfService(request.getTermsOfService());
        newTerms.setPrivacyPolicy(request.getPrivacyPolicy());
        newTerms.setVersion(request.getVersion() != null ? request.getVersion() : generateVersion());
        newTerms.setIsActive(true);
        newTerms.setUpdatedBy(userId);

        TermsAndPolicies saved = repository.save(newTerms);
        return toDTO(saved);
    }

    private TermsAndPoliciesDTO toDTO(TermsAndPolicies entity) {
        TermsAndPoliciesDTO dto = new TermsAndPoliciesDTO();
        dto.setId(entity.getId());
        dto.setTermsOfService(entity.getTermsOfService());
        dto.setPrivacyPolicy(entity.getPrivacyPolicy());
        dto.setVersion(entity.getVersion());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }

    private TermsAndPoliciesDTO createDefaultTermsAndPolicies() {
        TermsAndPoliciesDTO dto = new TermsAndPoliciesDTO();
        dto.setTermsOfService("<h3>Termos de Serviço</h3><p>Configure os termos de serviço da plataforma.</p>");
        dto.setPrivacyPolicy("<h3>Política de Privacidade</h3><p>Configure a política de privacidade da plataforma.</p>");
        dto.setVersion("1.0.0");
        dto.setIsActive(true);
        return dto;
    }

    private String generateVersion() {
        long count = repository.count();
        return "1.0." + (count + 1);
    }
}
