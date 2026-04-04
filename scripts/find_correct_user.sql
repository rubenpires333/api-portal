-- ============================================================================
-- Script para encontrar o usuário correto
-- ============================================================================

-- Buscar usuário pelo keycloak_id que o frontend está enviando
SELECT 
    u.id as user_id,
    u.keycloak_id,
    u.email,
    u.name,
    u.role,
    'Este é o usuário que está logado no frontend' as nota
FROM users u
WHERE u.keycloak_id = 'd95a9272-a662-4541-8dbd-df6c03763836';

-- Buscar usuário pelo ID que tem a carteira com saldo
SELECT 
    u.id as user_id,
    u.keycloak_id,
    u.email,
    u.name,
    u.role,
    pw.available_balance,
    'Este é o usuário que tem a carteira com saldo' as nota
FROM users u
JOIN provider_wallets pw ON pw.provider_id = u.id
WHERE u.id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- Verificar se são o mesmo usuário
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM users 
            WHERE id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2' 
            AND keycloak_id = 'd95a9272-a662-4541-8dbd-df6c03763836'
        ) THEN 'SIM - São o mesmo usuário'
        ELSE 'NÃO - São usuários diferentes'
    END as resultado;
