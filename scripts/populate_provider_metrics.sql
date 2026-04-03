-- ============================================================================
-- Script para Popular Métricas do Provider Dashboard
-- ============================================================================
-- Este script cria métricas realistas para os últimos 30 dias
-- Inclui variação de uso, horários de pico, e padrões realistas de erro
-- ============================================================================

-- Limpar dados anteriores (CUIDADO: descomente apenas se quiser resetar)
-- TRUNCATE TABLE api_metrics_daily CASCADE;
-- TRUNCATE TABLE api_metrics CASCADE;

DO $$
DECLARE
    v_api_id UUID;
    v_subscription_id UUID;
    v_consumer_id UUID;
    v_consumer_name TEXT;
    v_date DATE;
    v_hour INT;
    v_minute INT;
    v_endpoint TEXT;
    v_method TEXT;
    v_status_code INT;
    v_response_time NUMERIC;
    v_is_error BOOLEAN;
    v_day_of_week INT;
    v_is_business_hours BOOLEAN;
    v_call_multiplier NUMERIC;
    v_metrics_count INT := 0;
BEGIN
    RAISE NOTICE '=== Iniciando população de métricas do Provider ===';
    
    -- Loop por cada API publicada
    FOR v_api_id IN 
        SELECT id FROM apis WHERE status = 'PUBLISHED' LIMIT 10
    LOOP
        RAISE NOTICE 'Processando API: %', v_api_id;
        
        -- Loop por cada subscription ativa desta API
        FOR v_subscription_id, v_consumer_id, v_consumer_name IN 
            SELECT s.id, s.consumer_id, u.name
            FROM subscriptions s
            JOIN users u ON u.id = s.consumer_id
            WHERE s.api_id = v_api_id 
            AND s.status = 'ACTIVE'
            LIMIT 5
        LOOP
            -- Loop pelos últimos 30 dias
            FOR v_date IN 
                SELECT CURRENT_DATE - i 
                FROM generate_series(0, 29) AS i
            LOOP
                v_day_of_week := EXTRACT(DOW FROM v_date);
                
                -- Determinar multiplicador baseado no dia da semana
                -- Fim de semana tem menos tráfego
                v_call_multiplier := CASE 
                    WHEN v_day_of_week IN (0, 6) THEN 0.3  -- Domingo/Sábado
                    WHEN v_day_of_week = 5 THEN 0.7        -- Sexta
                    ELSE 1.0                                -- Seg-Qui
                END;
                
                -- Loop por horas do dia (6h às 23h)
                FOR v_hour IN 6..23 LOOP
                    v_is_business_hours := (v_hour >= 9 AND v_hour <= 18);
                    
                    -- Ajustar multiplicador por horário
                    IF v_is_business_hours THEN
                        v_call_multiplier := v_call_multiplier * 1.5;
                    ELSE
                        v_call_multiplier := v_call_multiplier * 0.5;
                    END IF;
                    
                    -- Gerar chamadas para esta hora (0-10 chamadas dependendo do multiplicador)
                    FOR v_minute IN 1..(floor(random() * 10 * v_call_multiplier) + 1)::INT LOOP
                        
                        -- Selecionar endpoint aleatório
                        v_endpoint := (ARRAY[
                            '/api/v1/users',
                            '/api/v1/users/{id}',
                            '/api/v1/products',
                            '/api/v1/products/{id}',
                            '/api/v1/orders',
                            '/api/v1/orders/{id}',
                            '/api/v1/payments',
                            '/api/v1/auth/login',
                            '/api/v1/reports',
                            '/api/v1/dashboard'
                        ])[floor(random() * 10 + 1)::INT];
                        
                        -- Selecionar método HTTP baseado no endpoint
                        v_method := CASE 
                            WHEN v_endpoint LIKE '%/auth/%' THEN 'POST'
                            WHEN v_endpoint LIKE '%{id}%' THEN 
                                (ARRAY['GET', 'PUT', 'DELETE'])[floor(random() * 3 + 1)::INT]
                            ELSE 
                                (ARRAY['GET', 'POST'])[floor(random() * 2 + 1)::INT]
                        END;
                        
                        -- Determinar se é erro (5% de chance base)
                        v_is_error := random() < 0.05;
                        
                        -- Aumentar chance de erro fora do horário comercial
                        IF NOT v_is_business_hours THEN
                            v_is_error := v_is_error OR (random() < 0.03);
                        END IF;
                        
                        -- Status code
                        IF v_is_error THEN
                            v_status_code := (ARRAY[400, 401, 403, 404, 429, 500, 502, 503])[floor(random() * 8 + 1)::INT];
                            v_response_time := 100 + (random() * 2000); -- Erros podem ser mais lentos
                        ELSE
                            v_status_code := (ARRAY[200, 200, 200, 201, 204])[floor(random() * 5 + 1)::INT];
                            v_response_time := CASE 
                                WHEN v_method = 'GET' THEN 30 + (random() * 150)
                                WHEN v_method = 'POST' THEN 50 + (random() * 200)
                                WHEN v_method = 'PUT' THEN 60 + (random() * 250)
                                ELSE 40 + (random() * 180)
                            END;
                        END IF;
                        
                        -- Inserir métrica
                        INSERT INTO api_metrics (
                            id,
                            api_id,
                            subscription_id,
                            consumer_id,
                            consumer_name,
                            endpoint,
                            http_method,
                            status_code,
                            response_time_ms,
                            request_size_bytes,
                            response_size_bytes,
                            user_agent,
                            ip_address,
                            created_at
                        ) VALUES (
                            gen_random_uuid(),
                            v_api_id,
                            v_subscription_id,
                            v_consumer_id,
                            v_consumer_name,
                            v_endpoint,
                            v_method,
                            v_status_code,
                            v_response_time,
                            floor(random() * 5000)::bigint + 100,
                            floor(random() * 50000)::bigint + 500,
                            (ARRAY['Mozilla/5.0', 'PostmanRuntime/7.32', 'curl/7.88', 'axios/1.4.0', 'Java/17'])[floor(random() * 5 + 1)::INT],
                            '192.168.' || floor(random() * 255)::text || '.' || floor(random() * 255)::text,
                            v_date + (v_hour || ' hours')::interval + (floor(random() * 3600) || ' seconds')::interval
                        );
                        
                        v_metrics_count := v_metrics_count + 1;
                        
                        -- Feedback a cada 1000 métricas
                        IF v_metrics_count % 1000 = 0 THEN
                            RAISE NOTICE '  % métricas criadas...', v_metrics_count;
                        END IF;
                        
                    END LOOP; -- minutos
                END LOOP; -- horas
            END LOOP; -- dias
        END LOOP; -- subscriptions
    END LOOP; -- apis
    
    RAISE NOTICE '=== Total de métricas individuais criadas: % ===', v_metrics_count;
    
END $$;

-- ============================================================================
-- Agregar métricas diárias
-- ============================================================================
RAISE NOTICE '=== Agregando métricas diárias ===';

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

-- ============================================================================
-- Relatórios de Verificação
-- ============================================================================

-- Resumo geral
SELECT 
    '=== RESUMO GERAL ===' as titulo,
    '' as info
UNION ALL
SELECT 
    'Total de métricas individuais',
    COUNT(*)::text
FROM api_metrics
UNION ALL
SELECT 
    'Total de métricas diárias',
    COUNT(*)::text
FROM api_metrics_daily
UNION ALL
SELECT 
    'APIs com métricas',
    COUNT(DISTINCT api_id)::text
FROM api_metrics
UNION ALL
SELECT 
    'Consumers únicos',
    COUNT(DISTINCT consumer_id)::text
FROM api_metrics
UNION ALL
SELECT 
    'Período de dados',
    MIN(DATE(created_at))::text || ' até ' || MAX(DATE(created_at))::text
FROM api_metrics;

-- Resumo por API
SELECT 
    '=== MÉTRICAS POR API ===' as titulo,
    a.name as api_name,
    COUNT(m.id)::text as total_calls,
    ROUND(AVG(m.response_time_ms)::numeric, 2)::text || 'ms' as avg_response_time,
    COUNT(*) FILTER (WHERE m.status_code >= 400)::text as errors,
    ROUND((COUNT(*) FILTER (WHERE m.status_code >= 400)::numeric / NULLIF(COUNT(*), 0)::numeric * 100), 2)::text || '%' as error_rate
FROM api_metrics m
JOIN apis a ON a.id = m.api_id
GROUP BY a.id, a.name
ORDER BY COUNT(m.id) DESC;

-- Top Endpoints
SELECT 
    '=== TOP 10 ENDPOINTS ===' as titulo,
    m.endpoint,
    m.http_method,
    COUNT(*)::text as calls,
    ROUND(AVG(m.response_time_ms)::numeric, 2)::text || 'ms' as avg_time
FROM api_metrics m
GROUP BY m.endpoint, m.http_method
ORDER BY COUNT(*) DESC
LIMIT 10;

-- Tendência dos últimos 7 dias
SELECT 
    '=== TENDÊNCIA ÚLTIMOS 7 DIAS ===' as titulo,
    d.metric_date::text as data,
    SUM(d.total_calls)::text as total_calls,
    ROUND(AVG(d.avg_response_time)::numeric, 2)::text || 'ms' as avg_response,
    SUM(d.error_calls)::text as errors
FROM api_metrics_daily d
WHERE d.metric_date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY d.metric_date
ORDER BY d.metric_date DESC;

RAISE NOTICE '=== População de métricas concluída com sucesso! ===';
