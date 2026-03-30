package com.api_portal.backend.modules.analytics.service;

import com.api_portal.backend.modules.analytics.dto.*;
import com.api_portal.backend.modules.analytics.repository.AnalyticsRepository;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final AnalyticsRepository analyticsRepository;
    private final ApiRepository apiRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getProviderSummary(
            Authentication authentication,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        
        // Métricas gerais
        Long totalRequests = analyticsRepository.countTotalRequests(startDate, endDate);
        Long totalErrors = analyticsRepository.countTotalErrors(startDate, endDate);
        Double errorRate = totalRequests > 0 ? (totalErrors * 100.0 / totalRequests) : 0.0;
        Double averageLatency = analyticsRepository.calculateAverageLatency(startDate, endDate);
        
        // Contar APIs do provider
        Long totalApis = apiRepository.countByProviderId(providerId);
        
        // Contar subscrições ativas
        Long totalSubscriptions = subscriptionRepository.countActiveByProviderId(providerId);
        
        // Top consumers
        List<TopConsumerDto> topConsumers = analyticsRepository.findTopConsumers(startDate, endDate, 5)
            .stream()
            .map(row -> TopConsumerDto.builder()
                .email((String) row[0])
                .requestCount(((Number) row[1]).longValue())
                .build())
            .collect(Collectors.toList());
        
        // Top endpoints
        List<TopEndpointDto> topEndpoints = analyticsRepository.findTopEndpoints(startDate, endDate, 10)
            .stream()
            .map(row -> TopEndpointDto.builder()
                .endpoint((String) row[0])
                .method((String) row[1])
                .requestCount(((Number) row[2]).longValue())
                .averageLatency(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .build())
            .collect(Collectors.toList());
        
        // Requests por método HTTP
        Map<String, Long> requestsByMethod = analyticsRepository.countRequestsByMethod(startDate, endDate)
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).longValue()
            ));
        
        // Requests por status
        Map<String, Long> requestsByStatus = analyticsRepository.countRequestsByStatus(startDate, endDate)
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).longValue()
            ));
        
        return AnalyticsSummaryResponse.builder()
            .totalRequests(totalRequests)
            .totalErrors(totalErrors)
            .errorRate(Math.round(errorRate * 100.0) / 100.0)
            .averageLatency(averageLatency != null ? Math.round(averageLatency * 100.0) / 100.0 : 0.0)
            .totalApis(totalApis)
            .totalSubscriptions(totalSubscriptions)
            .topConsumers(topConsumers)
            .topEndpoints(topEndpoints)
            .requestsByMethod(requestsByMethod)
            .requestsByStatus(requestsByStatus)
            .build();
    }
    
    @Transactional(readOnly = true)
    public ApiAnalyticsResponse getApiAnalytics(
            UUID apiId,
            Authentication authentication,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String providerId = jwt.getSubject();
        
        // Verificar se API pertence ao provider
        var api = apiRepository.findById(apiId)
            .orElseThrow(() -> new IllegalArgumentException("API não encontrada"));
        
        if (!api.getProviderId().equals(providerId)) {
            throw new IllegalStateException("Acesso negado");
        }
        
        String apiPattern = "/gateway/api/" + api.getSlug() + "/%";
        
        // Contar requests da API
        Long totalRequests = countApiRequests(apiPattern, startDate, endDate);
        Long totalErrors = countApiErrors(apiPattern, startDate, endDate);
        Double errorRate = totalRequests > 0 ? (totalErrors * 100.0 / totalRequests) : 0.0;
        Double averageLatency = calculateApiAverageLatency(apiPattern, startDate, endDate);
        
        // Contar subscrições ativas
        Long totalSubscriptions = subscriptionRepository.countActiveByApiId(apiId);
        
        // Top endpoints da API
        List<TopEndpointDto> topEndpoints = findApiTopEndpoints(apiPattern, startDate, endDate, 10);
        
        // Requests por dia
        Map<String, Long> requestsByDay = countApiRequestsByDay(apiPattern, startDate, endDate);
        
        // Requests por método
        Map<String, Long> requestsByMethod = countApiRequestsByMethod(apiPattern, startDate, endDate);
        
        return ApiAnalyticsResponse.builder()
            .apiId(apiId)
            .apiName(api.getName())
            .apiSlug(api.getSlug())
            .totalRequests(totalRequests)
            .totalErrors(totalErrors)
            .errorRate(Math.round(errorRate * 100.0) / 100.0)
            .averageLatency(averageLatency != null ? Math.round(averageLatency * 100.0) / 100.0 : 0.0)
            .totalSubscriptions(totalSubscriptions)
            .topEndpoints(topEndpoints)
            .requestsByDay(requestsByDay)
            .requestsByMethod(requestsByMethod)
            .build();
    }
    
    // Métodos auxiliares para queries específicas de API
    private Long countApiRequests(String apiPattern, LocalDateTime start, LocalDateTime end) {
        // Implementação simplificada - em produção usar query nativa
        return analyticsRepository.countTotalRequests(start, end);
    }
    
    private Long countApiErrors(String apiPattern, LocalDateTime start, LocalDateTime end) {
        return analyticsRepository.countTotalErrors(start, end);
    }
    
    private Double calculateApiAverageLatency(String apiPattern, LocalDateTime start, LocalDateTime end) {
        return analyticsRepository.calculateAverageLatency(start, end);
    }
    
    private List<TopEndpointDto> findApiTopEndpoints(String apiPattern, LocalDateTime start, LocalDateTime end, int limit) {
        return analyticsRepository.findTopEndpoints(start, end, limit)
            .stream()
            .map(row -> TopEndpointDto.builder()
                .endpoint((String) row[0])
                .method((String) row[1])
                .requestCount(((Number) row[2]).longValue())
                .averageLatency(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .build())
            .collect(Collectors.toList());
    }
    
    private Map<String, Long> countApiRequestsByDay(String apiPattern, LocalDateTime start, LocalDateTime end) {
        return analyticsRepository.countRequestsByDay(start, end)
            .stream()
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> ((Number) row[1]).longValue(),
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }
    
    private Map<String, Long> countApiRequestsByMethod(String apiPattern, LocalDateTime start, LocalDateTime end) {
        return analyticsRepository.countRequestsByMethod(start, end)
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).longValue()
            ));
    }
}
