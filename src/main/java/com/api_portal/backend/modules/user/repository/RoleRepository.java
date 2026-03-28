package com.api_portal.backend.modules.user.repository;

import com.api_portal.backend.modules.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    
    Optional<Role> findByCode(String code);
    
    Optional<Role> findByName(String name);
    
    boolean existsByCode(String code);
    
    boolean existsByName(String name);
    
    @Query("SELECT r FROM Role r WHERE r.active = true ORDER BY r.name")
    List<Role> findAllActive();
    
    @Query("SELECT r FROM Role r WHERE r.isSystem = false ORDER BY r.name")
    List<Role> findAllCustomRoles();
}
