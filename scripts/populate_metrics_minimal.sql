-- ============================================================================
-- Script MÍNIMO para Popular Métricas - Para Testes Rápidos
-- ============================================================================
-- Cria dados mínimos apenas para visualizar o dashboard funcionando
-- Ideal quando você tem poucas APIs/subscriptions ou quer testar rapidamente
-- ============================================================================

-- Inserir métricas dos últimos 7 dias (mínimo para visualização)
INSERT INTO api_metrics (
    id, api_id, subscription_id, consumer_id, consumer_name, 
    endpoint, http_method, status_code, response_time_ms, 
    request_size_bytes, response_size_bytes, user_agent, ip_address, created_at
)
SELECT 
    gen_random_uuid(),
    a.id,
    s.id,
    s.consumer_id,
    u.name,
    (ARRAY['/api/v1/users', '/api/v1/products', '/api/v1/orders'])[floor(random() * 3 + 1)::int],
    (ARRAY['GET', 'POST'])[floor(random() * 2 + 1)::int],
    CASE WHEN random() < 0.95 THEN 200 ELSE 500 END,
    50 + (random() * 200),
    1000,
    5000,
    'TestClient/1.0',
    '192.168.1.100',
    CURRENT_DATE - (days.day || ' days')::interval + ((9 + floor(random() * 9)) || ' hours')::interval
FROM 
    apis a
    JOIN subscriptions s ON s.api_id = a.id
    JOIN users u ON u.id = s.consumer_id
    CROSS JOIN generate_series(0, 6) AS days(day)  -- Apenas 7 dias
    CROSS JOIN generate_series(1, 20) AS calls(call)  -- 20 chamadas por dia
WHERE 
    a.status = 'PUBLISHED'
    AND s.status = 'ACTIVE'
LIMIT 2000;  -- Máximo 2000 métricas

-- Agregar
INSERT INTO api_metrics_daily (
    id, api_id, metric_date, total_calls, success_calls, error_calls,
    avg_response_time, min_response_time, max_response_time,
    total_request_size, total_response_size, unique_consumers,
    created_at, updated_at
)
SELECT 
    gen_random_uuid(), api_id, DATE(created_at),
    COUNT(*), 
    COUNT(*) FILTER (WHERE status_code < 400),
    COUNT(*) FILTER (WHERE status_code >= 400),
    AVG(response_time_ms), MIN(response_time_ms), MAX(response_time_ms),
    SUM(request_size_bytes), SUM(response_size_bytes),
    COUNT(DISTINCT consumer_id),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM api_metrics
GROUP BY api_id, DATE(created_at)
ON CONFLICT (api_id, metric_date) DO UPDATE SET
    total_calls = EXCLUDED.total_calls,
    success_calls = EXCLUDED.success_calls,
    error_calls = EXCLUDED.error_calls,
    avg_response_time = EXCLUDED.avg_response_time,
    updated_at = CURRENT_TIMESTAMP;

-- Verificação rápida
SELECT 
    'Métricas criadas: ' || COUNT(*)::text as resultado
FROM api_metrics
UNION ALL
SELECT 
    'APIs com dados: ' || COUNT(DISTINCT api_id)::text
FROM api_metrics
UNION ALL
SELECT 
    'Pronto! Acesse o Dashboard do Provider → Estatísticas' as resultado;
