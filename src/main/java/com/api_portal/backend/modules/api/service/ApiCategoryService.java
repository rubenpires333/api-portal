package com.api_portal.backend.modules.api.service;

import com.api_portal.backend.modules.api.domain.ApiCategory;
import com.api_portal.backend.modules.api.dto.ApiCategoryRequest;
import com.api_portal.backend.modules.api.dto.ApiCategoryResponse;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiCategoryService {
    
    private final ApiCategoryRepository categoryRepository;
    
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ApiCategoryResponse createCategory(ApiCategoryRequest request) {
        log.info("Criando categoria: {}", request.getName());
        
        String slug = generateSlug(request.getName());
        
        if (categoryRepository.existsBySlug(slug)) {
            throw new ApiException("Categoria com este nome já existe");
        }
        
        ApiCategory category = ApiCategory.builder()
            .name(request.getName())
            .slug(slug)
            .description(request.getDescription())
            .iconUrl(request.getIconUrl())
            .displayOrder(request.getDisplayOrder())
            .build();
        
        category = categoryRepository.save(category);
        
        return mapToResponse(category);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<ApiCategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public ApiCategoryResponse getCategoryById(UUID id) {
        ApiCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException("Categoria não encontrada"));
        
        return mapToResponse(category);
    }
    
    @Transactional
    @CachePut(value = "categories", key = "#id")
    @CacheEvict(value = "categories", key = "'all'")
    public ApiCategoryResponse updateCategory(UUID id, ApiCategoryRequest request) {
        log.info("Atualizando categoria: {}", id);
        
        ApiCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException("Categoria não encontrada"));
        
        String newSlug = generateSlug(request.getName());
        if (!category.getSlug().equals(newSlug) && categoryRepository.existsBySlug(newSlug)) {
            throw new ApiException("Categoria com este nome já existe");
        }
        
        category.setName(request.getName());
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setDisplayOrder(request.getDisplayOrder());
        
        category = categoryRepository.save(category);
        
        return mapToResponse(category);
    }
    
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(UUID id) {
        log.info("Deletando categoria: {}", id);
        
        ApiCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException("Categoria não encontrada"));
        
        if (!category.getApis().isEmpty()) {
            throw new ApiException("Não é possível deletar categoria com APIs associadas");
        }
        
        categoryRepository.delete(category);
    }
    
    private ApiCategoryResponse mapToResponse(ApiCategory category) {
        return ApiCategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .description(category.getDescription())
            .iconUrl(category.getIconUrl())
            .displayOrder(category.getDisplayOrder())
            .apiCount(category.getApis().size())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
    
    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
