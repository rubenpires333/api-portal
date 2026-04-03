package com.api_portal.backend.modules.notification.domain.repository;

import com.api_portal.backend.modules.notification.domain.entity.NotificationTemplate;
import com.api_portal.backend.modules.notification.domain.enums.NotificationChannel;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    
    Optional<NotificationTemplate> findByTypeAndChannelAndLanguage(
        NotificationType type, 
        NotificationChannel channel, 
        String language
    );
    
    List<NotificationTemplate> findByType(NotificationType type);
    
    List<NotificationTemplate> findByChannel(NotificationChannel channel);
}
