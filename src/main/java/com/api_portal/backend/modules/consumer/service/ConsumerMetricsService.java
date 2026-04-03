package com.api_portal.backend.modules.consumer.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.consumer.dto.ConsumerMetricsResponse;
import com.api_portal.backend.modules.metrics.domain.entity.ApiMetric;
import com.api_portal.backend.modules.metrics.domain.entity.ApiMetricDaily;
import com.api_portal.backend.modules.metrics.domain.repository.ApiMetricDailyRepository;
import com.api_portal.backend.modules.metrics.domain.repository.ApiMetricRepository;
import com.api_portal.backend.modules.subscription.domain.entity.Subscription;
import com.api_portal.backend.modules.subscription.domain.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerMetricsService {
    
    private final ApiMetricRepository metricRepository;
    private final ApiMetricDailyRepository dailyMetricRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiRepository apiRepository;
    
    @Transactional(readOnly = true)
    public ConsumerMetricsResponse getConsumerMetrics(UUID consumerId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        
        // Buscar métricas do consumer
        List<ApiMetric> metrics = metricRepository.findByConsumerIdAndCreatedAtBetween(
            consumerId, startDateTime, LocalDateTime.now());
        
        if (metrics.isEmpty()) {
            return generateEmptyMetrics(days);
        }
        
        return ConsumerMetricsResponse.builder()
            .summary(generateSummary(consumerId, metrics, startDate))
            .dailyUsage(generateDailyUsage(consumerId, startDate, endDate))
            .apiUsage(generateApiUsage(consumerId, metrics))
            .endpointUsage(generateEndpointUsage(metrics))
            .build();
    }
    
    private ConsumerMetricsResponse generateEmptyMetrics(int days) {
        LocalDate today = LocalDate.now();
        List<ConsumerMetricsResponse.DailyUsage> dailyUsage = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            dailyUsage.add(ConsumerMetricsResponse.DailyUsage.builder()
                .date(today.minusDays(i))
                .totalCalls(0L)
                .successCalls(0L)
                .errorCalls(0L)
                .averageResponseTime(0.0)
                .build());
        }
        
        return ConsumerMetricsResponse.builder()
            .summary(ConsumerMetricsResponse.MetricsSummary.builder()
                .totalCalls(0L)
                .totalCallsLast30Days(0L)
                .averageResponseTime(0.0)
                .errorRate(0.0)
                .activeSubscriptions(0L)
                .totalApis(0L)
                .build())
            .dailyUsage(dailyUsage)
            .apiUsage(new ArrayList<>())
            .endpointUsage(new ArrayList<>())
            .build();
    }
    
    private ConsumerMetricsResponse.MetricsSummary generateSummary(
            UUID consumerId, List<ApiMetric> metrics, LocalDate startDate) {
        
        long totalCalls = metrics.size();
        long errorCalls = metrics.stream().filter(ApiMetric::isError).count();
        double errorRate = totalCalls > 0 ? (errorCalls * 100.0 / totalCalls) : 0.0;
        
        double avgResponseTime = metrics.stream()
            .mapToDouble(ApiMetric::getResponseTimeMs)
            .average()
            .orElse(0.0);
        
        // consumerId é UUID na entidade Subscription
        long activeSubscriptions = subscriptionRepository.countByConsumerIdAndStatus(
            consumerId, com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus.ACTIVE);
        
        long totalApis = metrics.stream()
            .map(ApiMetric::getApiId)
            .distinct()
            .count();
        
        return ConsumerMetricsResponse.MetricsSummary.builder()
            .totalCalls(totalCalls)
            .totalCallsLast30Days(totalCalls)
            .averageResponseTime(avgResponseTime)
            .errorRate(errorRate)
            .activeSubscriptions(activeSubscriptions)
            .totalApis(totalApis)
            .build();
    }
    
    private List<ConsumerMetricsResponse.DailyUsage> generateDailyUsage(
            UUID consumerId, LocalDate startDate, LocalDate endDate) {
        
        // Buscar métricas diárias das APIs que o consumer tem subscription
        // consumerId é UUID na entidade Subscription
        List<ApiMetricDaily> dailyMetrics = dailyMetricRepository
            .findByConsumerSubscriptionsAndMetricDateBetween(consumerId, startDate, endDate);
        
        // Agrupar por data e somar
        Map<LocalDate, ApiMetricDaily> metricsMap = dailyMetrics.stream()
            .collect(Collectors.toMap(
                ApiMetricDaily::getMetricDate, 
                m -> m, 
                (m1, m2) -> ApiMetricDaily.builder()
                    .metricDate(m1.getMetricDate())
                    .totalCalls(m1.getTotalCalls() + m2.getTotalCalls())
                    .successCalls(m1.getSuccessCalls() + m2.getSuccessCalls())
                    .errorCalls(m1.getErrorCalls() + m2.getErrorCalls())
                    .avgResponseTime((m1.getAvgResponseTime() + m2.getAvgResponseTime()) / 2)
                    .build()
            ));
        
        List<ConsumerMetricsResponse.DailyUsage> result = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            ApiMetricDaily daily = metricsMap.get(current);
            
            if (daily != null) {
                result.add(ConsumerMetricsResponse.DailyUsage.builder()
                    .date(current)
                    .totalCalls(daily.getTotalCalls())
                    .successCalls(daily.getSuccessCalls())
                    .errorCalls(daily.getErrorCalls())
                    .averageResponseTime(daily.getAvgResponseTime())
                    .build());
            } else {
                result.add(ConsumerMetricsResponse.DailyUsage.builder()
                    .date(current)
                    .totalCalls(0L)
                    .successCalls(0L)
                    .errorCalls(0L)
                    .averageResponseTime(0.0)
                    .build());
            }
            
            current = current.plusDays(1);
        }
        
        return result;
    }
    
    private List<ConsumerMetricsResponse.ApiUsage> generateApiUsage(
            UUID consumerId, List<ApiMetric> metrics) {
        
        Map<UUID, List<ApiMetric>> metricsByApi = metrics.stream()
            .collect(Collectors.groupingBy(ApiMetric::getApiId));
        
        // Buscar subscriptions do consumer para pegar status - consumerId é UUID
        List<Subscription> subscriptions = subscriptionRepository.findByConsumerIdAndStatus(
            consumerId, com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus.ACTIVE);
        
        Map<UUID, Subscription> subscriptionMap = subscriptions.stream()
            .collect(Collectors.toMap(s -> s.getApi().getId(), s -> s));
        
        return metricsByApi.entrySet().stream()
            .map(entry -> {
                UUID apiId = entry.getKey();
                List<ApiMetric> apiMetrics = entry.getValue();
                long totalCalls = apiMetrics.size();
                long errorCalls = apiMetrics.stream().filter(ApiMetric::isError).count();
                double errorRate = totalCalls > 0 ? (errorCalls * 100.0 / totalCalls) : 0.0;
                
                double avgResponseTime = apiMetrics.stream()
                    .mapToDouble(ApiMetric::getResponseTimeMs)
                    .average()
                    .orElse(0.0);
                
                // Buscar informações da API
                Api api = apiRepository.findById(apiId).orElse(null);
                Subscription subscription = subscriptionMap.get(apiId);
                
                String apiName = api != null ? api.getName() : "API Desconhecida";
                String apiSlug = api != null ? api.getSlug() : "unknown";
                String status = subscription != null ? subscription.getStatus().name() : "INACTIVE";
                
                return ConsumerMetricsResponse.ApiUsage.builder()
                    .apiId(apiId.toString())
                    .apiName(apiName)
                    .apiSlug(apiSlug)
                    .totalCalls(totalCalls)
                    .averageResponseTime(avgResponseTime)
                    .errorRate(errorRate)
                    .subscriptionStatus(status)
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getTotalCalls(), a.getTotalCalls()))
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private List<ConsumerMetricsResponse.EndpointUsage> generateEndpointUsage(List<ApiMetric> metrics) {
        
        Map<String, List<ApiMetric>> metricsByEndpoint = metrics.stream()
            .collect(Collectors.groupingBy(m -> m.getApiId() + "|" + m.getEndpoint() + "|" + m.getHttpMethod()));
        
        return metricsByEndpoint.entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split("\\|");
                UUID apiId = UUID.fromString(parts[0]);
                String endpoint = parts[1];
                String method = parts[2];
                
                List<ApiMetric> endpointMetrics = entry.getValue();
                long totalCalls = endpointMetrics.size();
                long errorCalls = endpointMetrics.stream().filter(ApiMetric::isError).count();
                double errorRate = totalCalls > 0 ? (errorCalls * 100.0 / totalCalls) : 0.0;
                
                double avgResponseTime = endpointMetrics.stream()
                    .mapToDouble(ApiMetric::getResponseTimeMs)
                    .average()
                    .orElse(0.0);
                
                // Buscar nome da API
                Api api = apiRepository.findById(apiId).orElse(null);
                String apiName = api != null ? api.getName() : "API Desconhecida";
                
                return ConsumerMetricsResponse.EndpointUsage.builder()
                    .apiName(apiName)
                    .endpoint(endpoint)
                    .method(method)
                    .totalCalls(totalCalls)
                    .averageResponseTime(avgResponseTime)
                    .errorRate(errorRate)
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getTotalCalls(), a.getTotalCalls()))
            .limit(10)
            .collect(Collectors.toList());
    }
}
