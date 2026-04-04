package com.api_portal.backend.modules.subscription.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import com.api_portal.backend.modules.subscription.dto.SubscriptionRequest;
import com.api_portal.backend.modules.subscription.dto.SubscriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    
    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private ApiRepository apiRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private Jwt jwt;
    
    @InjectMocks
    private SubscriptionService subscriptionService;
    
    private Api testApi;
    private UUID apiId;
    private UUID testConsumerId;
    
    @BeforeEach
    void setUp() {
        apiId = UUID.randomUUID();
        testConsumerId = UUID.randomUUID();
        
        testApi = Api.builder()
            .id(apiId)
            .name("Test API")
            .slug("test-api")
            .status(ApiStatus.PUBLISHED)
            .isActive(true)
            .build();
    }
    
    @Test
    void subscribe_Success() {
        // Arrange
        SubscriptionRequest request = new SubscriptionRequest();
        request.setApiId(apiId);
        request.setNotes("Test subscription");
        
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testConsumerId.toString());
        when(jwt.getClaimAsString("email")).thenReturn("consumer@test.com");
        when(jwt.getClaimAsString("name")).thenReturn("Test Consumer");
        
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(testApi));
        when(subscriptionRepository.existsByConsumerIdAndApiIdAndStatus(
            any(UUID.class), any(UUID.class), any(SubscriptionStatus.class)
        )).thenReturn(false);
        
        Subscription savedSubscription = Subscription.builder()
            .id(UUID.randomUUID())
            .api(testApi)
            .consumerId(testConsumerId)
            .consumerEmail("consumer@test.com")
            .status(SubscriptionStatus.ACTIVE)
            .apiKey("apk_test123")
            .build();
        
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(savedSubscription);
        
        // Act
        SubscriptionResponse response = subscriptionService.subscribe(request, authentication);
        
        // Assert
        assertNotNull(response);
        assertEquals("Test API", response.getApiName());
        assertEquals("consumer@test.com", response.getConsumerEmail());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
        verify(eventPublisher, times(1)).publishEvent(any());
    }
    
    @Test
    void subscribe_ApiNotFound() {
        // Arrange
        SubscriptionRequest request = new SubscriptionRequest();
        request.setApiId(apiId);
        
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testConsumerId.toString());
        when(apiRepository.findById(apiId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            subscriptionService.subscribe(request, authentication);
        });
        
        // Verify que não tentou salvar
        verify(subscriptionRepository, never()).save(any());
    }
    
    @Test
    void subscribe_ApiNotPublished() {
        // Arrange
        testApi.setStatus(ApiStatus.DRAFT);
        SubscriptionRequest request = new SubscriptionRequest();
        request.setApiId(apiId);
        
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(testConsumerId.toString());
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(testApi));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            subscriptionService.subscribe(request, authentication);
        });
        
        // Verify que não tentou salvar
        verify(subscriptionRepository, never()).save(any());
    }
    
    @Test
    void validateApiKey_Success() {
        // Arrange
        String apiKey = "apk_test123";
        Subscription subscription = Subscription.builder()
            .id(UUID.randomUUID())
            .api(testApi)
            .apiKey(apiKey)
            .status(SubscriptionStatus.ACTIVE)
            .build();
        
        when(subscriptionRepository.findActiveByApiKey(apiKey)).thenReturn(Optional.of(subscription));
        
        // Act
        Subscription result = subscriptionService.validateApiKey(apiKey);
        
        // Assert
        assertNotNull(result);
        assertEquals(apiKey, result.getApiKey());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
    }
    
    @Test
    void validateApiKey_Invalid() {
        // Arrange
        String apiKey = "invalid_key";
        when(subscriptionRepository.findActiveByApiKey(apiKey)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            subscriptionService.validateApiKey(apiKey);
        });
    }
}
