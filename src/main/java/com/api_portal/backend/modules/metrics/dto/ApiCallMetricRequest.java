package com.api_portal.backend.modules.metrics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallMetricRequest {
    
    @NotNull(message = "API ID é obrigatório")
    private UUID apiId;
    
    private UUID subscriptionId;
    
    private String consumerId;
    
    private String consumerName;
    
    @NotBlank(message = "Endpoint é obrigatório")
    private String endpoint;
    
    @NotBlank(message = "Método HTTP é obrigatório")
    private String httpMethod;
    
    @NotNull(message = "Status code é obrigatório")
    private Integer statusCode;
    
    @NotNull(message = "Tempo de resposta é obrigatório")
    private Double responseTimeMs;
    
    private Long requestSizeBytes;
    
    private Long responseSizeBytes;
    
    private String errorMessage;
    
    private String userAgent;
    
    private String ipAddress;
}
