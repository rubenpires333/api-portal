package com.api_portal.backend.modules.metrics.service;

import com.api_portal.backend.modules.metrics.domain.entity.ApiMetric;
import com.api_portal.backend.modules.metrics.domain.entity.ApiMetricDaily;
import com.api_portal.backend.modules.metrics.domain.repository.ApiMetricDailyRepository;
import com.api_portal.backend.modules.metrics.domain.repository.ApiMetricRepository;
import com.api_portal.backend.modules.metrics.dto.ApiCallMetricRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiMetricService {
    
    private final ApiMetricRepository metricRepository;
    private final ApiMetricDailyRepository dailyRepository;
    
    /**
     * Registra uma chamada de API de forma assíncrona
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordApiCall(ApiCallMetricRequest request) {
        try {
            ApiMetric metric = ApiMetric.builder()
                .apiId(request.getApiId())
                .subscriptionId(request.getSubscriptionId())
                .consumerId(request.getConsumerId())
                .consumerName(request.getConsumerName())
                .endpoint(request.getEndpoint())
                .httpMethod(request.getHttpMethod())
                .statusCode(request.getStatusCode())
                .responseTimeMs(request.getResponseTimeMs())
                .requestSizeBytes(request.getRequestSizeBytes())
                .responseSizeBytes(request.getResponseSizeBytes())
                .errorMessage(request.getErrorMessage())
                .userAgent(request.getUserAgent())
                .ipAddress(request.getIpAddress())
                .build();
            
            metricRepository.save(metric);
            log.debug("Métrica registrada: API={}, Status={}, ResponseTime={}ms", 
                request.getApiId(), request.getStatusCode(), request.getResponseTimeMs());
        } catch (Exception e) {
            log.error("Erro ao registrar métrica de API: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Agrega métricas diárias (deve ser executado por um job agendado)
     */
    @Transactional
    public void aggregateDailyMetrics(LocalDate date) {
        log.info("Iniciando agregação de métricas para data: {}", date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        
        List<ApiMetric> metrics = metricRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        // Agrupar por API
        Set<UUID> apiIds = new HashSet<>();
        metrics.forEach(m -> apiIds.add(m.getApiId()));
        
        for (UUID apiId : apiIds) {
            aggregateForApi(apiId, date, metrics);
        }
        
        log.info("Agregação concluída: {} APIs processadas", apiIds.size());
    }
    
    private void aggregateForApi(UUID apiId, LocalDate date, List<ApiMetric> allMetrics) {
        List<ApiMetric> apiMetrics = allMetrics.stream()
            .filter(m -> m.getApiId().equals(apiId))
            .toList();
        
        if (apiMetrics.isEmpty()) return;
        
        long totalCalls = apiMetrics.size();
        long successCalls = apiMetrics.stream().filter(ApiMetric::isSuccess).count();
        long errorCalls = apiMetrics.stream().filter(ApiMetric::isError).count();
        
        double avgResponseTime = apiMetrics.stream()
            .mapToDouble(ApiMetric::getResponseTimeMs)
            .average()
            .orElse(0.0);
        
        double minResponseTime = apiMetrics.stream()
            .mapToDouble(ApiMetric::getResponseTimeMs)
            .min()
            .orElse(0.0);
        
        double maxResponseTime = apiMetrics.stream()
            .mapToDouble(ApiMetric::getResponseTimeMs)
            .max()
            .orElse(0.0);
        
        long totalRequestSize = apiMetrics.stream()
            .mapToLong(m -> m.getRequestSizeBytes() != null ? m.getRequestSizeBytes() : 0L)
            .sum();
        
        long totalResponseSize = apiMetrics.stream()
            .mapToLong(m -> m.getResponseSizeBytes() != null ? m.getResponseSizeBytes() : 0L)
            .sum();
        
        int uniqueConsumers = (int) apiMetrics.stream()
            .map(ApiMetric::getConsumerId)
            .filter(id -> id != null)  // UUID não tem isEmpty(), apenas verificar null
            .distinct()
            .count();
        
        // Buscar ou criar registro diário
        ApiMetricDaily daily = dailyRepository.findByApiIdAndMetricDate(apiId, date)
            .orElse(ApiMetricDaily.builder()
                .apiId(apiId)
                .metricDate(date)
                .build());
        
        daily.setTotalCalls(totalCalls);
        daily.setSuccessCalls(successCalls);
        daily.setErrorCalls(errorCalls);
        daily.setAvgResponseTime(avgResponseTime);
        daily.setMinResponseTime(minResponseTime);
        daily.setMaxResponseTime(maxResponseTime);
        daily.setTotalRequestSize(totalRequestSize);
        daily.setTotalResponseSize(totalResponseSize);
        daily.setUniqueConsumers(uniqueConsumers);
        
        dailyRepository.save(daily);
        
        log.debug("Métricas agregadas para API {}: {} chamadas, {} erros", 
            apiId, totalCalls, errorCalls);
    }
    
    /**
     * Limpa métricas antigas (manter apenas últimos N dias)
     */
    @Transactional
    public void cleanOldMetrics(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        log.info("Limpando métricas anteriores a: {}", cutoffDate);
        
        // Implementar limpeza se necessário
        // metricRepository.deleteByCreatedAtBefore(cutoffDate);
    }
}
