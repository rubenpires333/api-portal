package com.api_portal.backend.modules.api.repository;

import com.api_portal.backend.modules.api.domain.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, UUID> {
    
    List<ApiEndpoint> findByVersionId(UUID versionId);
    
    @Query("SELECT e FROM ApiEndpoint e WHERE e.version.id = :versionId AND e.method = :method")
    List<ApiEndpoint> findByVersionIdAndMethod(@Param("versionId") UUID versionId, @Param("method") String method);
    
    @Query("SELECT e FROM ApiEndpoint e WHERE e.version.id = :versionId AND e.isDeprecated = false")
    List<ApiEndpoint> findActiveEndpoints(@Param("versionId") UUID versionId);
}
