-- Script simplificado para popular métricas de teste
-- Copie e cole este script no seu cliente SQL (DBeaver, pgAdmin, etc)

-- Limpar dados anteriores (opcional)
-- TRUNCATE TABLE api_metrics_daily CASCADE;
-- TRUNCATE TABLE api_metrics CASCADE;

-- Inserir métricas de teste para os últimos 30 dias
-- Este script insere aproximadamente 1000 métricas por API

INSERT INTO api_metrics (id, api_id, subscription_id, consumer_id, consumer_name, endpoint, http_method, status_code, response_time_ms, request_size_bytes, response_size_bytes, user_agent, ip_address, created_at)
SELECT 
    gen_random_uuid() as id,
    a.id as api_id,
    s.id as subscription_id,
    s.consumer_id,
    s.consumer_name,
    endpoints.endpoint,
    methods.method as http_method,
    CASE 
        WHEN random() < 0.95 THEN (ARRAY[200, 201, 204])[floor(random() * 3 + 1)::int]
        ELSE (ARRAY[400, 401, 404, 500])[floor(random() * 4 + 1)::int]
    END as status_code,
    CASE 
        WHEN random() < 0.9 THEN 50 + (random() * 200)
        ELSE 500 + (random() * 1500)
    END as response_time_ms,
    (random() * 5000 + 100)::bigint as request_size_bytes,
    (random() * 50000 + 500)::bigint as response_size_bytes,
    'TestClient/1.0' as user_agent,
    '192.168.1.' || floor(random() * 255)::text as ip_address,
    (CURRENT_DATE - (days.day || ' days')::interval + (hours.hour || ' hours')::interval + (floor(random() * 3600) || ' seconds')::interval) as created_at
FROM 
    apis a
    CROSS JOIN subscriptions s
    CROSS JOIN (VALUES ('/users'), ('/products'), ('/orders'), ('/payments'), ('/auth/login'), ('/reports')) AS endpoints(endpoint)
    CROSS JOIN (VALUES ('GET'), ('POST'), ('PUT'), ('DELETE')) AS methods(method)
    CROSS JOIN generate_series(0, 29) AS days(day)
    CROSS JOIN generate_series(8, 18) AS hours(hour)
WHERE 
    a.status = 'PUBLISHED'
    AND s.api_id = a.id
    AND s.status = 'ACTIVE'
    AND random() < 0.3; -- 30% de chance de inserir (para não gerar dados demais)

-- Verificar quantas métricas foram criadas
SELECT COUNT(*) as total_metrics FROM api_metrics;

-- Agregar métricas diárias
INSERT INTO api_metrics_daily (
    id,
    api_id,
    metric_date,
    total_calls,
    success_calls,
    error_calls,
    avg_response_time,
    min_response_time,
    max_response_time,
    total_request_size,
    total_response_size,
    unique_consumers,
    created_at,
    updated_at
)
SELECT 
    gen_random_uuid() as id,
    api_id,
    DATE(created_at) as metric_date,
    COUNT(*) as total_calls,
    COUNT(*) FILTER (WHERE status_code >= 200 AND status_code < 300) as success_calls,
    COUNT(*) FILTER (WHERE status_code >= 400) as error_calls,
    AVG(response_time_ms) as avg_response_time,
    MIN(response_time_ms) as min_response_time,
    MAX(response_time_ms) as max_response_time,
    SUM(request_size_bytes) as total_request_size,
    SUM(response_size_bytes) as total_response_size,
    COUNT(DISTINCT consumer_id) as unique_consumers,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM api_metrics
GROUP BY api_id, DATE(created_at)
ON CONFLICT (api_id, metric_date) DO UPDATE SET
    total_calls = EXCLUDED.total_calls,
    success_calls = EXCLUDED.success_calls,
    error_calls = EXCLUDED.error_calls,
    avg_response_time = EXCLUDED.avg_response_time,
    min_response_time = EXCLUDED.min_response_time,
    max_response_time = EXCLUDED.max_response_time,
    total_request_size = EXCLUDED.total_request_size,
    total_response_size = EXCLUDED.total_response_size,
    unique_consumers = EXCLUDED.unique_consumers,
    updated_at = CURRENT_TIMESTAMP;

-- Verificar resultados
SELECT 
    'Métricas individuais' as tipo,
    COUNT(*)::text as quantidade
FROM api_metrics
UNION ALL
SELECT 
    'Métricas diárias agregadas',
    COUNT(*)::text
FROM api_metrics_daily
UNION ALL
SELECT 
    'APIs com métricas',
    COUNT(DISTINCT api_id)::text
FROM api_metrics;

-- Ver resumo por API
SELECT 
    a.name as api_name,
    COUNT(m.id) as total_calls,
    ROUND(AVG(m.response_time_ms)::numeric, 2) as avg_response_time,
    COUNT(*) FILTER (WHERE m.status_code >= 400) as errors,
    ROUND((COUNT(*) FILTER (WHERE m.status_code >= 400)::numeric / COUNT(*)::numeric * 100), 2) as error_rate_pct
FROM api_metrics m
JOIN apis a ON a.id = m.api_id
GROUP BY a.id, a.name
ORDER BY total_calls DESC;

-- Ver métricas dos últimos 7 dias
SELECT 
    a.name as api_name,
    d.metric_date,
    d.total_calls,
    ROUND(d.avg_response_time::numeric, 2) as avg_response_time,
    d.error_calls,
    ROUND((d.error_calls::numeric / d.total_calls::numeric * 100), 2) as error_rate_pct
FROM api_metrics_daily d
JOIN apis a ON a.id = d.api_id
WHERE d.metric_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY d.metric_date DESC, d.total_calls DESC
LIMIT 50;
