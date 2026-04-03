package com.api_portal.backend.modules.settings.service;

import com.api_portal.backend.modules.settings.domain.PlatformSetting;
import com.api_portal.backend.modules.settings.dto.PlatformSettingDTO;
import com.api_portal.backend.modules.settings.dto.UpdatePlatformSettingRequest;
import com.api_portal.backend.modules.settings.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformSettingService {
    
    private final PlatformSettingRepository repository;
    
    /**
     * Obter todas as configurações (apenas para SUPER_ADMIN)
     */
    public List<PlatformSettingDTO> getAllSettings() {
        return repository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obter configurações por categoria
     */
    public List<PlatformSettingDTO> getSettingsByCategory(String category) {
        return repository.findByCategory(category).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obter configurações públicas (sem autenticação)
     */
    public List<PlatformSettingDTO> getPublicSettings() {
        return repository.findByIsPublicTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obter valor de uma configuração (com cache)
     */
    @Cacheable(value = "platformSettings", key = "#key")
    public String getSetting(String key, String defaultValue) {
        return repository.findByKey(key)
            .map(PlatformSetting::getValue)
            .orElse(defaultValue);
    }
    
    /**
     * Obter valor booleano
     */
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Obter valor inteiro
     */
    public int getIntSetting(String key, int defaultValue) {
        try {
            String value = getSetting(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Atualizar configuração
     */
    @Transactional
    @CacheEvict(value = "platformSettings", key = "#request.key")
    public PlatformSettingDTO updateSetting(UpdatePlatformSettingRequest request, String userId) {
        PlatformSetting setting = repository.findByKey(request.getKey())
            .orElseThrow(() -> new RuntimeException("Configuração não encontrada: " + request.getKey()));
        
        setting.setValue(request.getValue());
        setting.setUpdatedBy(userId);
        
        PlatformSetting saved = repository.save(setting);
        log.info("Configuração atualizada: {} por usuário {}", request.getKey(), userId);
        
        return toDTO(saved);
    }
    
    /**
     * Criar nova configuração
     */
    @Transactional
    @CacheEvict(value = "platformSettings", allEntries = true)
    public PlatformSettingDTO createSetting(PlatformSetting setting, String userId) {
        setting.setUpdatedBy(userId);
        PlatformSetting saved = repository.save(setting);
        log.info("Configuração criada: {} por usuário {}", setting.getKey(), userId);
        return toDTO(saved);
    }
    
    /**
     * Deletar configuração
     */
    @Transactional
    @CacheEvict(value = "platformSettings", allEntries = true)
    public void deleteSetting(String key) {
        repository.findByKey(key).ifPresent(setting -> {
            repository.delete(setting);
            log.info("Configuração deletada: {}", key);
        });
    }
    
    /**
     * Converter entidade para DTO
     */
    private PlatformSettingDTO toDTO(PlatformSetting setting) {
        return PlatformSettingDTO.builder()
            .id(setting.getId())
            .key(setting.getKey())
            .value(setting.getIsSecret() ? "********" : setting.getValue()) // Ocultar valores secretos
            .type(setting.getType())
            .category(setting.getCategory())
            .description(setting.getDescription())
            .isSecret(setting.getIsSecret())
            .isPublic(setting.getIsPublic())
            .updatedBy(setting.getUpdatedBy())
            .createdAt(setting.getCreatedAt())
            .updatedAt(setting.getUpdatedAt())
            .build();
    }
}
