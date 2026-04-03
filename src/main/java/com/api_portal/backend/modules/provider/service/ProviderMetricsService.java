package com.api_portal.backend.modules.provider.service;

import com.api_portal.backend.modules.api.domain.Api;
import com.api_portal.backend.modules.api.repository.ApiRepository;
import com.api_portal.backend.modules.metrics.domain.entity.ApiMetric;
import com.api_portal.backend.modules.metrics.domain.entity.ApiMetricDaily;
import com.api_portal.backend.modules.metrics.domain.repository.ApiMetricDailyRepository;
import com.api_portal.backend.modules.metrics.domain.repository.ApiMetricRepository;
import com.api_portal.backend.modules.provider.dto.ProviderMetricsResponse;
import com.api_portal.backend.modules.subscription.domain.enums.SubscriptionStatus;
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
public class ProviderMetricsService {
    
    private final ApiRepository apiRepository;
    private final ApiMetricRepository metricRepository;
    private final ApiMetricDailyRepository dailyMetricRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    @Transactional(readOnly = true)
    public ProviderMetricsResponse getProviderMetrics(String providerId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        // Buscar APIs do provider
        List<Api> providerApis = apiRepository.findByProviderId(providerId);
        List<UUID> apiIds = providerApis.stream().map(Api::getId).collect(Collectors.toList());
        
        if (apiIds.isEmpty()) {
            return generateEmptyMetrics(days);
        }
        
        // Buscar métricas diárias
        List<ApiMetricDaily> dailyMetrics = new ArrayList<>();
        for (UUID apiId : apiIds) {
            dailyMetrics.addAll(dailyMetricRepository.findByApiIdAndMetricDateBetween(apiId, startDate, endDate));
        }
        
        // Gerar resposta
        return ProviderMetricsResponse.builder()
            .summary(generateSummary(apiIds, dailyMetrics, startDate))
            .dailyUsage(generateDailyUsage(startDate, endDate, dailyMetrics))
            .topApis(generateTopApis(providerApis, startDate))
            .topConsumers(generateTopConsumers(apiIds, startDate))
            .topEndpoints(generateTopEndpoints(apiIds, startDate))
            .build();
    }
    
    private ProviderMetricsResponse generateEmptyMetrics(int days) {
        LocalDate today = LocalDate.now();
        List<ProviderMetricsResponse.DailyUsage> dailyUsage = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            dailyUsage.add(ProviderMetricsResponse.DailyUsage.builder()
                .date(today.minusDays(i))
                .totalCalls(0L)
                .successCalls(0L)
                .errorCalls(0L)
                .averageResponseTime(0.0)
                .build());
        }
        
        return ProviderMetricsResponse.builder()
            .summary(ProviderMetricsResponse.MetricsSummary.builder()
                .totalCalls(0L)
                .totalCallsLast30Days(0L)
                .averageResponseTime(0.0)
                .errorRate(0.0)
                .activeApis(0L)
                .activeSubscriptions(0L)
                .uniqueConsumers(0L)
                .build())
            .dailyUsage(dailyUsage)
            .topApis(new ArrayList<>())
            .topConsumers(new ArrayList<>())
            .topEndpoints(new ArrayList<>())
            .build();
    }
    
    private ProviderMetricsResponse.MetricsSummary generateSummary(
            List<UUID> apiIds, List<ApiMetricDaily> dailyMetrics, LocalDate startDate) {
        
        long totalCalls = dailyMetrics.stream()
            .mapToLong(ApiMetricDaily::getTotalCalls)
            .sum();
        
        long totalErrors = dailyMetrics.stream()
            .mapToLong(ApiMetricDaily::getErrorCalls)
            .sum();
        
        double avgResponseTime = dailyMetrics.stream()
            .mapToDouble(ApiMetricDaily::getAvgResponseTime)
            .average()
            .orElse(0.0);
        
        double errorRate = totalCalls > 0 ? (totalErrors * 100.0) / totalCalls : 0.0;
        
        long activeApis = dailyMetrics.stream()
            .map(ApiMetricDaily::getApiId)
            .distinct()
            .count();
        
        long activeSubscriptions = apiIds.stream()
            .mapToLong(apiId -> subscriptionRepository.countByApiIdAndStatus(apiId, SubscriptionStatus.ACTIVE))
            .sum();
        
        long uniqueConsumers = dailyMetrics.stream()
            .mapToLong(ApiMetricDaily::getUniqueConsumers)
            .max()
            .orElse(0L);
        
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        Long totalCallsLast30Days = dailyMetricRepository.getTotalCallsAfter(thirtyDaysAgo);
        if (totalCallsLast30Days == null) totalCallsLast30Days = 0L;
        
        return ProviderMetricsResponse.MetricsSummary.builder()
            .totalCalls(totalCalls)
            .totalCallsLast30Days(totalCallsLast30Days)
            .averageResponseTime(avgResponseTime)
            .errorRate(errorRate)
            .activeApis(activeApis)
            .activeSubscriptions(activeSubscriptions)
            .uniqueConsumers(uniqueConsumers)
            .build();
    }
    
    private List<ProviderMetricsResponse.DailyUsage> generateDailyUsage(
            LocalDate startDate, LocalDate endDate, List<ApiMetricDaily> dailyMetrics) {
        
        List<ProviderMetricsResponse.DailyUsage> result = new ArrayList<>();
        
        Map<LocalDate, List<ApiMetricDaily>> metricsByDate = dailyMetrics.stream()
            .collect(Collectors.groupingBy(ApiMetricDaily::getMetricDate));
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<ApiMetricDaily> dayMetrics = metricsByDate.getOrDefault(current, new ArrayList<>());
            
            long totalCalls = dayMetrics.stream().mapToLong(ApiMetricDaily::getTotalCalls).sum();
            long successCalls = dayMetrics.stream().mapToLong(ApiMetricDaily::getSuccessCalls).sum();
            long errorCalls = dayMetrics.stream().mapToLong(ApiMetricDaily::getErrorCalls).sum();
            double avgResponseTime = dayMetrics.stream()
                .mapToDouble(ApiMetricDaily::getAvgResponseTime)
                .average()
                .orElse(0.0);
            
            result.add(ProviderMetricsResponse.DailyUsage.builder()
                .date(current)
                .totalCalls(totalCalls)
                .successCalls(successCalls)
                .errorCalls(errorCalls)
                .averageResponseTime(avgResponseTime)
                .build());
            
            current = current.plusDays(1);
        }
        
        return result;
    }
    
    private List<ProviderMetricsResponse.TopApi> generateTopApis(List<Api> providerApis, LocalDate startDate) {
        return providerApis.stream()
            .map(api -> {
                List<ApiMetricDaily> apiMetrics = dailyMetricRepository
                    .findByApiIdAndMetricDateBetween(api.getId(), startDate, LocalDate.now());
                
                long totalCalls = apiMetrics.stream().mapToLong(ApiMetricDaily::getTotalCalls).sum();
                long totalErrors = apiMetrics.stream().mapToLong(ApiMetricDaily::getErrorCalls).sum();
                double avgResponseTime = apiMetrics.stream()
                    .mapToDouble(ApiMetricDaily::getAvgResponseTime)
                    .average()
                    .orElse(0.0);
                double errorRate = totalCalls > 0 ? (totalErrors * 100.0) / totalCalls : 0.0;
                long activeSubscriptions = subscriptionRepository.countByApiIdAndStatus(
                    api.getId(), SubscriptionStatus.ACTIVE);
                
                return ProviderMetricsResponse.TopApi.builder()
                    .apiId(api.getId())
                    .apiName(api.getName())
                    .apiSlug(api.getSlug())
                    .totalCalls(totalCalls)
                    .averageResponseTime(avgResponseTime)
                    .errorRate(errorRate)
                    .activeSubscriptions(activeSubscriptions)
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getTotalCalls(), a.getTotalCalls()))
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private List<ProviderMetricsResponse.TopConsumer> generateTopConsumers(List<UUID> apiIds, LocalDate startDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        
        Map<UUID, List<ApiMetric>> metricsByConsumer = new HashMap<>();  // Alterado de String para UUID
        
        for (UUID apiId : apiIds) {
            List<ApiMetric> metrics = metricRepository.findByApiIdAndCreatedAtBetween(
                apiId, startDateTime, LocalDateTime.now());
            
            for (ApiMetric metric : metrics) {
                if (metric.getConsumerId() != null) {  // UUID não tem isEmpty()
                    metricsByConsumer.computeIfAbsent(metric.getConsumerId(), k -> new ArrayList<>())
                        .add(metric);
                }
            }
        }
        
        return metricsByConsumer.entrySet().stream()
            .map(entry -> {
                List<ApiMetric> consumerMetrics = entry.getValue();
                long totalCalls = consumerMetrics.size();
                double avgResponseTime = consumerMetrics.stream()
                    .mapToDouble(ApiMetric::getResponseTimeMs)
                    .average()
                    .orElse(0.0);
                
                ApiMetric lastMetric = consumerMetrics.stream()
                    .max(Comparator.comparing(ApiMetric::getCreatedAt))
                    .orElse(null);
                
                return ProviderMetricsResponse.TopConsumer.builder()
                    .consumerId(entry.getKey().toString())  // Converter UUID para String
                    .consumerName(lastMetric != null ? lastMetric.getConsumerName() : "")
                    .consumerEmail(entry.getKey().toString())  // Converter UUID para String
                    .totalCalls(totalCalls)
                    .averageResponseTime(avgResponseTime)
                    .lastCallAt(lastMetric != null ? lastMetric.getCreatedAt().toString() : "")
                    .build();
            })
            .sorted((a, b) -> Long.compare(b.getTotalCalls(), a.getTotalCalls()))  // Comparar totalCalls
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private List<ProviderMetricsResponse.TopEndpoint> generateTopEndpoints(List<UUID> apiIds, LocalDate startDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        
        Map<String, List<ApiMetric>> metricsByEndpoint = new HashMap<>();
        
        for (UUID apiId : apiIds) {
            List<ApiMetric> metrics = metricRepository.findByApiIdAndCreatedAtBetween(
                apiId, startDateTime, LocalDateTime.now());
            
            for (ApiMetric metric : metrics) {
                String key = metric.getHttpMethod() + ":" + metric.getEndpoint();
                metricsByEndpoint.computeIfAbsent(key, k -> new ArrayList<>()).add(metric);
            }
        }
        
        return metricsByEndpoint.entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split(":", 2);
                String method = parts[0];
                String endpoint = parts.length > 1 ? parts[1] : "";
                
                List<ApiMetric> endpointMetrics = entry.getValue();
                long totalCalls = endpointMetrics.size();
                long errorCalls = endpointMetrics.stream().filter(ApiMetric::isError).count();
                double avgResponseTime = endpointMetrics.stream()
                    .mapToDouble(ApiMetric::getResponseTimeMs)
                    .average()
                    .orElse(0.0);
                double errorRate = totalCalls > 0 ? (errorCalls * 100.0) / totalCalls : 0.0;
                
                return ProviderMetricsResponse.TopEndpoint.builder()
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
