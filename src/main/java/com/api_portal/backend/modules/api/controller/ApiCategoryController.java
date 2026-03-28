package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.ApiCategoryRequest;
import com.api_portal.backend.modules.api.dto.ApiCategoryResponse;
import com.api_portal.backend.modules.api.service.ApiCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "API Categories", description = "Gerenciamento de categorias de APIs")
public class ApiCategoryController {
    
    private final ApiCategoryService categoryService;
    
    @GetMapping
    @Operation(summary = "Listar todas as categorias")
    public ResponseEntity<List<ApiCategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de uma categoria")
    public ResponseEntity<ApiCategoryResponse> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Criar nova categoria (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiCategoryResponse> createCategory(@Valid @RequestBody ApiCategoryRequest request) {
        ApiCategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Atualizar categoria (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiCategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody ApiCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Deletar categoria (apenas ADMIN)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
