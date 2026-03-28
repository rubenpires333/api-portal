package com.api_portal.backend.modules.api.repository;

import com.api_portal.backend.modules.api.domain.ApiCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiCategoryRepository extends JpaRepository<ApiCategory, UUID> {
    
    Optional<ApiCategory> findBySlug(String slug);
    
    List<ApiCategory> findAllByOrderByDisplayOrderAsc();
    
    boolean existsBySlug(String slug);
    
    boolean existsByName(String name);
}
