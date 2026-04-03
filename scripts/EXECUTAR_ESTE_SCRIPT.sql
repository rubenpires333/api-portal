-- ============================================================================
-- SCRIPT PARA POPULAR MÉTRICAS - EXECUTE NO DBEAVER/PGADMIN
-- ============================================================================
-- Banco: db_portal_api
-- Host: localhost:5432
-- User: postgres
-- Password: postgres
-- ============================================================================

-- 1. VERIFICAR PRÉ-REQUISITOS
SELECT '=== VERIFICANDO PRÉ-REQUISITOS ===' as status;

-- Verificar APIs publicadas
SELECT 
    'APIs Publicadas: ' || COUNT(*)::text as info
FROM apis 
WHERE status = 'PUBLISHED';

-- Verificar Subscriptions ativas
SELECT 
    'Subscriptions Ativas: ' || COUNT(*)::text as info
FROM subscriptions 
WHERE status = 'ACTIVE';

-- Se não houver dados, o script não inserirá nada
-- Certifique-se de ter pelo menos 1 API publicada e 1 subscription ativa

-- ============================================================================
-- 2. POPULAR MÉTRICAS DOS ÚLTIMOS 30 DIAS
-- ============================================================================

SELECT '=== INICIANDO POPULAÇÃO DE MÉTRICAS ===' as status;

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
    endpoints.endpoint,
    methods.method,
    -- 95% sucesso, 5% erro
    CASE 
        WHEN random() < 0.95 THEN (ARRAY[200, 200, 200, 201, 204])[floor(random() * 5 + 1)::int]
        ELSE (ARRAY[400, 401, 404, 429, 500])[floor(random() * 5 + 1)::int]
    END,
    -- Tempo de resposta: 30-250ms normal, 500-2000ms para erros
    CASE 
        WHEN random() < 0.95 THEN 30 + (random() * 220)
        ELSE 500 + (random() * 1500)
    END,
    floor(random() * 5000 + 100)::bigint,
    floor(random() * 50000 + 500)::bigint,
    (ARRAY['Mozilla/5.0', 'PostmanRuntime/7.32', 'curl/7.88', 'axios/1.4.0'])[floor(random() * 4 + 1)::int],
    '192.168.' || floor(random() * 255)::text || '.' || floor(random() * 255)::text,
    -- Distribuir ao longo dos últimos 30 dias, horário comercial (9h-18h)
    (CURRENT_DATE - (days.day || ' days')::interval) + 
    ((9 + floor(random() * 10)) || ' hours')::interval + 
    (floor(random() * 3600) || ' seconds')::interval
FROM 
    apis a
    JOIN subscriptions s ON s.api_id = a.id
    JOIN users u ON u.id = s.consumer_id
    CROSS JOIN (VALUES 
        ('/api/v1/users'),
        ('/api/v1/users/{id}'),
        ('/api/v1/products'),
        ('/api/v1/products/{id}'),
        ('/api/v1/orders'),
        ('/api/v1/orders/{id}'),
        ('/api/v1/payments'),
        ('/api/v1/auth/login'),
        ('/api/v1/reports'),
        ('/api/v1/dashboard')
    ) AS endpoints(endpoint)
    CROSS JOIN (VALUES ('GET'), ('POST'), ('PUT'), ('DELETE')) AS methods(method)
    CROSS JOIN generate_series(0, 29) AS days(day)
WHERE 
    a.status = 'PUBLISHED'
    AND s.status = 'ACTIVE'
    AND random() < 0.15  -- 15% de chance (ajuste para mais/menos dados)
LIMIT 50000;  -- Limite de segurança

-- Verificar quantas métricas foram criadas
SELECT 
    '=== MÉTRICAS CRIADAS ===' as status,
    COUNT(*)::text || ' métricas individuais' as resultado
FROM api_metrics;

-- ============================================================================
-- 3. AGREGAR MÉTRICAS DIÁRIAS
-- ============================================================================

SELECT '=== AGREGANDO MÉTRICAS DIÁRIAS ===' as status;

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
    gen_random_uuid(),
    api_id,
    DATE(created_at),
    COUNT(*),
    COUNT(*) FILTER (WHERE status_code >= 200 AND status_code < 300),
    COUNT(*) FILTER (WHERE status_code >= 400),
    AVG(response_time_ms),
    MIN(response_time_ms),
    MAX(response_time_ms),
    SUM(request_size_bytes),
    SUM(response_size_bytes),
    COUNT(DISTINCT consumer_id),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
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

SELECT 
    '=== AGREGAÇÃO CONCLUÍDA ===' as status,
    COUNT(*)::text || ' métricas diárias' as resultado
FROM api_metrics_daily;

-- ============================================================================
-- 4. RELATÓRIOS DE VERIFICAÇÃO
-- ============================================================================

SELECT '=== RESUMO GERAL ===' as relatorio;

SELECT 
    'Total de métricas' as metrica,
    COUNT(*)::text as valor
FROM api_metrics
UNION ALL
SELECT 
    'Métricas diárias agregadas',
    COUNT(*)::text
FROM api_metrics_daily
UNION ALL
SELECT 
    'APIs com dados',
    COUNT(DISTINCT api_id)::text
FROM api_metrics
UNION ALL
SELECT 
    'Consumers únicos',
    COUNT(DISTINCT consumer_id)::text
FROM api_metrics
UNION ALL
SELECT 
    'Período',
    MIN(DATE(created_at))::text || ' a ' || MAX(DATE(created_at))::text
FROM api_metrics;

-- Por API
SELECT '=== MÉTRICAS POR API ===' as relatorio;

SELECT 
    a.name as api,
    COUNT(m.id) as chamadas,
    ROUND(AVG(m.response_time_ms)::numeric, 1) as tempo_medio_ms,
    COUNT(*) FILTER (WHERE m.status_code >= 400) as erros,
    ROUND((COUNT(*) FILTER (WHERE m.status_code >= 400)::numeric / COUNT(*)::numeric * 100), 2) as taxa_erro_pct
FROM api_metrics m
JOIN apis a ON a.id = m.api_id
GROUP BY a.id, a.name
ORDER BY chamadas DESC;

-- Top Consumers
SELECT '=== TOP 5 CONSUMERS ===' as relatorio;

SELECT 
    consumer_name,
    COUNT(*) as chamadas,
    ROUND(AVG(response_time_ms)::numeric, 1) as tempo_medio_ms
FROM api_metrics
GROUP BY consumer_id, consumer_name
ORDER BY chamadas DESC
LIMIT 5;

-- Top Endpoints
SELECT '=== TOP 10 ENDPOINTS ===' as relatorio;

SELECT 
    http_method,
    endpoint,
    COUNT(*) as chamadas,
    ROUND(AVG(response_time_ms)::numeric, 1) as tempo_medio_ms,
    ROUND((COUNT(*) FILTER (WHERE status_code >= 400)::numeric / COUNT(*)::numeric * 100), 2) as taxa_erro_pct
FROM api_metrics
GROUP BY endpoint, http_method
ORDER BY chamadas DESC
LIMIT 10;

-- Últimos 7 dias
SELECT '=== ÚLTIMOS 7 DIAS ===' as relatorio;

SELECT 
    metric_date as data,
    SUM(total_calls) as chamadas,
    ROUND(AVG(avg_response_time)::numeric, 1) as tempo_medio_ms,
    SUM(error_calls) as erros,
    ROUND((SUM(error_calls)::numeric / SUM(total_calls)::numeric * 100), 2) as taxa_erro_pct
FROM api_metrics_daily
WHERE metric_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY metric_date
ORDER BY metric_date DESC;

SELECT '=== ✅ POPULAÇÃO CONCLUÍDA COM SUCESSO! ===' as status;
SELECT 'Agora acesse: Login como Provider → Dashboard' as proximos_passos;
