package com.api_portal.backend.modules.help.service;

import com.api_portal.backend.modules.help.dto.HelpCategoryDTO;
import com.api_portal.backend.modules.help.dto.HelpFaqDTO;
import com.api_portal.backend.modules.help.entity.HelpCategory;
import com.api_portal.backend.modules.help.entity.HelpFaq;
import com.api_portal.backend.modules.help.repository.HelpCategoryRepository;
import com.api_portal.backend.modules.help.repository.HelpFaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HelpService {
    
    private final HelpCategoryRepository categoryRepository;
    private final HelpFaqRepository faqRepository;
    
    // Public methods - accessible to all users
    public List<HelpCategoryDTO> getActiveCategories() {
        return categoryRepository.findByActiveOrderByDisplayOrderAsc(true)
                .stream()
                .map(this::convertCategoryToDTO)
                .collect(Collectors.toList());
    }
    
    public List<HelpFaqDTO> searchFaqs(String search) {
        return faqRepository.searchFaqs(search)
                .stream()
                .map(this::convertFaqToDTO)
                .collect(Collectors.toList());
    }
    
    // Admin methods
    public List<HelpCategoryDTO> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::convertCategoryToDTO)
                .collect(Collectors.toList());
    }
    
    public HelpCategoryDTO getCategoryById(Long id) {
        HelpCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
        return convertCategoryToDTO(category);
    }
    
    @Transactional
    public HelpCategoryDTO createCategory(HelpCategoryDTO dto) {
        HelpCategory category = new HelpCategory();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        category.setActive(dto.getActive() != null ? dto.getActive() : true);
        
        category = categoryRepository.save(category);
        return convertCategoryToDTO(category);
    }
    
    @Transactional
    public HelpCategoryDTO updateCategory(Long id, HelpCategoryDTO dto) {
        HelpCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
        
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setDisplayOrder(dto.getDisplayOrder());
        category.setActive(dto.getActive());
        
        category = categoryRepository.save(category);
        return convertCategoryToDTO(category);
    }
    
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
    
    // FAQ methods
    public List<HelpFaqDTO> getFaqsByCategory(Long categoryId) {
        return faqRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId)
                .stream()
                .map(this::convertFaqToDTO)
                .collect(Collectors.toList());
    }
    
    public HelpFaqDTO getFaqById(Long id) {
        HelpFaq faq = faqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ não encontrado"));
        return convertFaqToDTO(faq);
    }
    
    @Transactional
    public HelpFaqDTO createFaq(HelpFaqDTO dto) {
        HelpCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
        
        HelpFaq faq = new HelpFaq();
        faq.setCategory(category);
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        faq.setActive(dto.getActive() != null ? dto.getActive() : true);
        
        faq = faqRepository.save(faq);
        return convertFaqToDTO(faq);
    }
    
    @Transactional
    public HelpFaqDTO updateFaq(Long id, HelpFaqDTO dto) {
        HelpFaq faq = faqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ não encontrado"));
        
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(faq.getCategory().getId())) {
            HelpCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            faq.setCategory(category);
        }
        
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setDisplayOrder(dto.getDisplayOrder());
        faq.setActive(dto.getActive());
        
        faq = faqRepository.save(faq);
        return convertFaqToDTO(faq);
    }
    
    @Transactional
    public void deleteFaq(Long id) {
        faqRepository.deleteById(id);
    }
    
    // Converters
    private HelpCategoryDTO convertCategoryToDTO(HelpCategory category) {
        HelpCategoryDTO dto = new HelpCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setActive(category.getActive());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        
        if (category.getFaqs() != null) {
            dto.setFaqs(category.getFaqs().stream()
                    .filter(HelpFaq::getActive)
                    .map(this::convertFaqToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private HelpFaqDTO convertFaqToDTO(HelpFaq faq) {
        HelpFaqDTO dto = new HelpFaqDTO();
        dto.setId(faq.getId());
        dto.setCategoryId(faq.getCategory().getId());
        dto.setCategoryName(faq.getCategory().getName());
        dto.setQuestion(faq.getQuestion());
        dto.setAnswer(faq.getAnswer());
        dto.setDisplayOrder(faq.getDisplayOrder());
        dto.setActive(faq.getActive());
        dto.setCreatedAt(faq.getCreatedAt());
        dto.setUpdatedAt(faq.getUpdatedAt());
        return dto;
    }
}
