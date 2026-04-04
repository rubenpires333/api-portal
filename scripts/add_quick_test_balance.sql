-- ============================================================================
-- Script RÁPIDO para adicionar saldo de teste
-- ============================================================================
-- 
-- COMO USAR:
-- 1. Primeiro, descubra o ID do seu provider:
--    SELECT id, name, email FROM users WHERE role_code = 'PROVIDER';
--
-- 2. Copie o ID e substitua abaixo
-- 3. Execute este script
--
-- ============================================================================

-- PASSO 1: Descobrir o ID do provider
SELECT 
    id as provider_id,
    name,
    email,
    role
FROM users 
WHERE role = 'PROVIDER'
ORDER BY created_at DESC
LIMIT 5;

-- ============================================================================
-- PASSO 2: Criar carteira e adicionar saldo
-- SUBSTITUA 'COLE_O_ID_AQUI' pelo ID do provider acima
-- ============================================================================

-- Criar carteira (se não existir)
INSERT INTO provider_wallets (
    id,
    provider_id,
    available_balance,
    pending_balance,
    reserved_balance,
    lifetime_earned,
    currency,
    minimum_payout,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'COLE_O_ID_AQUI', -- ALTERE AQUI
    0.00,
    0.00,
    0.00,
    0.00,
    'EUR',
    10.00,
    NOW(),
    NOW()
)
ON CONFLICT (provider_id) DO NOTHING;

-- Adicionar transação de R$ 250.00 disponível
INSERT INTO wallet_transactions (
    id,
    wallet_id,
    amount,
    type,
    status,
    description,
    available_at,
    created_at
)
SELECT 
    gen_random_uuid(),
    pw.id,
    250.00,
    'CREDIT_REVENUE',
    'AVAILABLE',
    'Saldo de teste para levantamento',
    NOW(),
    NOW()
FROM provider_wallets pw
WHERE pw.provider_id = 'COLE_O_ID_AQUI'; -- ALTERE AQUI

-- Atualizar saldos da carteira
UPDATE provider_wallets
SET 
    available_balance = 250.00,
    lifetime_earned = 250.00,
    updated_at = NOW()
WHERE provider_id = 'COLE_O_ID_AQUI'; -- ALTERE AQUI

-- Verificar resultado
SELECT 
    u.name as provider_name,
    u.email,
    pw.available_balance,
    pw.pending_balance,
    pw.currency,
    pw.minimum_payout,
    CASE 
        WHEN pw.available_balance >= pw.minimum_payout THEN 'SIM - Pode levantar!'
        ELSE 'NÃO - Saldo insuficiente'
    END as pode_levantar
FROM provider_wallets pw
JOIN users u ON pw.provider_id = u.id
WHERE pw.provider_id = 'COLE_O_ID_AQUI'; -- ALTERE AQUI
