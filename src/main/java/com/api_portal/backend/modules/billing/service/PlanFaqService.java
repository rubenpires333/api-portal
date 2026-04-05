package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.PlanFaqDTO;
import com.api_portal.backend.modules.billing.model.PlanFaq;
import com.api_portal.backend.modules.billing.repository.PlanFaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanFaqService {
    
    private final PlanFaqRepository faqRepository;
    
    /**
     * Buscar todas as FAQs (para admin)
     */
    @Transactional(readOnly = true)
    public List<PlanFaqDTO> getAllFaqs() {
        log.info("Buscando todas as FAQs");
        return faqRepository.findAllByOrderByDisplayOrderAsc()
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Buscar apenas FAQs ativas (para provider)
     */
    @Transactional(readOnly = true)
    public List<PlanFaqDTO> getActiveFaqs() {
        log.info("Buscando FAQs ativas");
        return faqRepository.findByActiveTrueOrderByDisplayOrderAsc()
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Buscar FAQ por ID
     */
    @Transactional(readOnly = true)
    public PlanFaqDTO getFaqById(UUID id) {
        log.info("Buscando FAQ por ID: {}", id);
        PlanFaq faq = faqRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("FAQ não encontrada"));
        return mapToDTO(faq);
    }
    
    /**
     * Criar nova FAQ
     */
    @Transactional
    public PlanFaqDTO createFaq(PlanFaqDTO dto) {
        log.info("Criando nova FAQ: {}", dto.getQuestion());
        
        PlanFaq faq = PlanFaq.builder()
            .question(dto.getQuestion())
            .answer(dto.getAnswer())
            .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
            .active(dto.getActive() != null ? dto.getActive() : true)
            .build();
        
        faq = faqRepository.save(faq);
        log.info("FAQ criada com sucesso: {}", faq.getId());
        
        return mapToDTO(faq);
    }
    
    /**
     * Atualizar FAQ existente
     */
    @Transactional
    public PlanFaqDTO updateFaq(UUID id, PlanFaqDTO dto) {
        log.info("Atualizando FAQ: {}", id);
        
        PlanFaq faq = faqRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("FAQ não encontrada"));
        
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setDisplayOrder(dto.getDisplayOrder());
        faq.setActive(dto.getActive());
        
        faq = faqRepository.save(faq);
        log.info("FAQ atualizada com sucesso: {}", faq.getId());
        
        return mapToDTO(faq);
    }
    
    /**
     * Deletar FAQ
     */
    @Transactional
    public void deleteFaq(UUID id) {
        log.info("Deletando FAQ: {}", id);
        
        if (!faqRepository.existsById(id)) {
            throw new RuntimeException("FAQ não encontrada");
        }
        
        faqRepository.deleteById(id);
        log.info("FAQ deletada com sucesso: {}", id);
    }
    
    /**
     * Alternar status ativo/inativo
     */
    @Transactional
    public PlanFaqDTO toggleFaqStatus(UUID id) {
        log.info("Alternando status da FAQ: {}", id);
        
        PlanFaq faq = faqRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("FAQ não encontrada"));
        
        faq.setActive(!faq.getActive());
        faq = faqRepository.save(faq);
        
        log.info("Status da FAQ alterado para: {}", faq.getActive());
        return mapToDTO(faq);
    }
    
    private PlanFaqDTO mapToDTO(PlanFaq faq) {
        return PlanFaqDTO.builder()
            .id(faq.getId())
            .question(faq.getQuestion())
            .answer(faq.getAnswer())
            .displayOrder(faq.getDisplayOrder())
            .active(faq.getActive())
            .build();
    }
}
