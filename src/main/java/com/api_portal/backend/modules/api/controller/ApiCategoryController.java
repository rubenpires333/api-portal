package com.api_portal.backend.modules.api.controller;

import com.api_portal.backend.modules.api.dto.ApiCategoryRequest;
import com.api_portal.backend.modules.api.dto.ApiCategoryResponse;
import com.api_portal.backend.modules.api.service.ApiCategoryService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @RequiresPermission("category.create")
    @Operation(
        summary = "Criar nova categoria",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiCategoryResponse> createCategory(@Valid @RequestBody ApiCategoryRequest request) {
        ApiCategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @RequiresPermission("category.update")
    @Operation(
        summary = "Atualizar categoria",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiCategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody ApiCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("category.delete")
    @Operation(
        summary = "Deletar categoria",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
