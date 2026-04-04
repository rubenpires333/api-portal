-- ============================================================================
-- Script para adicionar saldo de teste ao provider
-- ID: 69f2020f-7e2a-42ba-bf32-5821cfebe0c2
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
    '69f2020f-7e2a-42ba-bf32-5821cfebe0c2',
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
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- Atualizar saldos da carteira
UPDATE provider_wallets
SET 
    available_balance = 250.00,
    lifetime_earned = 250.00,
    updated_at = NOW()
WHERE provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';

-- Verificar resultado
SELECT 
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
WHERE pw.provider_id = '69f2020f-7e2a-42ba-bf32-5821cfebe0c2';
