package com.api_portal.backend.modules.auth.service;

import com.api_portal.backend.modules.auth.exception.AuthException;
import com.api_portal.backend.modules.auth.model.ApiKey;
import com.api_portal.backend.modules.auth.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();
    
    @Transactional
    public ApiKey createApiKey(String userId, String name, String description, Integer expiresInDays) {
        String keyValue = generateApiKey();
        
        ApiKey apiKey = ApiKey.builder()
            .keyValue(keyValue)
            .userId(userId)
            .name(name)
            .description(description)
            .active(true)
            .expiresAt(expiresInDays != null ? LocalDateTime.now().plusDays(expiresInDays) : null)
            .build();
        
        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("API Key criada para utilizador {}: {}", userId, name);
        
        return saved;
    }
    
    @Transactional
    public boolean validateApiKey(String keyValue) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyValue(keyValue);
        
        if (apiKeyOpt.isEmpty()) {
            return false;
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // Verificar se está ativa
        if (!apiKey.getActive()) {
            log.warn("API Key inativa: {}", keyValue);
            return false;
        }
        
        // Verificar se expirou
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("API Key expirada: {}", keyValue);
            return false;
        }
        
        // Atualizar último uso
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        
        return true;
    }
    
    public Optional<ApiKey> getApiKeyByValue(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue);
    }
    
    public List<ApiKey> getUserApiKeys(String userId) {
        return apiKeyRepository.findByUserId(userId);
    }
    
    public List<ApiKey> getUserActiveApiKeys(String userId) {
        return apiKeyRepository.findByUserIdAndActiveTrue(userId);
    }
    
    @Transactional
    public void revokeApiKey(Long apiKeyId, String userId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new AuthException("API Key não encontrada"));
        
        if (!apiKey.getUserId().equals(userId)) {
            throw new AuthException("Não autorizado a revogar esta API Key");
        }
        
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
        
        log.info("API Key revogada: {} por utilizador {}", apiKeyId, userId);
    }
    
    @Transactional
    public void deleteApiKey(Long apiKeyId, String userId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new AuthException("API Key não encontrada"));
        
        if (!apiKey.getUserId().equals(userId)) {
            throw new AuthException("Não autorizado a eliminar esta API Key");
        }
        
        apiKeyRepository.delete(apiKey);
        log.info("API Key eliminada: {} por utilizador {}", apiKeyId, userId);
    }
    
    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String key = base64Encoder.encodeToString(randomBytes);
        
        // Garantir unicidade
        while (apiKeyRepository.existsByKeyValue(key)) {
            secureRandom.nextBytes(randomBytes);
            key = base64Encoder.encodeToString(randomBytes);
        }
        
        return key;
    }
}
