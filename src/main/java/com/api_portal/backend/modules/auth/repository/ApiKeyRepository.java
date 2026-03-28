package com.api_portal.backend.modules.auth.repository;

import com.api_portal.backend.modules.auth.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    Optional<ApiKey> findByKeyValue(String keyValue);
    
    List<ApiKey> findByUserId(String userId);
    
    List<ApiKey> findByUserIdAndActiveTrue(String userId);
    
    boolean existsByKeyValue(String keyValue);
}
