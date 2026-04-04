-- ============================================================================
-- Script para verificar IDs de usuários e carteiras
-- ============================================================================

-- Ver todos os providers e seus IDs
SELECT 
    u.id as user_id,
    u.keycloak_id,
    u.email,
    u.name,
    u.role,
    pw.id as wallet_id,
    pw.available_balance
FROM users u
LEFT JOIN provider_wallets pw ON pw.provider_id = u.id
WHERE u.role = 'PROVIDER'
ORDER BY u.created_at DESC;

-- Verificar se existe carteira com keycloak_id ao invés de user_id
SELECT 
    pw.id as wallet_id,
    pw.provider_id,
    pw.available_balance,
    u.id as user_id,
    u.keycloak_id,
    u.email,
    CASE 
        WHEN pw.provider_id = u.id THEN 'OK - Usando user.id'
        WHEN pw.provider_id::text = u.keycloak_id THEN 'ERRO - Usando keycloak_id'
        ELSE 'ERRO - ID não corresponde'
    END as status
FROM provider_wallets pw
LEFT JOIN users u ON pw.provider_id = u.id OR pw.provider_id::text = u.keycloak_id
ORDER BY pw.created_at DESC;
