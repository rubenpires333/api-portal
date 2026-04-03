package com.api_portal.backend.modules.api.service;

import com.api_portal.backend.modules.api.domain.ApiEndpoint;
import com.api_portal.backend.modules.api.domain.ApiVersion;
import com.api_portal.backend.modules.api.dto.ApiEndpointRequest;
import com.api_portal.backend.modules.api.dto.ApiEndpointResponse;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiEndpointRepository;
import com.api_portal.backend.modules.api.repository.ApiVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiEndpointService {
    
    private final ApiEndpointRepository endpointRepository;
    private final ApiVersionRepository versionRepository;
    
    @Transactional
    public ApiEndpointResponse createEndpoint(UUID versionId, ApiEndpointRequest request, String providerId) {
        log.info("Criando endpoint {} {} para versão {}", request.getMethod(), request.getPath(), versionId);
        
        ApiVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        if (!version.getApi().getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para criar endpoints nesta API");
        }
        
        ApiEndpoint endpoint = ApiEndpoint.builder()
            .version(version)
            .path(request.getPath())
            .method(request.getMethod().toUpperCase())
            .summary(request.getSummary())
            .description(request.getDescription())
            .tags(request.getTags())
            .requiresAuth(request.getRequiresAuth())
            .authHeadersJson(request.getAuthHeadersJson())
            .authQueryParamsJson(request.getAuthQueryParamsJson())
            .isDeprecated(false)
            .requestExample(request.getRequestExample())
            .responseExample(request.getResponseExample())
            .build();
        
        endpoint = endpointRepository.save(endpoint);
        
        return mapToResponse(endpoint);
    }
    
    @Transactional(readOnly = true)
    public List<ApiEndpointResponse> getEndpointsByVersionId(UUID versionId) {
        List<ApiEndpoint> endpoints = endpointRepository.findByVersionId(versionId);
        return endpoints.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ApiEndpointResponse getEndpointById(UUID id) {
        ApiEndpoint endpoint = endpointRepository.findById(id)
            .orElseThrow(() -> new ApiException("Endpoint não encontrado"));
        
        return mapToResponse(endpoint);
    }
    
    @Transactional
    public ApiEndpointResponse updateEndpoint(UUID id, ApiEndpointRequest request, String providerId) {
        log.info("Atualizando endpoint: {}", id);
        
        ApiEndpoint endpoint = endpointRepository.findById(id)
            .orElseThrow(() -> new ApiException("Endpoint não encontrado"));
        
        if (!endpoint.getVersion().getApi().getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para atualizar este endpoint");
        }
        
        endpoint.setPath(request.getPath());
        endpoint.setMethod(request.getMethod().toUpperCase());
        endpoint.setSummary(request.getSummary());
        endpoint.setDescription(request.getDescription());
        endpoint.setTags(request.getTags());
        endpoint.setRequiresAuth(request.getRequiresAuth());
        endpoint.setAuthHeadersJson(request.getAuthHeadersJson());
        endpoint.setAuthQueryParamsJson(request.getAuthQueryParamsJson());
        endpoint.setRequestExample(request.getRequestExample());
        endpoint.setResponseExample(request.getResponseExample());
        
        endpoint = endpointRepository.save(endpoint);
        
        return mapToResponse(endpoint);
    }
    
    @Transactional
    public void deleteEndpoint(UUID id, String providerId) {
        log.info("Deletando endpoint: {}", id);
        
        ApiEndpoint endpoint = endpointRepository.findById(id)
            .orElseThrow(() -> new ApiException("Endpoint não encontrado"));
        
        if (!endpoint.getVersion().getApi().getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para deletar este endpoint");
        }
        
        endpointRepository.delete(endpoint);
    }
    
    private ApiEndpointResponse mapToResponse(ApiEndpoint endpoint) {
        return ApiEndpointResponse.builder()
            .id(endpoint.getId())
            .versionId(endpoint.getVersion().getId())
            .path(endpoint.getPath())
            .method(endpoint.getMethod())
            .summary(endpoint.getSummary())
            .description(endpoint.getDescription())
            .tags(endpoint.getTags())
            .requiresAuth(endpoint.getRequiresAuth())
            .authHeadersJson(endpoint.getAuthHeadersJson())
            .authQueryParamsJson(endpoint.getAuthQueryParamsJson())
            .isDeprecated(endpoint.getIsDeprecated())
            .requestExample(endpoint.getRequestExample())
            .responseExample(endpoint.getResponseExample())
            .responseTime(endpoint.getResponseTime())
            .successRate(endpoint.getSuccessRate())
            .createdAt(endpoint.getCreatedAt())
            .updatedAt(endpoint.getUpdatedAt())
            .build();
    }
}
