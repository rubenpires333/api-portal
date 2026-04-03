# Sistema de Tracking de Métricas de APIs

## Visão Geral

Sistema completo para rastreamento e análise de métricas de uso de APIs, incluindo:
- Registro de cada chamada de API
- Agregação diária automática
- Análise de performance e uso
- Dashboard administrativo com dados reais

## Estrutura

### Entidades

#### ApiMetric
Registro detalhado de cada chamada individual à API:
- API ID e Subscription ID
- Consumer (ID e nome)
- Endpoint e método HTTP
- Status code e tempo de resposta
- Tamanhos de request/response
- Mensagem de erro (se houver)
- User agent e IP
- Timestamp da chamada

#### ApiMetricDaily
Métricas agregadas por dia para otimização:
- Total de chamadas (sucesso/erro)
- Tempos de resposta (médio/min/max)
- Tamanhos totais de dados
- Número de consumers únicos
- Taxa de erro calculada

### Serviços

#### ApiMetricService
- `recordApiCall()`: Registra uma chamada de forma assíncrona
- `aggregateDailyMetrics()`: Agrega métricas de um dia específico
- `cleanOldMetrics()`: Remove métricas antigas

### Scheduler

#### MetricAggregationScheduler
- Agrega métricas do dia anterior às 2h da manhã (diariamente)
- Limpa métricas antigas aos domingos às 3h (mantém 90 dias)

## Como Usar

### 1. Executar Migration

```bash
# A migration V18 cria as tabelas necessárias
./mvnw flyway:migrate
```

### 2. Popular Dados de Teste (Opcional)

```bash
# Execute o script SQL para gerar métricas de teste
psql -U seu_usuario -d seu_banco -f scripts/populate_test_metrics.sql
```

Este script irá:
- Gerar ~50.000 métricas para os últimos 30 dias
- Distribuir chamadas ao longo do dia (mais durante horário comercial)
- Simular 95% de sucesso e 5% de erros
- Criar métricas agregadas diárias

### 3. Registrar Métricas em Produção

#### Opção A: Via API REST

```java
// No seu API Gateway ou Proxy
ApiCallMetricRequest request = ApiCallMetricRequest.builder()
    .apiId(apiId)
    .subscriptionId(subscriptionId)
    .consumerId(consumerId)
    .consumerName(consumerName)
    .endpoint("/users/123")
    .httpMethod("GET")
    .statusCode(200)
    .responseTimeMs(145.5)
    .requestSizeBytes(512L)
    .responseSizeBytes(2048L)
    .userAgent(userAgent)
    .ipAddress(ipAddress)
    .build();

// POST /api/v1/metrics/record
restTemplate.postForEntity("/api/v1/metrics/record", request, Void.class);
```

#### Opção B: Injetar Serviço Diretamente

```java
@Service
public class ApiGatewayService {
    private final ApiMetricService metricService;
    
    public void handleApiCall(...) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Executar chamada à API
            Response response = executeApiCall(...);
            
            // Registrar métrica
            metricService.recordApiCall(ApiCallMetricRequest.builder()
                .apiId(apiId)
                .statusCode(response.getStatusCode())
                .responseTimeMs((double)(System.currentTimeMillis() - startTime))
                // ... outros campos
                .build());
                
        } catch (Exception e) {
            // Registrar erro
            metricService.recordApiCall(ApiCallMetricRequest.builder()
                .apiId(apiId)
                .statusCode(500)
                .errorMessage(e.getMessage())
                .responseTimeMs((double)(System.currentTimeMillis() - startTime))
                .build());
        }
    }
}
```

### 4. Agregar Métricas Manualmente (Teste)

```bash
# Agregar métricas de uma data específica
curl -X POST "http://localhost:8080/api/v1/metrics/aggregate?date=2024-01-15"

# Agregar métricas de ontem
curl -X POST "http://localhost:8080/api/v1/metrics/aggregate"
```

### 5. Visualizar no Dashboard

As métricas aparecem automaticamente no Dashboard Administrativo em:
- **Métricas de Uso e Performance**
  - Total de chamadas
  - Tempo médio de resposta
  - Taxa de erro
  - APIs ativas
  - Gráfico de tendência
  - Top 10 APIs
  - Performance por API

## Queries Úteis

### Ver métricas recentes
```sql
SELECT 
    a.name as api_name,
    m.endpoint,
    m.http_method,
    m.status_code,
    m.response_time_ms,
    m.created_at
FROM api_metrics m
JOIN apis a ON a.id = m.api_id
ORDER BY m.created_at DESC
LIMIT 100;
```

### Ver métricas agregadas
```sql
SELECT 
    a.name as api_name,
    d.metric_date,
    d.total_calls,
    d.error_calls,
    d.avg_response_time,
    ROUND((d.error_calls::numeric / d.total_calls * 100), 2) as error_rate_pct
FROM api_metrics_daily d
JOIN apis a ON a.id = d.api_id
ORDER BY d.metric_date DESC, d.total_calls DESC;
```

### Top APIs por uso
```sql
SELECT 
    a.name,
    SUM(d.total_calls) as total_calls,
    AVG(d.avg_response_time) as avg_response_time,
    SUM(d.error_calls)::numeric / SUM(d.total_calls) * 100 as error_rate
FROM api_metrics_daily d
JOIN apis a ON a.id = d.api_id
WHERE d.metric_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY a.id, a.name
ORDER BY total_calls DESC
LIMIT 10;
```

## Performance

### Índices Criados
- `idx_api_metrics_api_id`: Busca por API
- `idx_api_metrics_created_at`: Busca por data
- `idx_api_metrics_api_created`: Busca combinada (API + data)
- `idx_api_metrics_daily_api_id`: Agregados por API
- `idx_api_metrics_daily_date`: Agregados por data

### Otimizações
- Registro assíncrono (@Async) para não bloquear requests
- Agregação diária para consultas rápidas
- Índices compostos para queries comuns
- Limpeza automática de dados antigos

## Próximos Passos

1. **Integração com API Gateway Real**
   - Implementar interceptor/middleware
   - Capturar todas as chamadas automaticamente

2. **Alertas Automáticos**
   - Taxa de erro acima do limite
   - Tempo de resposta degradado
   - Picos de uso anormais

3. **Análise Avançada**
   - Padrões de uso por horário
   - Correlação entre endpoints
   - Previsão de carga

4. **Exportação de Dados**
   - Relatórios em PDF/Excel
   - Integração com ferramentas de BI
   - API para dados históricos
