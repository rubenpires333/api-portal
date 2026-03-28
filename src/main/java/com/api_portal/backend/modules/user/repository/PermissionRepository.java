package com.api_portal.backend.modules.user.repository;

import com.api_portal.backend.modules.user.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    
    Optional<Permission> findByCode(String code);
    
    boolean existsByCode(String code);
    
    @Query("SELECT p FROM Permission p WHERE p.active = true ORDER BY p.resource, p.action")
    List<Permission> findAllActive();
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.active = true")
    List<Permission> findByResource(@Param("resource") String resource);
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action")
    Optional<Permission> findByResourceAndAction(@Param("resource") String resource, @Param("action") String action);
}
