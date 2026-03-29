package com.api_portal.backend.modules.api.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.ApiVersion;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.dto.ApiVersionRequest;
import com.api_portal.backend.modules.api.dto.ApiVersionResponse;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.api.repository.ApiVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiVersionService {
    
    private final ApiVersionRepository versionRepository;
    private final ApiRepository apiRepository;
    
    @Transactional
    public ApiVersionResponse createVersion(UUID apiId, ApiVersionRequest request, String providerId) {
        log.info("Criando versão {} para API {}", request.getVersion(), apiId);
        
        Api api = apiRepository.findById(apiId)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão para criar versões desta API");
        }
        
        if (versionRepository.findByApiIdAndVersion(apiId, request.getVersion()).isPresent()) {
            throw new ApiException("Versão já existe para esta API");
        }
        
        ApiVersion version = ApiVersion.builder()
            .api(api)
            .version(request.getVersion())
            .description(request.getDescription())
            .status(ApiStatus.DRAFT)
            .isDefault(request.getIsDefault())
            .isDeprecated(false)
            .baseUrl(request.getBaseUrl())
            .openApiSpec(request.getOpenApiSpec())
            .build();
        
        if (request.getIsDefault()) {
            versionRepository.findDefaultVersion(apiId)
                .ifPresent(v -> {
                    v.setIsDefault(false);
                    versionRepository.save(v);
                });
        }
        
        version = versionRepository.save(version);
        
        return mapToResponse(version);
    }
    
    @Transactional(readOnly = true)
    public List<ApiVersionResponse> getVersionsByApiId(UUID apiId) {
        return versionRepository.findByApiIdOrderByCreatedAtDesc(apiId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ApiVersionResponse getVersionById(UUID id) {
        ApiVersion version = versionRepository.findById(id)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        return mapToResponse(version);
    }
    
    @Transactional
    public ApiVersionResponse setDefaultVersion(UUID apiId, UUID versionId, String providerId) {
        Api api = apiRepository.findById(apiId)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão");
        }
        
        versionRepository.findDefaultVersion(apiId)
            .ifPresent(v -> {
                v.setIsDefault(false);
                versionRepository.save(v);
            });
        
        ApiVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        version.setIsDefault(true);
        version = versionRepository.save(version);
        
        return mapToResponse(version);
    }
    
    @Transactional
    public ApiVersionResponse publishVersion(UUID apiId, UUID versionId, String providerId) {
        Api api = apiRepository.findById(apiId)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão");
        }
        
        ApiVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        if (!version.getApi().getId().equals(apiId)) {
            throw new ApiException("Versão não pertence a esta API");
        }
        
        if (version.getStatus() != ApiStatus.DRAFT) {
            throw new ApiException("Apenas versões em rascunho podem ser publicadas");
        }
        
        version.setStatus(ApiStatus.PUBLISHED);
        version = versionRepository.save(version);
        
        log.info("Versão {} publicada com sucesso", version.getVersion());
        
        return mapToResponse(version);
    }
    
    @Transactional
    public ApiVersionResponse deprecateVersion(UUID id, String message, String providerId) {
        ApiVersion version = versionRepository.findById(id)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        if (!version.getApi().getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão");
        }
        
        version.setIsDeprecated(true);
        version.setDeprecatedAt(LocalDateTime.now());
        version.setDeprecationMessage(message);
        
        version = versionRepository.save(version);
        
        return mapToResponse(version);
    }
    
    @Transactional
    public void deleteVersion(UUID id, String providerId) {
        ApiVersion version = versionRepository.findById(id)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        if (!version.getApi().getProviderId().equals(providerId)) {
            throw new ApiException("Você não tem permissão");
        }
        
        if (version.getIsDefault()) {
            throw new ApiException("Não é possível deletar a versão padrão");
        }
        
        versionRepository.delete(version);
    }
    
    private ApiVersionResponse mapToResponse(ApiVersion version) {
        return ApiVersionResponse.builder()
            .id(version.getId())
            .apiId(version.getApi().getId())
            .version(version.getVersion())
            .description(version.getDescription())
            .status(version.getStatus())
            .isDefault(version.getIsDefault())
            .isDeprecated(version.getIsDeprecated())
            .deprecatedAt(version.getDeprecatedAt())
            .deprecationMessage(version.getDeprecationMessage())
            .baseUrl(version.getBaseUrl())
            .openApiSpec(version.getOpenApiSpec())
            .endpoints(version.getEndpoints().stream()
                .map(e -> ApiVersionResponse.EndpointSummary.builder()
                    .id(e.getId())
                    .path(e.getPath())
                    .method(e.getMethod())
                    .summary(e.getSummary())
                    .requiresAuth(e.getRequiresAuth())
                    .isDeprecated(e.getIsDeprecated())
                    .build())
                .collect(Collectors.toList()))
            .createdAt(version.getCreatedAt())
            .updatedAt(version.getUpdatedAt())
            .build();
    }
}
