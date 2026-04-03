-- ============================================
-- Script: Popular Métricas para Consumer
-- Descrição: Popula dados de métricas para testar Dashboard Consumer
-- Credenciais: postgresql://localhost:5432/db_portal_api (postgres/postgres)
-- Data: 2026-04-03
-- ============================================

-- IMPORTANTE: Execute este script APÓS ter:
-- 1. Tabelas api_metrics e api_metrics_daily criadas
-- 2. Pelo menos 1 consumer com subscriptions ativas
-- 3. Pelo menos 2 APIs publicadas

DO $$
DECLARE
    v_consumer_id UUID;
    v_api_id_1 UUID;
    v_api_id_2 UUID;
    v_subscription_id_1 UUID;
    v_subscription_id_2 UUID;
    v_current_date DATE := CURRENT_DATE;
    v_date DATE;
    v_hour INT;
    v_calls INT;
    v_response_time DOUBLE PRECISION;
    v_is_error BOOLEAN;
    v_status_code INT;
BEGIN
    RAISE NOTICE '=== Iniciando população de métricas para Consumer ===';
    
    -- 1. Buscar um consumer existente
    SELECT id INTO v_consumer_id 
    FROM users 
    WHERE email LIKE '%consumer%' 
    LIMIT 1;
    
    IF v_consumer_id IS NULL THEN
        RAISE EXCEPTION 'Nenhum consumer encontrado! Crie um usuário consumer primeiro.';
    END IF;
    
    RAISE NOTICE '✓ Consumer encontrado: %', v_consumer_id;
    
    -- 2. Buscar 2 APIs publicadas
    SELECT id INTO v_api_id_1 
    FROM apis 
    WHERE status = 'PUBLISHED' 
    ORDER BY created_at 
    LIMIT 1;
    
    SELECT id INTO v_api_id_2 
    FROM apis 
    WHERE status = 'PUBLISHED' AND id != v_api_id_1
    ORDER BY created_at 
    LIMIT 1 OFFSET 1;
    
    IF v_api_id_1 IS NULL OR v_api_id_2 IS NULL THEN
        RAISE EXCEPTION 'Não há APIs suficientes publicadas! Crie pelo menos 2 APIs.';
    END IF;
    
    RAISE NOTICE '✓ APIs encontradas: % e %', v_api_id_1, v_api_id_2;
    
    -- 3. Criar ou verificar subscriptions
    SELECT id INTO v_subscription_id_1
    FROM subscriptions
    WHERE consumer_id = v_consumer_id::TEXT AND api_id = v_api_id_1;
    
    IF v_subscription_id_1 IS NULL THEN
        INSERT INTO subscriptions (
            id, consumer_id, consumer_name, consumer_email, api_id,
            status, api_key, requests_used, created_at, updated_at
        ) VALUES (
            gen_random_uuid(),
            v_consumer_id::TEXT,
            'Consumer Test',
            'consumer@test.com',
            v_api_id_1,
            'ACTIVE',
            'test_key_' || substr(md5(random()::text), 1, 32),
            0,
            NOW(),
            NOW()
        )
        RETURNING id INTO v_subscription_id_1;
        
        RAISE NOTICE '✓ Subscription 1 criada';
    ELSE
        RAISE NOTICE '✓ Subscription 1 já existe';
    END IF;
    
    SELECT id INTO v_subscription_id_2
    FROM subscriptions
    WHERE consumer_id = v_consumer_id::TEXT AND api_id = v_api_id_2;
    
    IF v_subscription_id_2 IS NULL THEN
        INSERT INTO subscriptions (
            id, consumer_id, consumer_name, consumer_email, api_id,
            status, api_key, requests_used, created_at, updated_at
        ) VALUES (
            gen_random_uuid(),
            v_consumer_id::TEXT,
            'Consumer Test',
            'consumer@test.com',
            v_api_id_2,
            'ACTIVE',
            'test_key_' || substr(md5(random()::text), 1, 32),
            0,
            NOW(),
            NOW()
        )
        RETURNING id INTO v_subscription_id_2;
        
        RAISE NOTICE '✓ Subscription 2 criada';
    ELSE
        RAISE NOTICE '✓ Subscription 2 já existe';
    END IF;
    
    -- 4. Limpar métricas antigas do consumer
    DELETE FROM api_metrics WHERE consumer_id = v_consumer_id;
    RAISE NOTICE '✓ Métricas antigas removidas';
    
    -- 5. Gerar métricas dos últimos 30 dias
    RAISE NOTICE 'Gerando métricas dos últimos 30 dias...';
    
    FOR i IN 0..29 LOOP
        v_date := v_current_date - i;
        
        -- API 1: 50-150 chamadas por dia
        v_calls := 50 + floor(random() * 100)::INT;
        
        FOR j IN 1..v_calls LOOP
            v_response_time := 50 + (random() * 200);
            v_is_error := random() < 0.05; -- 5% de erro
            v_status_code := CASE WHEN v_is_error THEN 500 ELSE 200 END;
            
            INSERT INTO api_metrics (
                id, api_id, consumer_id, endpoint, http_method,
                status_code, response_time_ms, is_error,
                request_size_bytes, response_size_bytes,
                created_at
            ) VALUES (
                gen_random_uuid(),
                v_api_id_1,
                v_consumer_id,
                CASE (random() * 3)::INT
                    WHEN 0 THEN '/users'
                    WHEN 1 THEN '/products'
                    ELSE '/orders'
                END,
                CASE (random() * 2)::INT
                    WHEN 0 THEN 'GET'
                    ELSE 'POST'
                END,
                v_status_code,
                v_response_time,
                v_is_error,
                floor(random() * 1000)::BIGINT,
                floor(random() * 5000)::BIGINT,
                v_date + (random() * INTERVAL '24 hours')
            );
        END LOOP;
        
        -- API 2: 30-80 chamadas por dia
        v_calls := 30 + floor(random() * 50)::INT;
        
        FOR j IN 1..v_calls LOOP
            v_response_time := 80 + (random() * 300);
            v_is_error := random() < 0.08; -- 8% de erro
            v_status_code := CASE WHEN v_is_error THEN 404 ELSE 200 END;
            
            INSERT INTO api_metrics (
                id, api_id, consumer_id, endpoint, http_method,
                status_code, response_time_ms, is_error,
                request_size_bytes, response_size_bytes,
                created_at
            ) VALUES (
                gen_random_uuid(),
                v_api_id_2,
                v_consumer_id,
                CASE (random() * 3)::INT
                    WHEN 0 THEN '/data'
                    WHEN 1 THEN '/reports'
                    ELSE '/analytics'
                END,
                CASE (random() * 2)::INT
                    WHEN 0 THEN 'GET'
                    ELSE 'PUT'
                END,
                v_status_code,
                v_response_time,
                v_is_error,
                floor(random() * 800)::BIGINT,
                floor(random() * 3000)::BIGINT,
                v_date + (random() * INTERVAL '24 hours')
            );
        END LOOP;
        
        IF i % 5 = 0 THEN
            RAISE NOTICE '  Processado dia % (% dias atrás)', v_date, i;
        END IF;
    END LOOP;
    
    RAISE NOTICE '✓ Métricas geradas com sucesso!';
    
    -- 6. Estatísticas finais
    RAISE NOTICE '';
    RAISE NOTICE '=== ESTATÍSTICAS ===';
    RAISE NOTICE 'Consumer ID: %', v_consumer_id;
    RAISE NOTICE 'Total de métricas: %', (SELECT COUNT(*) FROM api_metrics WHERE consumer_id = v_consumer_id);
    RAISE NOTICE 'API 1 chamadas: %', (SELECT COUNT(*) FROM api_metrics WHERE consumer_id = v_consumer_id AND api_id = v_api_id_1);
    RAISE NOTICE 'API 2 chamadas: %', (SELECT COUNT(*) FROM api_metrics WHERE consumer_id = v_consumer_id AND api_id = v_api_id_2);
    RAISE NOTICE 'Taxa de erro: %', (SELECT ROUND((COUNT(*) FILTER (WHERE is_error) * 100.0 / COUNT(*))::NUMERIC, 2) FROM api_metrics WHERE consumer_id = v_consumer_id) || '%';
    RAISE NOTICE 'Tempo médio: %ms', (SELECT ROUND(AVG(response_time_ms)::NUMERIC, 2) FROM api_metrics WHERE consumer_id = v_consumer_id);
    RAISE NOTICE '';
    RAISE NOTICE '✓ Script concluído com sucesso!';
    
END $$;
