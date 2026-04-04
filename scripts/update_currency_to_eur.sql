-- ============================================================================
-- Script para atualizar moeda de USD para EUR
-- ============================================================================

-- Atualizar todas as carteiras existentes para EUR
UPDATE provider_wallets
SET currency = 'EUR',
    updated_at = NOW()
WHERE currency = 'USD';

-- Verificar resultado
SELECT 
    pw.id,
    pw.provider_id,
    u.email,
    pw.available_balance,
    pw.currency,
    'Moeda atualizada para EUR' as status
FROM provider_wallets pw
JOIN users u ON pw.provider_id = u.id
ORDER BY pw.created_at DESC;
