package com.api_portal.backend.modules.api.repository;

import com.api_portal.backend.modules.api.domain.ApiVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiVersionRepository extends JpaRepository<ApiVersion, UUID> {
    
    List<ApiVersion> findByApiId(UUID apiId);
    
    @Query("SELECT v FROM ApiVersion v WHERE v.api.id = :apiId AND v.isDefault = true")
    Optional<ApiVersion> findDefaultVersion(@Param("apiId") UUID apiId);
    
    @Query("SELECT v FROM ApiVersion v WHERE v.api.id = :apiId AND v.version = :version")
    Optional<ApiVersion> findByApiIdAndVersion(@Param("apiId") UUID apiId, @Param("version") String version);
    
    @Query("SELECT v FROM ApiVersion v WHERE v.api.id = :apiId ORDER BY v.createdAt DESC")
    List<ApiVersion> findByApiIdOrderByCreatedAtDesc(@Param("apiId") UUID apiId);
}
