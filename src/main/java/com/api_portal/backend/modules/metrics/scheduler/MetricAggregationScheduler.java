package com.api_portal.backend.modules.metrics.scheduler;

import com.api_portal.backend.modules.metrics.service.ApiMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricAggregationScheduler {
    
    private final ApiMetricService metricService;
    
    /**
     * Agrega métricas do dia anterior todos os dias às 2h da manhã
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void aggregateDailyMetrics() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("Iniciando agregação automática de métricas para: {}", yesterday);
            metricService.aggregateDailyMetrics(yesterday);
            log.info("Agregação automática concluída com sucesso");
        } catch (Exception e) {
            log.error("Erro na agregação automática de métricas: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Limpa métricas antigas (mantém últimos 90 dias) - executa semanalmente
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanOldMetrics() {
        try {
            log.info("Iniciando limpeza de métricas antigas");
            metricService.cleanOldMetrics(90);
            log.info("Limpeza de métricas concluída");
        } catch (Exception e) {
            log.error("Erro na limpeza de métricas: {}", e.getMessage(), e);
        }
    }
}
