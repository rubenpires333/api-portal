package com.api_portal.backend.modules.user.repository;

import com.api_portal.backend.modules.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByKeycloakId(String keycloakId);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByKeycloakId(String keycloakId);
    
    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findAllActive(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.code = :roleCode")
    Page<User> findByRoleCode(@Param("roleCode") String roleCode, Pageable pageable);
    
    // Métodos para dashboard
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.code = :roleCode")
    long countByRolesContaining(@Param("roleCode") String roleCode);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt > :date")
    long countByLastLoginAfter(@Param("date") LocalDateTime date);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    long countByCreatedAtBefore(LocalDateTime date);
    
    List<User> findTop5ByOrderByCreatedAtDesc();
    
    List<User> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date);
}
