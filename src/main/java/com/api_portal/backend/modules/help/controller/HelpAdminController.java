package com.api_portal.backend.modules.help.controller;

import com.api_portal.backend.modules.help.dto.HelpCategoryDTO;
import com.api_portal.backend.modules.help.dto.HelpFaqDTO;
import com.api_portal.backend.modules.help.service.HelpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/help")
@RequiredArgsConstructor
public class HelpAdminController {
    
    private final HelpService helpService;
    
    // Category management
    @GetMapping("/categories")
    public ResponseEntity<List<HelpCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(helpService.getAllCategories());
    }
    
    @GetMapping("/categories/{id}")
    public ResponseEntity<HelpCategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(helpService.getCategoryById(id));
    }
    
    @PostMapping("/categories")
    public ResponseEntity<HelpCategoryDTO> createCategory(@RequestBody HelpCategoryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(helpService.createCategory(dto));
    }
    
    @PutMapping("/categories/{id}")
    public ResponseEntity<HelpCategoryDTO> updateCategory(@PathVariable Long id, @RequestBody HelpCategoryDTO dto) {
        return ResponseEntity.ok(helpService.updateCategory(id, dto));
    }
    
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        helpService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
    
    // FAQ management
    @GetMapping("/categories/{categoryId}/faqs")
    public ResponseEntity<List<HelpFaqDTO>> getFaqsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(helpService.getFaqsByCategory(categoryId));
    }
    
    @GetMapping("/faqs/{id}")
    public ResponseEntity<HelpFaqDTO> getFaqById(@PathVariable Long id) {
        return ResponseEntity.ok(helpService.getFaqById(id));
    }
    
    @PostMapping("/faqs")
    public ResponseEntity<HelpFaqDTO> createFaq(@RequestBody HelpFaqDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(helpService.createFaq(dto));
    }
    
    @PutMapping("/faqs/{id}")
    public ResponseEntity<HelpFaqDTO> updateFaq(@PathVariable Long id, @RequestBody HelpFaqDTO dto) {
        return ResponseEntity.ok(helpService.updateFaq(id, dto));
    }
    
    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        helpService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }
}
