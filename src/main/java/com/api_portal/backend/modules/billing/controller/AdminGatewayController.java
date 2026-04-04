package com.api_portal.backend.modules.billing.controller;

import com.api_portal.backend.modules.billing.dto.GatewayConfigDTO;
import com.api_portal.backend.modules.billing.dto.GatewayConfigResponseDTO;
import com.api_portal.backend.modules.billing.model.enums.GatewayType;
import com.api_portal.backend.modules.billing.service.GatewayConfigService;
import com.api_portal.backend.shared.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/billing/gateways")
@RequiredArgsConstructor
@RequiresPermission("billing.manage")
public class AdminGatewayController {

    private final GatewayConfigService gatewayConfigService;

    @GetMapping
    public ResponseEntity<List<GatewayConfigResponseDTO>> getAllGateways() {
        List<GatewayConfigResponseDTO> response = gatewayConfigService.getAllGateways()
            .stream()
            .map(GatewayConfigResponseDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<GatewayConfigResponseDTO> getActiveGateway() {
        return ResponseEntity.ok(
            GatewayConfigResponseDTO.fromEntity(gatewayConfigService.getActiveGateway())
        );
    }

    @GetMapping("/{type}")
    public ResponseEntity<GatewayConfigResponseDTO> getGatewayByType(@PathVariable GatewayType type) {
        return ResponseEntity.ok(
            GatewayConfigResponseDTO.fromEntity(gatewayConfigService.getGatewayByType(type))
        );
    }

    @PostMapping
    public ResponseEntity<GatewayConfigResponseDTO> createGateway(@RequestBody GatewayConfigDTO dto) {
        return ResponseEntity.ok(
            GatewayConfigResponseDTO.fromEntity(gatewayConfigService.createGateway(dto))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<GatewayConfigResponseDTO> updateGateway(
            @PathVariable UUID id,
            @RequestBody GatewayConfigDTO dto) {
        return ResponseEntity.ok(
            GatewayConfigResponseDTO.fromEntity(gatewayConfigService.updateGateway(id, dto))
        );
    }

    @PostMapping("/{type}/activate")
    public ResponseEntity<Void> activateGateway(@PathVariable GatewayType type) {
        gatewayConfigService.activateGateway(type);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGateway(@PathVariable UUID id) {
        gatewayConfigService.deleteGateway(id);
        return ResponseEntity.noContent().build();
    }
}
