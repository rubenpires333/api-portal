-- ============================================
-- Script Consolidado: Setup Dashboard Consumer
-- Descrição: Configura permissões e popula dados para Dashboard Consumer
-- Credenciais: postgresql://localhost:5432/db_portal_api (postgres/postgres)
-- Data: 2026-04-03
-- ============================================

\echo '=== SETUP DASHBOARD CONSUMER ==='
\echo ''

-- ============================================
-- PARTE 1: Adicionar Permissão
-- ============================================

\echo '1. Adicionando permissão consumer.metrics.read...'

DO $$
DECLARE
    v_permission_id UUID;
    v_role_id UUID;
BEGIN
    -- Buscar ou criar a permissão
    SELECT id INTO v_permission_id FROM permissions WHERE code = 'consumer.metrics.read';
    
    IF v_permission_id IS NULL THEN
        INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
        VALUES (
            gen_random_uuid(),
            'Ler Métricas do Consumer',
            'consumer.metrics.read',
            'Permite visualizar métricas de uso das APIs',
            'consumer',
            'metrics.read',
            true,
            NOW(),
            NOW()
        )
        RETURNING id INTO v_permission_id;
        
        RAISE NOTICE '✓ Permissão consumer.metrics.read criada';
    ELSE
        RAISE NOTICE '✓ Permissão consumer.metrics.read já existe';
    END IF;
    
    -- Buscar o role CONSUMER
    SELECT id INTO v_role_id FROM roles WHERE code = 'CONSUMER';
    
    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Role CONSUMER não encontrado!';
    END IF;
    
    -- Adicionar permissão ao role
    IF NOT EXISTS (
        SELECT 1 FROM role_permissions 
        WHERE role_id = v_role_id AND permission_id = v_permission_id
    ) THEN
        INSERT INTO role_permissions (role_id, permission_id)
        VALUES (v_role_id, v_permission_id);
        
        RAISE NOTICE '✓ Permissão adicionada ao role CONSUMER';
    ELSE
        RAISE NOTICE '✓ Permissão já está no role CONSUMER';
    END IF;
END $$;

\echo ''
\echo '2. Verificando permissões do CONSUMER...'

SELECT 
    p.code,
    p.name
FROM permissions p
INNER JOIN role_permissions rp ON p.id = rp.permission_id
INNER JOIN roles r ON rp.role_id = r.id
WHERE r.code = 'CONSUMER'
ORDER BY p.code;

\echo ''

-- ============================================
-- PARTE 2: Popular Métricas
-- ============================================

\echo '3. Populando métricas de teste...'

DO $$
DECLARE
    v_consumer_id UUID;
    v_api_id_1 UUID;
    v_api_id_2 UUID;
    v_subscription_id_1 UUID;
    v_subscription_id_2 UUID;
    v_current_date DATE := CURRENT_DATE;
    v_date DATE;
    v_calls INT;
    v_response_time DOUBLE PRECISION;
    v_is_error BOOLEAN;
    v_status_code INT;
BEGIN
    -- Buscar consumer
    SELECT id INTO v_consumer_id 
    FROM users 
    WHERE email LIKE '%consumer%' 
    LIMIT 1;
    
    IF v_consumer_id IS NULL THEN
        RAISE EXCEPTION 'Nenhum consumer encontrado! Crie um usuário consumer primeiro.';
    END IF;
    
    RAISE NOTICE '✓ Consumer: %', v_consumer_id;
    
    -- Buscar APIs
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
        RAISE EXCEPTION 'Não há APIs suficientes! Crie pelo menos 2 APIs publicadas.';
    END IF;
    
    RAISE NOTICE '✓ APIs encontradas';
    
    -- Criar subscriptions
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
    ON CONFLICT DO NOTHING;
    
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
    ON CONFLICT DO NOTHING;
    
    RAISE NOTICE '✓ Subscriptions criadas';
    
    -- Limpar métricas antigas
    DELETE FROM api_metrics WHERE consumer_id = v_consumer_id;
    
    -- Gerar métricas
    FOR i IN 0..29 LOOP
        v_date := v_current_date - i;
        
        -- API 1
        v_calls := 50 + floor(random() * 100)::INT;
        FOR j IN 1..v_calls LOOP
            v_response_time := 50 + (random() * 200);
            v_is_error := random() < 0.05;
            v_status_code := CASE WHEN v_is_error THEN 500 ELSE 200 END;
            
            INSERT INTO api_metrics (
                id, api_id, consumer_id, endpoint, http_method,
                status_code, response_time_ms, is_error,
                request_size_bytes, response_size_bytes, created_at
            ) VALUES (
                gen_random_uuid(), v_api_id_1, v_consumer_id,
                CASE (random() * 3)::INT WHEN 0 THEN '/users' WHEN 1 THEN '/products' ELSE '/orders' END,
                CASE (random() * 2)::INT WHEN 0 THEN 'GET' ELSE 'POST' END,
                v_status_code, v_response_time, v_is_error,
                floor(random() * 1000)::BIGINT, floor(random() * 5000)::BIGINT,
                v_date + (random() * INTERVAL '24 hours')
            );
        END LOOP;
        
        -- API 2
        v_calls := 30 + floor(random() * 50)::INT;
        FOR j IN 1..v_calls LOOP
            v_response_time := 80 + (random() * 300);
            v_is_error := random() < 0.08;
            v_status_code := CASE WHEN v_is_error THEN 404 ELSE 200 END;
            
            INSERT INTO api_metrics (
                id, api_id, consumer_id, endpoint, http_method,
                status_code, response_time_ms, is_error,
                request_size_bytes, response_size_bytes, created_at
            ) VALUES (
                gen_random_uuid(), v_api_id_2, v_consumer_id,
                CASE (random() * 3)::INT WHEN 0 THEN '/data' WHEN 1 THEN '/reports' ELSE '/analytics' END,
                CASE (random() * 2)::INT WHEN 0 THEN 'GET' ELSE 'PUT' END,
                v_status_code, v_response_time, v_is_error,
                floor(random() * 800)::BIGINT, floor(random() * 3000)::BIGINT,
                v_date + (random() * INTERVAL '24 hours')
            );
        END LOOP;
    END LOOP;
    
    RAISE NOTICE '✓ Métricas geradas';
    RAISE NOTICE '';
    RAISE NOTICE '=== ESTATÍSTICAS ===';
    RAISE NOTICE 'Consumer ID: %', v_consumer_id;
    RAISE NOTICE 'Total métricas: %', (SELECT COUNT(*) FROM api_metrics WHERE consumer_id = v_consumer_id);
    RAISE NOTICE 'Taxa de erro: %', (SELECT ROUND((COUNT(*) FILTER (WHERE is_error) * 100.0 / COUNT(*))::NUMERIC, 2) FROM api_metrics WHERE consumer_id = v_consumer_id) || '%';
    RAISE NOTICE 'Tempo médio: %ms', (SELECT ROUND(AVG(response_time_ms)::NUMERIC, 2) FROM api_metrics WHERE consumer_id = v_consumer_id);
END $$;

\echo ''
\echo '=== SETUP CONCLUÍDO COM SUCESSO! ==='
\echo ''
\echo 'Próximos passos:'
\echo '1. Reinicie o backend Spring Boot'
\echo '2. Faça login como CONSUMER'
\echo '3. Acesse: /consumer/statistics'
\echo ''

