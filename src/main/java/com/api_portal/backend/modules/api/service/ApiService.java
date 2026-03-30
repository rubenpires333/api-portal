package com.api_portal.backend.modules.api.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.ApiCategory;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.dto.ApiPublicResponse;
import com.api_portal.backend.modules.api.dto.ApiRequest;
import com.api_portal.backend.modules.api.dto.ApiResponse;
import com.api_portal.backend.modules.api.dto.EndpointPublicDto;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiCategoryRepository;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiService {
    
    private final ApiRepository apiRepository;
    private final ApiCategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    @Transactional
    public ApiResponse createApi(ApiRequest request, String providerId, String providerName, String providerEmail) {
        log.info("Criando API: {} por {}", request.getName(), providerId);
        
        String slug = generateSlug(request.getName());
        
        if (apiRepository.existsBySlug(slug)) {
            throw new ApiException("API com este nome já existe");
        }
        
        ApiCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Categoria não encontrada"));
        
        Api api = Api.builder()
            .name(request.getName())
            .slug(slug)
            .description(request.getDescription())
            .shortDescription(request.getShortDescription())
            .category(category)
            .status(ApiStatus.DRAFT)
            .visibility(request.getVisibility())
            .providerId(providerId)
            .providerName(providerName)
            .providerEmail(providerEmail)
            .baseUrl(request.getBaseUrl())
            .documentationUrl(request.getDocumentationUrl())
            .termsOfServiceUrl(request.getTermsOfServiceUrl())
            .authType(request.getAuthType())
            .logoUrl(request.getLogoUrl())
            .iconUrl(request.getIconUrl())
            .tags(request.getTags())
            .rateLimit(request.getRateLimit())
            .rateLimitPeriod(request.getRateLimitPeriod())
            .requiresApproval(request.getRequiresApproval())
            .isActive(true)
            .build();
        
        api = apiRepository.save(api);
        
        return mapToResponse(api);
    }
    
    @Transactional(readOnly = true)
    public Page<ApiResponse> getAllApis(Pageable pageable) {
        return apiRepository.findPublicApis(ApiStatus.PUBLISHED, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ApiResponse> searchApis(String search, Pageable pageable) {
        return apiRepository.searchApis(search, pageable)
            .map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public ApiResponse getApiById(UUID id) {
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        return mapToResponse(api);
    }
    
    @Transactional(readOnly = true)
    public ApiResponse getApiBySlug(String slug) {
        Api api = apiRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        return mapToResponse(api);
    }
    
    @Transactional(readOnly = true)
    public List<ApiResponse> getMyApis(String providerId) {
        return apiRepository.findByProviderId(providerId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ApiResponse updateApi(UUID id, ApiRequest request, String providerId) {
        log.info("Atualizando API: {}", id);
        
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para atualizar esta API");
        }
        
        ApiCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Categoria não encontrada"));
        
        String newSlug = generateSlug(request.getName());
        if (!api.getSlug().equals(newSlug) && apiRepository.existsBySlug(newSlug)) {
            throw new ApiException("API com este nome já existe");
        }
        
        api.setName(request.getName());
        api.setSlug(newSlug);
        api.setDescription(request.getDescription());
        api.setShortDescription(request.getShortDescription());
        api.setCategory(category);
        api.setVisibility(request.getVisibility());
        api.setBaseUrl(request.getBaseUrl());
        api.setDocumentationUrl(request.getDocumentationUrl());
        api.setTermsOfServiceUrl(request.getTermsOfServiceUrl());
        api.setAuthType(request.getAuthType());
        api.setLogoUrl(request.getLogoUrl());
        api.setIconUrl(request.getIconUrl());
        api.setTags(request.getTags());
        api.setRateLimit(request.getRateLimit());
        api.setRateLimitPeriod(request.getRateLimitPeriod());
        api.setRequiresApproval(request.getRequiresApproval());
        
        api = apiRepository.save(api);
        
        return mapToResponse(api);
    }
    
    @Transactional
    public ApiResponse publishApi(UUID id, String providerId) {
        log.info("Publicando API: {}", id);
        
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para publicar esta API");
        }
        
        if (api.getStatus() == ApiStatus.PUBLISHED) {
            throw new ApiException("API já está publicada");
        }
        
        api.setStatus(ApiStatus.PUBLISHED);
        api.setPublishedAt(LocalDateTime.now());
        
        api = apiRepository.save(api);
        
        return mapToResponse(api);
    }
    
    @Transactional
    public ApiResponse deprecateApi(UUID id, String providerId) {
        log.info("Depreciando API: {}", id);
        
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para depreciar esta API");
        }
        
        api.setStatus(ApiStatus.DEPRECATED);
        
        api = apiRepository.save(api);
        
        return mapToResponse(api);
    }
    
    @Transactional
    public void deleteApi(UUID id, String providerId) {
        log.info("Deletando API: {}", id);
        
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para deletar esta API");
        }
        
        apiRepository.delete(api);
    }
    
    @Transactional(readOnly = true)
    public Page<ApiPublicResponse> exploreApis(
            String search, 
            String category, 
            String consumerId, 
            Pageable pageable) {
        
        Page<Api> apis;
        
        if (search != null && !search.isEmpty()) {
            apis = apiRepository.searchApis(search, pageable);
        } else {
            apis = apiRepository.findPublicApis(ApiStatus.PUBLISHED, pageable);
        }
        
        return apis.map(api -> mapToPublicResponse(api, consumerId));
    }
    
    @Transactional(readOnly = true)
    public ApiPublicResponse getApiDetailsForConsumer(String slug, String consumerId) {
        Api api = apiRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (api.getStatus() != ApiStatus.PUBLISHED) {
            throw new ApiException("API não está disponível");
        }
        
        return mapToPublicResponse(api, consumerId);
    }

    @Transactional(readOnly = true)
    public ApiPublicResponse getApiDetailsForConsumer(UUID id, String consumerId) {
        Api api = apiRepository.findById(id)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (api.getStatus() != ApiStatus.PUBLISHED) {
            throw new ApiException("API não está disponível");
        }
        
        return mapToPublicResponse(api, consumerId);
    }
    
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return categoryRepository.findAll()
            .stream()
            .map(ApiCategory::getName)
            .collect(Collectors.toList());
    }
    
    private ApiResponse mapToResponse(Api api) {
        return ApiResponse.builder()
            .id(api.getId())
            .name(api.getName())
            .slug(api.getSlug())
            .description(api.getDescription())
            .shortDescription(api.getShortDescription())
            .category(api.getCategory() != null ? ApiResponse.CategorySummary.builder()
                .id(api.getCategory().getId())
                .name(api.getCategory().getName())
                .slug(api.getCategory().getSlug())
                .build() : null)
            .status(api.getStatus())
            .visibility(api.getVisibility())
            .provider(ApiResponse.ProviderInfo.builder()
                .id(api.getProviderId())
                .name(api.getProviderName())
                .email(api.getProviderEmail())
                .build())
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
            .versions(api.getVersions().stream()
                .map(v -> ApiResponse.VersionSummary.builder()
                    .id(v.getId())
                    .version(v.getVersion())
                    .isDefault(v.getIsDefault())
                    .isDeprecated(v.getIsDeprecated())
                    .status(v.getStatus())
                    .build())
                .collect(Collectors.toList()))
            .createdAt(api.getCreatedAt())
            .updatedAt(api.getUpdatedAt())
            .publishedAt(api.getPublishedAt())
            .build();
    }
    
    private ApiPublicResponse mapToPublicResponse(Api api, String consumerId) {
        boolean isSubscribed = false;
        UUID subscriptionId = null;
        
        if (consumerId != null) {
            var subscription = subscriptionRepository.findByConsumerIdAndApiIdAndStatus(
                consumerId, api.getId(), SubscriptionStatus.ACTIVE);
            
            if (subscription.isPresent()) {
                isSubscribed = true;
                subscriptionId = subscription.get().getId();
            }
        }
        
        List<EndpointPublicDto> endpoints = List.of();
        if (api.getVersions() != null && !api.getVersions().isEmpty()) {
            var defaultVersion = api.getVersions().stream()
                .filter(v -> v.getIsDefault() != null && v.getIsDefault())
                .findFirst();
            
            if (defaultVersion.isPresent() && defaultVersion.get().getEndpoints() != null) {
                endpoints = defaultVersion.get().getEndpoints().stream()
                    .map(e -> EndpointPublicDto.builder()
                        .id(e.getId())
                        .path(e.getPath())
                        .method(e.getMethod())
                        .description(e.getDescription())
                        .build())
                    .collect(Collectors.toList());
            }
        }
        
        return ApiPublicResponse.builder()
            .id(api.getId())
            .name(api.getName())
            .slug(api.getSlug())
            .shortDescription(api.getShortDescription())
            .description(api.getDescription())
            .category(api.getCategory() != null ? ApiPublicResponse.CategorySummary.builder()
                .id(api.getCategory().getId())
                .name(api.getCategory().getName())
                .slug(api.getCategory().getSlug())
                .build() : null)
            .visibility(api.getVisibility())
            .provider(ApiPublicResponse.ProviderInfo.builder()
                .id(api.getProviderId())
                .name(api.getProviderName())
                .email(api.getProviderEmail())
                .build())
            .tags(api.getTags())
            .baseUrl(api.getBaseUrl())
            .documentationUrl(api.getDocumentationUrl())
            .termsOfServiceUrl(api.getTermsOfServiceUrl())
            .endpoints(endpoints)
            .versions(api.getVersions() != null ? api.getVersions().stream()
                .map(v -> ApiPublicResponse.VersionSummary.builder()
                    .id(v.getId())
                    .version(v.getVersion())
                    .isDefault(v.getIsDefault())
                    .isDeprecated(v.getIsDeprecated())
                    .status(v.getStatus())
                    .build())
                .collect(Collectors.toList()) : List.of())
            .isSubscribed(isSubscribed)
            .subscriptionId(subscriptionId)
            .createdAt(api.getCreatedAt())
            .updatedAt(api.getUpdatedAt())
            .publishedAt(api.getPublishedAt())
            .build();
    }
    
    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
