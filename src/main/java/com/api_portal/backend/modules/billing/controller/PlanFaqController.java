package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.PlanFaqDTO;
import com.api_portal.backend.modules.billing.service.PlanFaqService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/faqs")
@RequiredArgsConstructor
@Tag(name = "Plan FAQs", description = "Gerenciamento de FAQs de Planos")
public class PlanFaqController {
    
    private final PlanFaqService faqService;
    
    /**
     * Listar FAQs ativas (público para providers)
     */
    @GetMapping("/active")
    @Operation(summary = "Listar FAQs ativas")
    public ResponseEntity<List<PlanFaqDTO>> getActiveFaqs() {
        log.info("Requisição para listar FAQs ativas");
        List<PlanFaqDTO> faqs = faqService.getActiveFaqs();
        return ResponseEntity.ok(faqs);
    }
}

/**
 * Endpoints administrativos de FAQs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing/faqs")
@RequiredArgsConstructor
@RequiresPermission("billing.manage")
@Tag(name = "Plan FAQs Admin", description = "Gerenciamento Administrativo de FAQs")
class AdminPlanFaqController {
    
    private final PlanFaqService faqService;
    
    /**
     * Listar todas as FAQs (admin)
     */
    @GetMapping
    @Operation(summary = "Listar todas as FAQs (Admin)")
    public ResponseEntity<List<PlanFaqDTO>> getAllFaqs() {
        log.info("Admin requisitando todas as FAQs");
        List<PlanFaqDTO> faqs = faqService.getAllFaqs();
        return ResponseEntity.ok(faqs);
    }
    
    /**
     * Buscar FAQ por ID (admin)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar FAQ por ID (Admin)")
    public ResponseEntity<PlanFaqDTO> getFaqById(@PathVariable UUID id) {
        log.info("Admin buscando FAQ: {}", id);
        PlanFaqDTO faq = faqService.getFaqById(id);
        return ResponseEntity.ok(faq);
    }
    
    /**
     * Criar nova FAQ (admin)
     */
    @PostMapping
    @Operation(summary = "Criar nova FAQ (Admin)")
    public ResponseEntity<PlanFaqDTO> createFaq(@RequestBody PlanFaqDTO dto) {
        log.info("Admin criando nova FAQ");
        PlanFaqDTO created = faqService.createFaq(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Atualizar FAQ (admin)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar FAQ (Admin)")
    public ResponseEntity<PlanFaqDTO> updateFaq(
            @PathVariable UUID id,
            @RequestBody PlanFaqDTO dto) {
        log.info("Admin atualizando FAQ: {}", id);
        PlanFaqDTO updated = faqService.updateFaq(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Deletar FAQ (admin)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar FAQ (Admin)")
    public ResponseEntity<Void> deleteFaq(@PathVariable UUID id) {
        log.info("Admin deletando FAQ: {}", id);
        faqService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Alternar status da FAQ (admin)
     */
    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Alternar status da FAQ (Admin)")
    public ResponseEntity<PlanFaqDTO> toggleFaqStatus(@PathVariable UUID id) {
        log.info("Admin alternando status da FAQ: {}", id);
        PlanFaqDTO updated = faqService.toggleFaqStatus(id);
        return ResponseEntity.ok(updated);
    }
}
