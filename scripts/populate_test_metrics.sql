-- Script para popular métricas de teste
-- Execute este script após criar as tabelas de métricas

-- Função auxiliar para gerar métricas de teste
DO $$
DECLARE
    v_api_id UUID;
    v_subscription_id UUID;
    v_consumer_id VARCHAR(255);
    v_consumer_name VARCHAR(255);
    v_date DATE;
    v_hour INT;
    v_calls_per_hour INT;
    v_response_time DOUBLE PRECISION;
    v_status_code INT;
    v_endpoint VARCHAR(500);
    v_method VARCHAR(10);
    v_endpoints TEXT[] := ARRAY['/users', '/products', '/orders', '/payments', '/auth/login', '/auth/refresh', '/reports', '/analytics'];
    v_methods TEXT[] := ARRAY['GET', 'POST', 'PUT', 'DELETE'];
BEGIN
    -- Buscar APIs publicadas
    FOR v_api_id IN SELECT id FROM apis WHERE status = 'PUBLISHED' LIMIT 5
    LOOP
        -- Buscar subscriptions ativas para esta API
        FOR v_subscription_id, v_consumer_id, v_consumer_name IN 
            SELECT id, consumer_id, consumer_name 
            FROM subscriptions 
            WHERE api_id = v_api_id AND status = 'ACTIVE'
            LIMIT 3
        LOOP
            -- Gerar métricas para os últimos 30 dias
            FOR v_date IN SELECT generate_series(
                CURRENT_DATE - INTERVAL '30 days',
                CURRENT_DATE - INTERVAL '1 day',
                '1 day'::interval
            )::date
            LOOP
                -- Gerar chamadas ao longo do dia
                FOR v_hour IN 0..23
                LOOP
                    -- Número de chamadas varia por hora (mais durante horário comercial)
                    IF v_hour BETWEEN 8 AND 18 THEN
                        v_calls_per_hour := 50 + floor(random() * 100)::int;
                    ELSE
                        v_calls_per_hour := 10 + floor(random() * 30)::int;
                    END IF;
                    
                    -- Gerar chamadas individuais
                    FOR i IN 1..v_calls_per_hour
                    LOOP
                        -- Tempo de resposta varia (maioria rápida, algumas lentas)
                        IF random() < 0.9 THEN
                            v_response_time := 50 + (random() * 200); -- 50-250ms
                        ELSE
                            v_response_time := 500 + (random() * 1500); -- 500-2000ms
                        END IF;
                        
                        -- Status code (95% sucesso, 5% erro)
                        IF random() < 0.95 THEN
                            v_status_code := CASE floor(random() * 3)::int
                                WHEN 0 THEN 200
                                WHEN 1 THEN 201
                                ELSE 204
                            END;
                        ELSE
                            v_status_code := CASE floor(random() * 4)::int
                                WHEN 0 THEN 400
                                WHEN 1 THEN 401
                                WHEN 2 THEN 404
                                ELSE 500
                            END;
                        END IF;
                        
                        -- Endpoint e método aleatórios
                        v_endpoint := v_endpoints[1 + floor(random() * array_length(v_endpoints, 1))::int];
                        v_method := v_methods[1 + floor(random() * array_length(v_methods, 1))::int];
                        
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
                            'TestClient/1.0',
                            '192.168.1.' || floor(random() * 255)::text,
                            v_date + (v_hour || ' hours')::interval + (floor(random() * 3600) || ' seconds')::interval
                        );
                    END LOOP;
                END LOOP;
            END LOOP;
        END LOOP;
    END LOOP;
    
    RAISE NOTICE 'Métricas de teste criadas com sucesso!';
END $$;

-- Agregar métricas diárias para os últimos 30 dias
DO $$
DECLARE
    v_date DATE;
    v_api_id UUID;
    v_total_calls BIGINT;
    v_success_calls BIGINT;
    v_error_calls BIGINT;
    v_avg_response DOUBLE PRECISION;
    v_min_response DOUBLE PRECISION;
    v_max_response DOUBLE PRECISION;
    v_total_req_size BIGINT;
    v_total_resp_size BIGINT;
    v_unique_consumers INT;
BEGIN
    FOR v_date IN SELECT generate_series(
        CURRENT_DATE - INTERVAL '30 days',
        CURRENT_DATE - INTERVAL '1 day',
        '1 day'::interval
    )::date
    LOOP
        FOR v_api_id IN SELECT DISTINCT api_id FROM api_metrics WHERE DATE(created_at) = v_date
        LOOP
            SELECT 
                COUNT(*),
                COUNT(*) FILTER (WHERE status_code >= 200 AND status_code < 300),
                COUNT(*) FILTER (WHERE status_code >= 400),
                AVG(response_time_ms),
                MIN(response_time_ms),
                MAX(response_time_ms),
                SUM(request_size_bytes),
                SUM(response_size_bytes),
                COUNT(DISTINCT consumer_id)
            INTO 
                v_total_calls,
                v_success_calls,
                v_error_calls,
                v_avg_response,
                v_min_response,
                v_max_response,
                v_total_req_size,
                v_total_resp_size,
                v_unique_consumers
            FROM api_metrics
            WHERE api_id = v_api_id AND DATE(created_at) = v_date;
            
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
                unique_consumers
            ) VALUES (
                gen_random_uuid(),
                v_api_id,
                v_date,
                v_total_calls,
                v_success_calls,
                v_error_calls,
                v_avg_response,
                v_min_response,
                v_max_response,
                v_total_req_size,
                v_total_resp_size,
                v_unique_consumers
            )
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
        END LOOP;
    END LOOP;
    
    RAISE NOTICE 'Métricas diárias agregadas com sucesso!';
END $$;

-- Verificar resultados
SELECT 
    'Total de métricas criadas' as descricao,
    COUNT(*) as quantidade
FROM api_metrics
UNION ALL
SELECT 
    'Total de métricas diárias agregadas',
    COUNT(*)
FROM api_metrics_daily
UNION ALL
SELECT 
    'APIs com métricas',
    COUNT(DISTINCT api_id)
FROM api_metrics;
