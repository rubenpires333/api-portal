package com.api_portal.backend.modules.consumer.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.dto.ApiResponse;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerApiService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final ApiRepository apiRepository;
    
    @Transactional(readOnly = true)
    public Page<ApiResponse> getMyApis(UUID consumerId, String search, String category, Pageable pageable) {
        log.info("Buscando APIs com subscrição ativa para consumer: {}", consumerId);
        
        // Buscar todas as subscriptions ativas do consumer
        var subscriptions = subscriptionRepository.findByConsumerIdAndStatus(
            consumerId, SubscriptionStatus.ACTIVE);
        
        if (subscriptions.isEmpty()) {
            log.info("Consumer {} não possui subscrições ativas", consumerId);
            return Page.empty(pageable);
        }
        
        // Extrair IDs das APIs
        List<UUID> apiIds = subscriptions.stream()
            .map(sub -> sub.getApi().getId())
            .collect(Collectors.toList());
        
        log.info("Consumer {} possui {} APIs com subscrição ativa", consumerId, apiIds.size());
        
        // Buscar APIs completas
        List<Api> apis = apiRepository.findAllById(apiIds);
        
        // Aplicar filtros
        List<Api> filteredApis = apis.stream()
            .filter(api -> search == null || search.isEmpty() || 
                api.getName().toLowerCase().contains(search.toLowerCase()) ||
                api.getDescription().toLowerCase().contains(search.toLowerCase()))
            .filter(api -> category == null || category.isEmpty() ||
                (api.getCategory() != null && api.getCategory().getName().equalsIgnoreCase(category)))
            .collect(Collectors.toList());
        
        // Converter para DTO manualmente
        List<ApiResponse> apiResponses = filteredApis.stream()
            .map(this::toApiResponse)
            .collect(Collectors.toList());
        
        // Aplicar paginação manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), apiResponses.size());
        
        List<ApiResponse> pageContent = apiResponses.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, apiResponses.size());
    }
    
    private ApiResponse toApiResponse(Api api) {
        // Criar CategorySummary
        ApiResponse.CategorySummary categorySummary = null;
        if (api.getCategory() != null) {
            categorySummary = ApiResponse.CategorySummary.builder()
                .id(api.getCategory().getId())
                .name(api.getCategory().getName())
                .slug(api.getCategory().getSlug())
                .build();
        }
        
        // Criar ProviderInfo
        ApiResponse.ProviderInfo providerInfo = ApiResponse.ProviderInfo.builder()
            .id(api.getProviderId().toString())
            .name(api.getProviderName())
            .email(api.getProviderEmail())
            .build();
        
        return ApiResponse.builder()
            .id(api.getId())
            .name(api.getName())
            .slug(api.getSlug())
            .description(api.getDescription())
            .shortDescription(api.getShortDescription())
            .status(api.getStatus())
            .visibility(api.getVisibility())
            .category(categorySummary)
            .provider(providerInfo)
            .baseUrl(api.getBaseUrl())
            .documentationUrl(api.getDocumentationUrl())
            .termsOfServiceUrl(api.getTermsOfServiceUrl())
            .authType(api.getAuthType())
            .logoUrl(api.getLogoUrl())
            .iconUrl(api.getIconUrl())
            .tags(api.getTags())
            .rateLimit(api.getRateLimit())
            .rateLimitPeriod(api.getRateLimitPeriod())
            .requiresApproval(api.getRequiresApproval())
            .isActive(api.getIsActive())
            .createdAt(api.getCreatedAt())
            .updatedAt(api.getUpdatedAt())
            .publishedAt(api.getPublishedAt())
            .build();
    }
}
