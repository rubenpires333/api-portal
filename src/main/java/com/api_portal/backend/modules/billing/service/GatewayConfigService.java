package com.api_portal.backend.modules.billing.service;

import com.api_portal.backend.modules.billing.dto.GatewayConfigDTO;
import com.api_portal.backend.modules.billing.model.GatewayConfig;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import com.api_portal.backend.modules.billing.repository.GatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayConfigService {

    private final GatewayConfigRepository gatewayConfigRepository;

    @Transactional(readOnly = true)
    public List<GatewayConfig> getAllGateways() {
        return gatewayConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public GatewayConfig getActiveGateway() {
        return gatewayConfigRepository.findByActiveTrue()
            .orElseThrow(() -> new IllegalStateException("No active gateway configured"));
    }

    @Transactional(readOnly = true)
    public GatewayConfig getGatewayByType(GatewayType type) {
        return gatewayConfigRepository.findByGatewayType(type)
            .orElseThrow(() -> new IllegalArgumentException("Gateway not found: " + type));
    }

    @Transactional
    public GatewayConfig createGateway(GatewayConfigDTO dto) {
        log.info("Creating gateway config: {}", dto.getGatewayType());

        // Verificar se já existe
        if (gatewayConfigRepository.findByGatewayType(dto.getGatewayType()).isPresent()) {
            throw new IllegalArgumentException("Gateway already exists: " + dto.getGatewayType());
        }

        // Preparar settings
        Map<String, String> settings = Map.of(
            "api_key", dto.getApiKey(),
            "webhook_secret", dto.getWebhookSecret(),
            "test_mode", String.valueOf(dto.isTestMode())
        );

        // Usar displayName fornecido ou valor padrão baseado no tipo
        String displayName = dto.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = getDefaultDisplayName(dto.getGatewayType());
        }

        // Definir valores padrão baseados no tipo
        String supportedCurrencies;
        boolean supportsSubscriptions;
        boolean supportsRefunds;

        switch (dto.getGatewayType()) {
            case STRIPE:
                supportedCurrencies = "USD,EUR,GBP,BRL";
                supportsSubscriptions = true;
                supportsRefunds = true;
                break;
            case VINTI4:
                supportedCurrencies = "CVE";
                supportsSubscriptions = false;
                supportsRefunds = false;
                break;
            default:
                supportedCurrencies = "USD";
                supportsSubscriptions = false;
                supportsRefunds = false;
        }

        GatewayConfig config = GatewayConfig.builder()
            .gatewayType(dto.getGatewayType())
            .active(dto.isActive())
            .displayName(displayName)
            .logoUrl(dto.getLogoUrl())
            .settings(settings)
            .supportedCurrencies(supportedCurrencies)
            .supportsSubscriptions(supportsSubscriptions)
            .supportsRefunds(supportsRefunds)
            .build();

        return gatewayConfigRepository.save(config);
    }

    @Transactional
    public GatewayConfig updateGateway(UUID id, GatewayConfigDTO dto) {
        log.info("Updating gateway config: {}", id);

        GatewayConfig config = gatewayConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Gateway not found: " + id));

        // Atualizar settings
        Map<String, String> settings = Map.of(
            "api_key", dto.getApiKey(),
            "webhook_secret", dto.getWebhookSecret(),
            "test_mode", String.valueOf(dto.isTestMode())
        );

        config.setSettings(settings);
        config.setActive(dto.isActive());
        
        // Atualizar displayName se fornecido
        if (dto.getDisplayName() != null && !dto.getDisplayName().trim().isEmpty()) {
            config.setDisplayName(dto.getDisplayName());
        }
        
        // Atualizar logoUrl (pode ser null para remover)
        config.setLogoUrl(dto.getLogoUrl());

        return gatewayConfigRepository.save(config);
    }

    private String getDefaultDisplayName(GatewayType type) {
        switch (type) {
            case STRIPE:
                return "Stripe";
            case VINTI4:
                return "Vinti4";
            default:
                return type.toString();
        }
    }

    @Transactional
    public void activateGateway(GatewayType type) {
        log.info("Activating gateway: {}", type);

        // Desativar todos
        gatewayConfigRepository.findAll().forEach(config -> {
            config.setActive(false);
            gatewayConfigRepository.save(config);
        });

        // Ativar o selecionado
        GatewayConfig config = getGatewayByType(type);
        config.setActive(true);
        gatewayConfigRepository.save(config);

        log.info("Gateway {} activated successfully", type);
    }

    @Transactional
    public void deleteGateway(UUID id) {
        log.info("Deleting gateway config: {}", id);

        GatewayConfig config = gatewayConfigRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Gateway not found: " + id));

        if (config.isActive()) {
            throw new IllegalStateException("Cannot delete active gateway. Please activate another gateway first.");
        }

        gatewayConfigRepository.delete(config);
        log.info("Gateway {} deleted successfully", id);
    }
}
