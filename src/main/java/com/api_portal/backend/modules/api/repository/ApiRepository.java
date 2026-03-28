package com.api_portal.backend.modules.api.repository;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.domain.enums.ApiVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiRepository extends JpaRepository<Api, UUID> {
    
    Optional<Api> findBySlug(String slug);
    
    List<Api> findByProviderId(String providerId);
    
    Page<Api> findByStatus(ApiStatus status, Pageable pageable);
    
    Page<Api> findByVisibility(ApiVisibility visibility, Pageable pageable);
    
    Page<Api> findByStatusAndVisibility(ApiStatus status, ApiVisibility visibility, Pageable pageable);
    
    @Query("SELECT a FROM Api a WHERE a.status = :status AND a.visibility = 'PUBLIC' AND a.isActive = true")
    Page<Api> findPublicApis(@Param("status") ApiStatus status, Pageable pageable);
    
    @Query("SELECT a FROM Api a WHERE " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.shortDescription) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "a.status = 'PUBLISHED' AND a.isActive = true")
    Page<Api> searchApis(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Api a JOIN a.tags t WHERE t IN :tags AND a.status = 'PUBLISHED' AND a.isActive = true")
    Page<Api> findByTags(@Param("tags") List<String> tags, Pageable pageable);
    
    boolean existsBySlug(String slug);
}
