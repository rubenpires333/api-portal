package com.api_portal.backend.modules.subscription.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.notification.event.SubscriptionApprovedEvent;
import com.api_portal.backend.modules.notification.event.SubscriptionRequestedEvent;
import com.api_portal.backend.modules.notification.event.SubscriptionRevokedEvent;
import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import com.api_portal.backend.modules.subscription.dto.RevokeRequest;
import com.api_portal.backend.modules.subscription.dto.SubscriptionRequest;
import com.api_portal.backend.modules.subscription.dto.SubscriptionResponse;
import com.api_portal.backend.modules.user.domain.User;
import com.api_portal.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final ApiRepository apiRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Consumer subscreve uma API
     */
    @Transactional
    public SubscriptionResponse subscribe(SubscriptionRequest request, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr); // Converter String para UUID
        String consumerEmail = jwt.getClaimAsString("email");
        String consumerName = jwt.getClaimAsString("name");
        
        // Verificar se API existe e está publicada
        Api api = apiRepository.findById(request.getApiId())
            .orElseThrow(() -> new IllegalArgumentException("API não encontrada"));
        
        if (api.getStatus() != ApiStatus.PUBLISHED) {
            throw new IllegalStateException("Apenas APIs publicadas podem ser subscritas");
        }
        
        // Verificar se já existe subscrição ativa ou pendente
        if (subscriptionRepository.existsByConsumerIdAndApiIdAndStatus(
                consumerId, request.getApiId(), SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("Já existe uma subscrição ativa para esta API");
        }
        
        if (subscriptionRepository.existsByConsumerIdAndApiIdAndStatus(
                consumerId, request.getApiId(), SubscriptionStatus.PENDING)) {
            throw new IllegalStateException("Já existe uma subscrição pendente de aprovação para esta API");
        }
        
        // Buscar versão padrão da API
        UUID defaultVersionId = null;
        if (api.getVersions() != null && !api.getVersions().isEmpty()) {
            var defaultVersion = api.getVersions().stream()
                .filter(v -> v.getIsDefault() != null && v.getIsDefault())
                .findFirst();
            
            if (defaultVersion.isPresent()) {
                defaultVersionId = defaultVersion.get().getId();
            } else {
                // Se não houver versão padrão, usar a primeira versão publicada
                defaultVersionId = api.getVersions().stream()
                    .filter(v -> v.getStatus() == com.api_portal.backend.modules.api.domain.enums.ApiStatus.PUBLISHED)
                    .findFirst()
                    .map(v -> v.getId())
                    .orElse(null);
            }
        }
        
        // Criar subscrição
        // Se a API requer aprovação, criar com status PENDING
        // Caso contrário, aprovar automaticamente
        SubscriptionStatus initialStatus = api.getRequiresApproval() 
            ? SubscriptionStatus.PENDING 
            : SubscriptionStatus.ACTIVE;
        
        LocalDateTime approvedAt = api.getRequiresApproval() 
            ? null 
            : LocalDateTime.now();
        
        Subscription subscription = Subscription.builder()
            .api(api)
            .apiVersionId(defaultVersionId)
            .consumerId(consumerId)
            .consumerEmail(consumerEmail)
            .consumerName(consumerName)
            .status(initialStatus)
            .apiKey(generateApiKey())
            .approvedAt(approvedAt)
            .notes(request.getNotes())
            .requestsUsed(0)
            .requestsLimit(api.getRateLimit() != null ? api.getRateLimit() : 1000)
            .lastResetAt(initialStatus == SubscriptionStatus.ACTIVE ? LocalDateTime.now() : null)
            .build();
        
        subscription = subscriptionRepository.save(subscription);
        
        // Publicar evento de subscription criada
        if (initialStatus == SubscriptionStatus.PENDING) {
            eventPublisher.publishEvent(new SubscriptionRequestedEvent(this, subscription));
            log.info("Nova subscrição PENDENTE criada: {} para API: {} (requer aprovação)", 
                subscription.getId(), api.getName());
        } else {
            eventPublisher.publishEvent(new SubscriptionApprovedEvent(this, subscription));
            log.info("Nova subscrição ATIVA criada: {} para API: {}", 
                subscription.getId(), api.getName());
        }
        
        return mapToResponse(subscription);
    }
    
    /**
     * Listar subscrições do consumer
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getMySubscriptions(Authentication authentication, Pageable pageable) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr);
        
        return subscriptionRepository.findByConsumerId(consumerId, pageable)
            .map(this::mapToResponse);
    }
    
    /**
     * Listar subscrições do consumer (sem paginação)
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptionsList(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr);
        
        return subscriptionRepository.findByConsumerId(consumerId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }
    
    /**
     * Verificar se tem subscrição ativa para uma API
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscriptionByApiId(UUID apiId, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr);
        
        return subscriptionRepository.findByConsumerIdAndApiIdAndStatus(
                consumerId, apiId, SubscriptionStatus.ACTIVE)
            .map(this::mapToResponse)
            .orElse(null);
    }
    
    /**
     * Verificar se tem subscrição ativa ou pendente para uma API
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveOrPendingSubscriptionByApiId(UUID apiId, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr);
        
        // Primeiro tenta encontrar ativa
        var activeSubscription = subscriptionRepository.findByConsumerIdAndApiIdAndStatus(
                consumerId, apiId, SubscriptionStatus.ACTIVE);
        
        if (activeSubscription.isPresent()) {
            return mapToResponse(activeSubscription.get());
        }
        
        // Se não encontrar ativa, tenta encontrar pendente
        var pendingSubscription = subscriptionRepository.findByConsumerIdAndApiIdAndStatus(
                consumerId, apiId, SubscriptionStatus.PENDING);
        
        return pendingSubscription.map(this::mapToResponse).orElse(null);
    }
    
    /**
     * Detalhes de uma subscrição (consumer)
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(UUID id, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr);
        
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subscrição não encontrada"));
        
        if (!subscription.getConsumerId().equals(consumerId)) {
            throw new IllegalStateException("Acesso negado");
        }
        
        return mapToResponse(subscription);
    }
    
    /**
     * Cancelar subscrição (consumer)
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID id, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String consumerIdStr = jwt.getSubject();
        UUID consumerId = UUID.fromString(consumerIdStr);
        
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subscrição não encontrada"));
        
        if (!subscription.getConsumerId().equals(consumerId)) {
            throw new IllegalStateException("Acesso negado");
        }
        
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Subscrição já está cancelada");
        }
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription = subscriptionRepository.save(subscription);
        
        log.info("Subscrição cancelada: {}", id);
        
        return mapToResponse(subscription);
    }
    
    /**
     * Listar subscrições das APIs do provider
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getProviderSubscriptions(
            Authentication authentication, 
            SubscriptionStatus status,
            Pageable pageable) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        
        Page<Subscription> subscriptions;
        if (status != null) {
            subscriptions = subscriptionRepository.findByProviderIdAndStatus(providerId, status, pageable);
        } else {
            subscriptions = subscriptionRepository.findByProviderId(providerId, pageable);
        }
        
        return subscriptions.map(this::mapToResponse);
    }
    
    /**
     * Contar subscrições pendentes do provider
     */
    @Transactional(readOnly = true)
    public long getPendingSubscriptionsCount(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        
        return subscriptionRepository.countByProviderIdAndStatus(providerId, SubscriptionStatus.PENDING);
    }
    
    /**
     * Aprovar subscrição (provider)
     */
    @Transactional
    public SubscriptionResponse approveSubscription(UUID id, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subscrição não encontrada"));
        
        if (!subscription.getApi().getProviderId().equals(providerId)) {
            throw new IllegalStateException("Acesso negado");
        }
        
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new IllegalStateException("Apenas subscrições pendentes podem ser aprovadas");
        }
        
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setApprovedAt(LocalDateTime.now());
        subscription = subscriptionRepository.save(subscription);
        
        // Publicar evento de aprovação
        eventPublisher.publishEvent(new SubscriptionApprovedEvent(this, subscription));
        
        log.info("Subscrição aprovada: {}", id);
        
        return mapToResponse(subscription);
    }
    
    /**
     * Revogar subscrição (provider)
     */
    @Transactional
    public SubscriptionResponse revokeSubscription(
            UUID id, 
            RevokeRequest request,
            Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subscrição não encontrada"));
        
        if (!subscription.getApi().getProviderId().equals(providerId)) {
            throw new IllegalStateException("Acesso negado");
        }
        
        if (subscription.getStatus() == SubscriptionStatus.REVOKED) {
            throw new IllegalStateException("Subscrição já está revogada");
        }
        
        subscription.setStatus(SubscriptionStatus.REVOKED);
        subscription.setRevokedAt(LocalDateTime.now());
        subscription.setRevokeReason(request.getReason());
        subscription = subscriptionRepository.save(subscription);
        
        // Publicar evento de revogação
        eventPublisher.publishEvent(new SubscriptionRevokedEvent(this, subscription, request.getReason()));
        
        log.info("Subscrição revogada: {} - Motivo: {}", id, request.getReason());
        
        return mapToResponse(subscription);
    }
    
    /**
     * Validar API Key (usado pelo gateway)
     */
    public Subscription validateApiKey(String apiKey) {
        return subscriptionRepository.findActiveByApiKey(apiKey)
            .orElseThrow(() -> new IllegalArgumentException("API Key inválida ou inativa"));
    }
    
    /**
     * Incrementar contador de requisições (usado pelo gateway)
     */
    @Transactional
    public void incrementRequestCount(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscrição não encontrada"));
        
        int currentCount = subscription.getRequestsUsed() != null ? subscription.getRequestsUsed() : 0;
        subscription.setRequestsUsed(currentCount + 1);
        subscriptionRepository.save(subscription);
        
        log.debug("Request count incremented for subscription {}: {} -> {}", 
            subscriptionId, currentCount, currentCount + 1);
    }
    
    /**
     * Gerar API Key única
     */
    private String generateApiKey() {
        return "apk_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Mapear entidade para DTO
     */
    private SubscriptionResponse mapToResponse(Subscription subscription) {
        // Buscar versão da subscription
        String apiVersion = "N/A";
        if (subscription.getApiVersionId() != null && subscription.getApi().getVersions() != null) {
            apiVersion = subscription.getApi().getVersions().stream()
                .filter(v -> v.getId().equals(subscription.getApiVersionId()))
                .findFirst()
                .map(v -> v.getVersion())
                .orElse("N/A");
        }
        
        // Buscar id da tabela users pelo keycloakId
        UUID userConsumerId = null;
        try {
            User consumer = userRepository.findByKeycloakId(subscription.getConsumerId().toString())
                .orElse(null);
            if (consumer != null) {
                userConsumerId = consumer.getId();
            }
        } catch (Exception e) {
            log.warn("Não foi possível buscar user id para consumerId: {}", subscription.getConsumerId());
        }
        
        return SubscriptionResponse.builder()
            .id(subscription.getId())
            .apiId(subscription.getApi().getId())
            .apiVersionId(subscription.getApiVersionId())
            .apiName(subscription.getApi().getName())
            .apiSlug(subscription.getApi().getSlug())
            .apiVersion(apiVersion)
            .consumerId(subscription.getConsumerId().toString()) // keycloakId
            .userConsumerId(userConsumerId) // id da tabela users
            .consumerEmail(subscription.getConsumerEmail())
            .consumerName(subscription.getConsumerName())
            .status(subscription.getStatus())
            .apiKey(subscription.getApiKey())
            .requestsUsed(subscription.getRequestsUsed() != null ? subscription.getRequestsUsed() : 0)
            .requestsLimit(subscription.getRequestsLimit())
            .rateLimitPeriod(subscription.getApi().getRateLimitPeriod())
            .lastResetAt(subscription.getLastResetAt())
            .startDate(subscription.getCreatedAt())
            .endDate(subscription.getExpiresAt())
            .expiresAt(subscription.getExpiresAt())
            .approvedAt(subscription.getApprovedAt())
            .revokedAt(subscription.getRevokedAt())
            .revokeReason(subscription.getRevokeReason())
            .notes(subscription.getNotes())
            .createdAt(subscription.getCreatedAt())
            .updatedAt(subscription.getUpdatedAt())
            .build();
    }
}
