package com.api_portal.backend.modules.notification.domain.repository;

import com.api_portal.backend.modules.notification.domain.entity.NotificationPreference;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    
    List<NotificationPreference> findByUserId(String userId);
    
    Optional<NotificationPreference> findByUserIdAndNotificationType(String userId, NotificationType notificationType);
    
    boolean existsByUserIdAndNotificationType(String userId, NotificationType notificationType);
}
