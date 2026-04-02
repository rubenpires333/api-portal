package com.api_portal.backend.modules.auth.repository;

import com.api_portal.backend.modules.auth.domain.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    Optional<LoginAttempt> findByEmailAndIpAddress(String email, String ipAddress);
    
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.lastAttempt < :cutoffTime")
    void deleteOldAttempts(LocalDateTime cutoffTime);
}
