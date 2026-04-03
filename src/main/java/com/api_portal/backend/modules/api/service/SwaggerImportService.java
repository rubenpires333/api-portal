package com.api_portal.backend.modules.api.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.ApiCategory;
import com.api_portal.backend.modules.api.domain.ApiEndpoint;
import com.api_portal.backend.modules.api.domain.ApiVersion;
import com.api_portal.backend.modules.api.domain.enums.ApiStatus;
import com.api_portal.backend.modules.api.domain.enums.AuthType;
import com.api_portal.backend.modules.api.dto.swagger.EndpointPreview;
import com.api_portal.backend.modules.api.dto.swagger.ImportSwaggerRequest;
import com.api_portal.backend.modules.api.dto.swagger.ImportSwaggerResponse;
import com.api_portal.backend.modules.api.dto.swagger.SwaggerPreviewResponse;
import com.api_portal.backend.modules.api.repository.ApiCategoryRepository;
import com.api_portal.backend.modules.api.repository.ApiEndpointRepository;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.api.repository.ApiVersionRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwaggerImportService {
    
    private final ApiRepository apiRepository;
    private final ApiVersionRepository versionRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiCategoryRepository categoryRepository;
    
    private static final int MAX_ENDPOINTS = 500;
    
    /**
     * Parse Swagger/OpenAPI e gera preview
     */
    public SwaggerPreviewResponse parseSwagger(String content) {
        try {
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            SwaggerParseResult result = parser.readContents(content, null, null);
            
            if (result.getOpenAPI() == null) {
                throw new RuntimeException("Falha ao parsear Swagger: " + 
                    (result.getMessages() != null ? String.join(", ", result.getMessages()) : "Formato inválido"));
            }
            
            OpenAPI openAPI = result.getOpenAPI();
            SwaggerPreviewResponse preview = extractPreview(openAPI, content);
            
            // Adicionar warnings se houver
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                preview.getWarnings().addAll(result.getMessages());
            }
            
            return preview;
            
        } catch (Exception e) {
            log.error("Erro ao parsear Swagger: {}", e.getMessage());
            throw new RuntimeException("Erro ao processar arquivo Swagger: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extrai preview do OpenAPI
     */
    private SwaggerPreviewResponse extractPreview(OpenAPI openAPI, String originalSpec) {
        Info info = openAPI.getInfo();
        List<Server> servers = openAPI.getServers();
        
        SwaggerPreviewResponse preview = SwaggerPreviewResponse.builder()
            .title(info.getTitle())
            .description(info.getDescription())
            .version(info.getVersion())
            .baseUrl(extractBaseUrl(servers))
            .termsOfServiceUrl(info.getTermsOfService())
            .documentationUrl(openAPI.getExternalDocs() != null ? openAPI.getExternalDocs().getUrl() : null)
            .tags(extractTags(openAPI))
            .openApiVersion(openAPI.getOpenapi())
            .originalSpec(originalSpec)
            .build();
        
        // Extrair endpoints
        List<EndpointPreview> endpoints = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                
                endpoints.addAll(extractEndpoints(path, pathItem));
            }
        }
        
        // Limitar endpoints
        if (endpoints.size() > MAX_ENDPOINTS) {
            preview.getWarnings().add(
                String.format("API possui %d endpoints. Apenas os primeiros %d serão importados.", 
                    endpoints.size(), MAX_ENDPOINTS)
            );
            endpoints = endpoints.subList(0, MAX_ENDPOINTS);
        }
        
        preview.setEndpoints(endpoints);
        preview.setTotalEndpoints(endpoints.size());
        
        return preview;
    }
    
    /**
     * Extrai base URL dos servers
     */
    private String extractBaseUrl(List<Server> servers) {
        if (servers != null && !servers.isEmpty()) {
            return servers.get(0).getUrl();
        }
        return "";
    }
    
    /**
     * Extrai tags globais
     */
    private List<String> extractTags(OpenAPI openAPI) {
        if (openAPI.getTags() != null) {
            return openAPI.getTags().stream()
                .map(io.swagger.v3.oas.models.tags.Tag::getName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    /**
     * Extrai endpoints de um PathItem
     */
    private List<EndpointPreview> extractEndpoints(String path, PathItem pathItem) {
        List<EndpointPreview> endpoints = new ArrayList<>();
        
        if (pathItem.getGet() != null) {
            endpoints.add(createEndpointPreview(path, "GET", pathItem.getGet()));
        }
        if (pathItem.getPost() != null) {
            endpoints.add(createEndpointPreview(path, "POST", pathItem.getPost()));
        }
        if (pathItem.getPut() != null) {
            endpoints.add(createEndpointPreview(path, "PUT", pathItem.getPut()));
        }
        if (pathItem.getDelete() != null) {
            endpoints.add(createEndpointPreview(path, "DELETE", pathItem.getDelete()));
        }
        if (pathItem.getPatch() != null) {
            endpoints.add(createEndpointPreview(path, "PATCH", pathItem.getPatch()));
        }
        if (pathItem.getOptions() != null) {
            endpoints.add(createEndpointPreview(path, "OPTIONS", pathItem.getOptions()));
        }
        if (pathItem.getHead() != null) {
            endpoints.add(createEndpointPreview(path, "HEAD", pathItem.getHead()));
        }
        
        return endpoints;
    }
    
    /**
     * Cria preview de um endpoint
     */
    private EndpointPreview createEndpointPreview(String path, String method, Operation operation) {
        String id = UUID.randomUUID().toString();
        
        return EndpointPreview.builder()
            .id(id)
            .path(path)
            .method(method.toUpperCase())
            .summary(operation.getSummary() != null ? operation.getSummary() : "")
            .description(operation.getDescription() != null ? operation.getDescription() : "")
            .tags(operation.getTags() != null ? operation.getTags() : new ArrayList<>())
            .requiresAuth(operation.getSecurity() != null && !operation.getSecurity().isEmpty())
            .isDeprecated(operation.getDeprecated() != null && operation.getDeprecated())
            .requestExample(extractRequestExample(operation))
            .responseExample(extractResponseExample(operation))
            .selected(true)
            .build();
    }
    
    /**
     * Extrai exemplo de request
     */
    private String extractRequestExample(Operation operation) {
        try {
            if (operation.getRequestBody() != null && 
                operation.getRequestBody().getContent() != null) {
                
                Content content = operation.getRequestBody().getContent();
                MediaType mediaType = content.get("application/json");
                
                if (mediaType != null && mediaType.getExample() != null) {
                    return mediaType.getExample().toString();
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair request example: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extrai exemplo de response
     */
    private String extractResponseExample(Operation operation) {
        try {
            if (operation.getResponses() != null) {
                ApiResponse response200 = operation.getResponses().get("200");
                if (response200 == null) {
                    response200 = operation.getResponses().get("201");
                }
                
                if (response200 != null && response200.getContent() != null) {
                    Content content = response200.getContent();
                    MediaType mediaType = content.get("application/json");
                    
                    if (mediaType != null && mediaType.getExample() != null) {
                        return mediaType.getExample().toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair response example: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Importa API do Swagger
     */
    @Transactional
    public ImportSwaggerResponse importSwagger(
            ImportSwaggerRequest request,
            String providerId,
            String providerName,
            String providerEmail) {
        
        // NÃO fazer parse novamente - usar os dados que vieram do request
        // O preview já foi feito no frontend, apenas criar as entidades
        
        // Buscar categoria
        ApiCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }
        
        // Criar API
        Api api = Api.builder()
            .name(request.getTitle())
            .slug(generateSlug(request.getTitle()))
            .description(request.getDescription())
            .shortDescription(truncate(request.getDescription(), 500))
            .category(category)
            .status(ApiStatus.DRAFT)
            .visibility(request.getVisibility())
            .providerId(providerId)
            .providerName(providerName)
            .providerEmail(providerEmail)
            .baseUrl(request.getBaseUrl())
            .documentationUrl(request.getDocumentationUrl())
            .termsOfServiceUrl(request.getTermsOfServiceUrl())
            .authType(AuthType.API_KEY)
            .tags(request.getTags())
            .requiresApproval(false)
            .isActive(true)
            .build();
        
        api = apiRepository.save(api);
        log.info("API criada via import: {} ({})", api.getName(), api.getId());
        
        // Criar versão
        ApiVersion version = ApiVersion.builder()
            .api(api)
            .version(request.getVersion())
            .description(request.getDescription())
            .status(ApiStatus.DRAFT)
            .isDefault(true)
            .isDeprecated(false)
            .baseUrl(request.getBaseUrl())
            .openApiSpec(request.getOriginalSpec())
            .build();
        
        version = versionRepository.save(version);
        log.info("Versão criada: {} para API {}", version.getVersion(), api.getId());
        
        // Criar endpoints selecionados
        int endpointsCreated = 0;
        
        for (EndpointPreview ep : request.getSelectedEndpoints()) {
            ApiEndpoint endpoint = ApiEndpoint.builder()
                .version(version)
                .path(ep.getPath())
                .method(ep.getMethod())
                .summary(ep.getSummary())
                .description(ep.getDescription())
                .tags(ep.getTags())
                .requiresAuth(ep.getRequiresAuth())
                .isDeprecated(ep.getIsDeprecated())
                .requestExample(ep.getRequestExample())
                .responseExample(ep.getResponseExample())
                .build();
            
            endpointRepository.save(endpoint);
            endpointsCreated++;
        }
        
        log.info("{} endpoints criados para API {}", endpointsCreated, api.getId());
        
        return ImportSwaggerResponse.builder()
            .apiId(api.getId())
            .apiName(api.getName())
            .versionId(version.getId())
            .version(version.getVersion())
            .endpointsCreated(endpointsCreated)
            .build();
    }
    
    /**
     * Gera slug a partir do nome
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
    
    /**
     * Trunca string
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
