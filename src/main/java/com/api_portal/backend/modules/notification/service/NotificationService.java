package com.api_portal.backend.modules.notification.service;

import com.api_portal.backend.modules.notification.domain.entity.Notification;
import com.api_portal.backend.modules.notification.domain.entity.NotificationPreference;
import com.api_portal.backend.modules.notification.domain.entity.NotificationTemplate;
import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.notification.domain.repository.NotificationPreferenceRepository;
import com.api_portal.backend.modules.notification.domain.repository.NotificationRepository;
import com.api_portal.backend.modules.notification.domain.repository.NotificationTemplateRepository;
import com.api_portal.backend.modules.notification.dto.NotificationPreferenceRequest;
import com.api_portal.backend.modules.notification.dto.NotificationPreferenceResponse;
import com.api_portal.backend.modules.notification.dto.NotificationResponse;
import com.api_portal.backend.modules.notification.dto.NotificationTemplateRequest;
import com.api_portal.backend.modules.notification.dto.NotificationTemplateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailNotificationService emailService;
    
    /**
     * Criar e enviar notificação
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(
            String userId,
            String userEmail,
            NotificationType type,
            String title,
            String message,
            Map<String, Object> data,
            String actionUrl) {
        
        // Verificar preferências do usuário
        NotificationPreference preference = preferenceRepository
            .findByUserIdAndNotificationType(userId, type)
            .orElseGet(() -> createDefaultPreference(userId, type));
        
        // Criar notificação in-app
        if (preference.getInAppEnabled()) {
            Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .isRead(false)
                .actionUrl(actionUrl)
                .build();
            
            notificationRepository.save(notification);
        }
        
        // Enviar email se habilitado
        if (preference.getEmailEnabled() && userEmail != null) {
            emailService.sendNotificationEmail(userEmail, type, data);
        }
    }
    
    /**
     * Listar notificações do usuário
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::mapToResponse);
    }
    
    /**
     * Buscar notificações por texto
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchNotifications(
            String userId, 
            String search,
            NotificationType type,
            Pageable pageable) {
        
        if (search == null || search.trim().isEmpty()) {
            if (type != null) {
                return getMyNotificationsByType(userId, type, pageable);
            }
            return getMyNotifications(userId, pageable);
        }
        
        if (type != null) {
            return notificationRepository.searchByUserIdAndTypeAndTitleOrMessage(userId, type, search, pageable)
                .map(this::mapToResponse);
        }
        
        return notificationRepository.searchByUserIdAndTitleOrMessage(userId, search, pageable)
            .map(this::mapToResponse);
    }
    
    /**
     * Listar notificações por tipo
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotificationsByType(
            String userId, 
            NotificationType type, 
            Pageable pageable) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
            .map(this::mapToResponse);
    }
    
    /**
     * Listar notificações não lidas
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable)
            .map(this::mapToResponse);
    }
    
    /**
     * Obter últimas notificações (para dropdown)
     * Retorna 4 notificações com prioridade para não lidas
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(String userId) {
        Pageable pageable = Pageable.ofSize(4);
        return notificationRepository.findTop4ByUserIdOrderByIsReadAndCreatedAt(userId, pageable)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Contar notificações não lidas
     */
    @Transactional(readOnly = true)
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }
    
    /**
     * Marcar notificação como lida
     */
    @Transactional
    public void markAsRead(UUID notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    /**
     * Marcar todas como lidas
     */
    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }
    
    /**
     * Deletar notificação
     */
    @Transactional
    public void deleteNotification(UUID notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário");
        }
        
        notificationRepository.delete(notification);
    }
    
    /**
     * Obter preferências do usuário
     */
    @Transactional
    public List<NotificationPreferenceResponse> getPreferences(String userId) {
        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);
        
        // Se não tem preferências, criar defaults
        if (preferences.isEmpty()) {
            preferences = createDefaultPreferences(userId);
        }
        
        return preferences.stream()
            .map(this::mapPreferenceToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Atualizar preferências
     */
    @Transactional
    public NotificationPreferenceResponse updatePreference(
            String userId, 
            NotificationPreferenceRequest request) {
        
        NotificationPreference preference = preferenceRepository
            .findByUserIdAndNotificationType(userId, request.getNotificationType())
            .orElse(NotificationPreference.builder()
                .userId(userId)
                .notificationType(request.getNotificationType())
                .build());
        
        preference.setInAppEnabled(request.getInAppEnabled());
        preference.setEmailEnabled(request.getEmailEnabled());
        
        preference = preferenceRepository.save(preference);
        return mapPreferenceToResponse(preference);
    }
    
    // Helper methods
    
    private NotificationPreference createDefaultPreference(String userId, NotificationType type) {
        NotificationPreference preference = NotificationPreference.builder()
            .userId(userId)
            .notificationType(type)
            .inAppEnabled(true)
            .emailEnabled(true)
            .build();
        
        return preferenceRepository.save(preference);
    }
    
    private List<NotificationPreference> createDefaultPreferences(String userId) {
        return List.of(NotificationType.values()).stream()
            .map(type -> createDefaultPreference(userId, type))
            .collect(Collectors.toList());
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .type(notification.getType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .data(notification.getData())
            .isRead(notification.getIsRead())
            .actionUrl(notification.getActionUrl())
            .createdAt(notification.getCreatedAt())
            .build();
    }
    
    private NotificationPreferenceResponse mapPreferenceToResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
            .id(preference.getId())
            .notificationType(preference.getNotificationType())
            .inAppEnabled(preference.getInAppEnabled())
            .emailEnabled(preference.getEmailEnabled())
            .build();
    }
    
    // Template Management Methods
    
    /**
     * Listar todos os templates
     */
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
            .map(this::mapTemplateToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Obter template por ID
     */
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateById(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template não encontrado"));
        return mapTemplateToResponse(template);
    }
    
    /**
     * Criar novo template
     */
    @Transactional
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
            .type(request.getType())
            .channel(request.getChannel())
            .language(request.getLanguage())
            .subject(request.getSubject())
            .template(request.getTemplate())
            .variables(request.getVariables())
            .build();
        
        template = templateRepository.save(template);
        return mapTemplateToResponse(template);
    }
    
    /**
     * Atualizar template
     */
    @Transactional
    public NotificationTemplateResponse updateTemplate(UUID id, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template não encontrado"));
        
        template.setSubject(request.getSubject());
        template.setTemplate(request.getTemplate());
        template.setVariables(request.getVariables());
        
        template = templateRepository.save(template);
        return mapTemplateToResponse(template);
    }
    
    /**
     * Deletar template
     */
    @Transactional
    public void deleteTemplate(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template não encontrado"));
        templateRepository.delete(template);
    }
    
    private NotificationTemplateResponse mapTemplateToResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
            .id(template.getId())
            .type(template.getType())
            .channel(template.getChannel())
            .language(template.getLanguage())
            .subject(template.getSubject())
            .template(template.getTemplate())
            .variables(template.getVariables())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
