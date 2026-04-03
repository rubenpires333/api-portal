package com.api_portal.backend.modules.api.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.domain.ApiEndpoint;
import com.api_portal.backend.modules.api.domain.ApiVersion;
import com.api_portal.backend.modules.api.exception.ApiException;
import com.api_portal.backend.modules.api.repository.ApiEndpointRepository;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.api.repository.ApiVersionRepository;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiGeneratorService {
    
    private final ApiRepository apiRepository;
    private final ApiVersionRepository versionRepository;
    private final ApiEndpointRepository endpointRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Gera spec OpenAPI 3.0 para uma versão específica
     */
    @Transactional(readOnly = true)
    public String generateOpenApiSpec(UUID apiId, UUID versionId, String format, boolean forConsumer) {
        Api api = apiRepository.findById(apiId)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        ApiVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ApiException("Versão não encontrada"));
        
        if (!version.getApi().getId().equals(apiId)) {
            throw new ApiException("Versão não pertence a esta API");
        }
        
        // Se já existe spec armazenada, retornar
        if (version.getOpenApiSpec() != null && !version.getOpenApiSpec().isEmpty()) {
            return version.getOpenApiSpec();
        }
        
        // Gerar spec dinamicamente
        List<ApiEndpoint> endpoints = endpointRepository.findByVersionId(versionId);
        
        return generateSpec(api, version, endpoints, format, forConsumer);
    }
    
    /**
     * Gera spec OpenAPI para versão padrão
     */
    @Transactional(readOnly = true)
    public String generateOpenApiSpec(UUID apiId, String format, boolean forConsumer) {
        Api api = apiRepository.findById(apiId)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        ApiVersion defaultVersion = api.getVersions().stream()
            .filter(ApiVersion::getIsDefault)
            .findFirst()
            .orElseThrow(() -> new ApiException("Nenhuma versão padrão encontrada"));
        
        return generateOpenApiSpec(apiId, defaultVersion.getId(), format, forConsumer);
    }
    
    /**
     * Gera spec OpenAPI por slug (público)
     * Apenas para APIs públicas
     */
    @Transactional(readOnly = true)
    public String generateOpenApiSpecBySlug(String slug, String format, String authHeader, boolean forConsumer) {
        Api api = apiRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        // Se API não é pública e é para consumer, negar acesso
        if (!"PUBLIC".equals(api.getVisibility().name()) && forConsumer) {
            throw new ApiException("Esta API requer subscription ativa. Use o endpoint autenticado.");
        }
        
        return generateOpenApiSpec(api.getId(), format, forConsumer);
    }
    
    /**
     * Gera spec OpenAPI para consumer autenticado
     * Verifica se tem subscription ativa para APIs privadas/pagas
     */
    @Transactional(readOnly = true)
    public String generateOpenApiSpecForConsumer(String slug, String format, String consumerId) {
        Api api = apiRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException("API não encontrada"));
        
        // Se API não é pública, verificar subscription ativa
        if (!"PUBLIC".equals(api.getVisibility().name())) {
            boolean hasActiveSubscription = subscriptionRepository
                .existsByConsumerIdAndApiIdAndStatus(consumerId, api.getId(), 
                    com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus.ACTIVE);
            
            if (!hasActiveSubscription) {
                throw new ApiException("Você precisa ter uma subscription ativa para acessar esta API");
            }
        }
        
        return generateOpenApiSpec(api.getId(), format, true);
    }
    
    /**
     * Gera a spec OpenAPI completa
     */
    private String generateSpec(Api api, ApiVersion version, List<ApiEndpoint> endpoints, String format, boolean forConsumer) {
        try {
            ObjectNode spec = objectMapper.createObjectNode();
            
            // OpenAPI version
            spec.put("openapi", "3.0.0");
            
            // Info
            ObjectNode info = spec.putObject("info");
            info.put("title", api.getName());
            info.put("description", api.getDescription());
            info.put("version", version.getVersion());
            
            if (api.getTermsOfServiceUrl() != null) {
                info.put("termsOfService", api.getTermsOfServiceUrl());
            }
            
            ObjectNode contact = info.putObject("contact");
            contact.put("name", api.getProviderName());
            contact.put("email", api.getProviderEmail());
            
            // Servers
            ArrayNode servers = spec.putArray("servers");
            
            // Para consumers, mostrar APENAS o gateway
            if (forConsumer) {
                ObjectNode gatewayServer = servers.addObject();
                gatewayServer.put("url", "http://localhost:8080/gateway/api/" + api.getSlug());
                gatewayServer.put("description", "API Gateway");
            } else {
                // Para providers, mostrar URL original E gateway
                String baseUrl = version.getBaseUrl() != null ? version.getBaseUrl() : api.getBaseUrl();
                if (baseUrl != null && !baseUrl.isEmpty()) {
                    ObjectNode server1 = servers.addObject();
                    server1.put("url", baseUrl);
                    server1.put("description", "Production server");
                }
                
                ObjectNode gatewayServer = servers.addObject();
                gatewayServer.put("url", "http://localhost:8080/gateway/api/" + api.getSlug());
                gatewayServer.put("description", "API Gateway");
            }
            
            // Tags
            Set<String> allTags = new HashSet<>();
            endpoints.forEach(ep -> allTags.addAll(ep.getTags()));
            
            if (!allTags.isEmpty()) {
                ArrayNode tags = spec.putArray("tags");
                allTags.forEach(tag -> {
                    ObjectNode tagObj = tags.addObject();
                    tagObj.put("name", tag);
                });
            }
            
            // Paths
            ObjectNode paths = spec.putObject("paths");
            Map<String, ObjectNode> pathMap = new HashMap<>();
            
            for (ApiEndpoint endpoint : endpoints) {
                ObjectNode pathItem = pathMap.computeIfAbsent(
                    endpoint.getPath(),
                    k -> paths.putObject(k)
                );
                
                ObjectNode operation = pathItem.putObject(endpoint.getMethod().toLowerCase());
                operation.put("summary", endpoint.getSummary());
                
                if (endpoint.getDescription() != null && !endpoint.getDescription().isEmpty()) {
                    operation.put("description", endpoint.getDescription());
                }
                
                if (!endpoint.getTags().isEmpty()) {
                    ArrayNode opTags = operation.putArray("tags");
                    endpoint.getTags().forEach(opTags::add);
                }
                
                operation.put("operationId", generateOperationId(endpoint));
                
                // Parameters (path e query)
                ArrayNode parameters = operation.putArray("parameters");
                extractPathParameters(endpoint.getPath()).forEach(param -> {
                    ObjectNode paramObj = parameters.addObject();
                    paramObj.put("name", param);
                    paramObj.put("in", "path");
                    paramObj.put("required", true);
                    ObjectNode schema = paramObj.putObject("schema");
                    schema.put("type", "string");
                });
                
                // Request Body (POST, PUT, PATCH)
                if (Arrays.asList("POST", "PUT", "PATCH").contains(endpoint.getMethod())) {
                    ObjectNode requestBody = operation.putObject("requestBody");
                    requestBody.put("required", true);
                    ObjectNode content = requestBody.putObject("content");
                    ObjectNode jsonContent = content.putObject("application/json");
                    
                    if (endpoint.getRequestExample() != null) {
                        try {
                            Object example = objectMapper.readValue(endpoint.getRequestExample(), Object.class);
                            jsonContent.set("example", objectMapper.valueToTree(example));
                        } catch (Exception e) {
                            log.debug("Erro ao parsear request example: {}", e.getMessage());
                        }
                    }
                }
                
                // Responses
                ObjectNode responses = operation.putObject("responses");
                ObjectNode response200 = responses.putObject("200");
                response200.put("description", "Successful response");
                
                if (endpoint.getResponseExample() != null) {
                    ObjectNode respContent = response200.putObject("content");
                    ObjectNode respJson = respContent.putObject("application/json");
                    
                    try {
                        Object example = objectMapper.readValue(endpoint.getResponseExample(), Object.class);
                        respJson.set("example", objectMapper.valueToTree(example));
                    } catch (Exception e) {
                        log.debug("Erro ao parsear response example: {}", e.getMessage());
                    }
                }
                
                // Security
                if (endpoint.getRequiresAuth()) {
                    ArrayNode security = operation.putArray("security");
                    ObjectNode secReq = security.addObject();
                    secReq.putArray("ApiKeyAuth");
                }
                
                // Deprecated
                if (endpoint.getIsDeprecated()) {
                    operation.put("deprecated", true);
                }
            }
            
            // Security Schemes
            ObjectNode components = spec.putObject("components");
            ObjectNode securitySchemes = components.putObject("securitySchemes");
            ObjectNode apiKeyAuth = securitySchemes.putObject("ApiKeyAuth");
            apiKeyAuth.put("type", "apiKey");
            apiKeyAuth.put("in", "header");
            apiKeyAuth.put("name", "X-API-Key");
            apiKeyAuth.put("description", "API Key fornecida após subscription");
            
            // Converter para formato solicitado
            if ("yaml".equalsIgnoreCase(format)) {
                // TODO: Implementar conversão YAML se necessário
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
            }
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
            
        } catch (Exception e) {
            log.error("Erro ao gerar OpenAPI spec: {}", e.getMessage(), e);
            throw new ApiException("Erro ao gerar documentação: " + e.getMessage());
        }
    }
    
    /**
     * Extrai parâmetros do path
     */
    private List<String> extractPathParameters(String path) {
        List<String> params = new ArrayList<>();
        int start = 0;
        
        while ((start = path.indexOf('{', start)) != -1) {
            int end = path.indexOf('}', start);
            if (end != -1) {
                params.add(path.substring(start + 1, end));
                start = end + 1;
            } else {
                break;
            }
        }
        
        return params;
    }
    
    /**
     * Gera operationId único
     */
    private String generateOperationId(ApiEndpoint endpoint) {
        String path = endpoint.getPath()
            .replaceAll("[^a-zA-Z0-9]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
        
        return endpoint.getMethod().toLowerCase() + "_" + path;
    }
}
