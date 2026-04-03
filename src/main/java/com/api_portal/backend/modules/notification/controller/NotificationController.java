package com.api_portal.backend.modules.notification.controller;

import com.api_portal.backend.modules.notification.domain.enums.NotificationType;
import com.api_portal.backend.modules.notification.dto.NotificationPreferenceRequest;
import com.api_portal.backend.modules.notification.dto.NotificationPreferenceResponse;
import com.api_portal.backend.modules.notification.dto.NotificationResponse;
import com.api_portal.backend.modules.notification.dto.NotificationTemplateRequest;
import com.api_portal.backend.modules.notification.dto.NotificationTemplateResponse;
import com.api_portal.backend.modules.notification.service.EmailNotificationService;
import com.api_portal.backend.modules.notification.service.NotificationService;
import com.api_portal.backend.shared.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gerenciamento de notificações")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    
    @GetMapping
    @Operation(summary = "Listar minhas notificações")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<NotificationResponse> notifications;
        
        if (type != null) {
            notifications = notificationService.getMyNotificationsByType(userId, type, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationService.getUnreadNotifications(userId, pageable);
        } else {
            notifications = notificationService.getMyNotifications(userId, pageable);
        }
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/recent")
    @Operation(summary = "Obter últimas 10 notificações (para dropdown)")
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications(
            Authentication authentication) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(notificationService.getRecentNotifications(userId));
    }
    
    @GetMapping("/unread-count")
    @Operation(summary = "Contar notificações não lidas")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String userId = getUserId(authentication);
        long count = notificationService.countUnreadNotifications(userId);
        
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar notificação como lida")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        String userId = getUserId(authentication);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/mark-all-read")
    @Operation(summary = "Marcar todas as notificações como lidas")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(Authentication authentication) {
        String userId = getUserId(authentication);
        int count = notificationService.markAllAsRead(userId);
        
        Map<String, Integer> response = new HashMap<>();
        response.put("markedCount", count);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar notificação")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            Authentication authentication) {
        String userId = getUserId(authentication);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/preferences")
    @Operation(summary = "Obter preferências de notificação")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(
            Authentication authentication) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }
    
    @PutMapping("/preferences")
    @Operation(summary = "Atualizar preferência de notificação")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @Valid @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(notificationService.updatePreference(userId, request));
    }
    
    @PostMapping("/test-email")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Enviar email de teste (apenas SUPER_ADMIN)")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestParam String email,
            Authentication authentication) {
        try {
            emailNotificationService.sendTestEmail(email);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email de teste enviado com sucesso para " + email);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/templates")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Listar todos os templates de notificação (apenas SUPER_ADMIN)")
    public ResponseEntity<List<NotificationTemplateResponse>> getAllTemplates() {
        return ResponseEntity.ok(notificationService.getAllTemplates());
    }
    
    @GetMapping("/templates/{id}")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Obter template por ID (apenas SUPER_ADMIN)")
    public ResponseEntity<NotificationTemplateResponse> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.getTemplateById(id));
    }
    
    @PutMapping("/templates/{id}")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Atualizar template de notificação (apenas SUPER_ADMIN)")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.ok(notificationService.updateTemplate(id, request));
    }
    
    @PostMapping("/templates")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Criar novo template de notificação (apenas SUPER_ADMIN)")
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.ok(notificationService.createTemplate(request));
    }
    
    @DeleteMapping("/templates/{id}")
    @RequiresPermission("settings.manage")
    @Operation(summary = "Deletar template de notificação (apenas SUPER_ADMIN)")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        notificationService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
    
    private String getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
