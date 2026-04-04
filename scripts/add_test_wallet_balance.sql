-- ============================================================================
-- Script para adicionar saldo de teste na carteira do provider
-- ============================================================================
-- 
-- INSTRUÇÕES:
-- 1. Substitua 'SEU_PROVIDER_ID_AQUI' pelo ID real do provider
-- 2. Execute este script no banco de dados
-- 3. O script criará uma carteira (se não existir) e adicionará transações de teste
--
-- ============================================================================

-- Variável para o provider_id (SUBSTITUA PELO ID REAL)
DO $$
DECLARE
    v_provider_id UUID := 'SEU_PROVIDER_ID_AQUI'; -- ALTERE AQUI
    v_wallet_id UUID;
    v_transaction_id UUID;
BEGIN
    -- Verificar se o provider existe
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = v_provider_id) THEN
        RAISE EXCEPTION 'Provider com ID % não encontrado', v_provider_id;
    END IF;

    -- Criar ou obter a carteira do provider
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
        v_provider_id,
        0.00,
        0.00,
        0.00,
        0.00,
        'EUR',
        10.00,
        NOW(),
        NOW()
    )
    ON CONFLICT (provider_id) DO NOTHING
    RETURNING id INTO v_wallet_id;

    -- Se a carteira já existia, obter o ID
    IF v_wallet_id IS NULL THEN
        SELECT id INTO v_wallet_id FROM provider_wallets WHERE provider_id = v_provider_id;
    END IF;

    RAISE NOTICE 'Wallet ID: %', v_wallet_id;

    -- Adicionar transação de receita disponível (R$ 150.00)
    INSERT INTO wallet_transactions (
        id,
        wallet_id,
        amount,
        type,
        status,
        description,
        available_at,
        created_at
    ) VALUES (
        gen_random_uuid(),
        v_wallet_id,
        150.00,
        'CREDIT_REVENUE',
        'AVAILABLE',
        'Receita de teste - API vendida',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day'
    );

    -- Adicionar transação de receita pendente (R$ 80.00)
    INSERT INTO wallet_transactions (
        id,
        wallet_id,
        amount,
        type,
        status,
        description,
        available_at,
        created_at
    ) VALUES (
        gen_random_uuid(),
        v_wallet_id,
        80.00,
        'CREDIT_REVENUE',
        'PENDING',
        'Receita de teste - Em holdback',
        NOW() + INTERVAL '10 days',
        NOW() - INTERVAL '4 days'
    );

    -- Adicionar mais uma transação disponível (R$ 75.50)
    INSERT INTO wallet_transactions (
        id,
        wallet_id,
        amount,
        type,
        status,
        description,
        available_at,
        created_at
    ) VALUES (
        gen_random_uuid(),
        v_wallet_id,
        75.50,
        'CREDIT_REVENUE',
        'AVAILABLE',
        'Receita de teste - Subscrição mensal',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days'
    );

    -- Adicionar transação de taxa da plataforma (-R$ 15.00)
    INSERT INTO wallet_transactions (
        id,
        wallet_id,
        amount,
        type,
        status,
        description,
        available_at,
        created_at
    ) VALUES (
        gen_random_uuid(),
        v_wallet_id,
        -15.00,
        'DEBIT_PLATFORM_FEE',
        'DEBITED',
        'Taxa da plataforma (10%)',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day'
    );

    -- Atualizar os saldos da carteira
    UPDATE provider_wallets
    SET 
        available_balance = (
            SELECT COALESCE(SUM(amount), 0)
            FROM wallet_transactions
            WHERE wallet_id = v_wallet_id
            AND status = 'AVAILABLE'
        ),
        pending_balance = (
            SELECT COALESCE(SUM(amount), 0)
            FROM wallet_transactions
            WHERE wallet_id = v_wallet_id
            AND status = 'PENDING'
        ),
        reserved_balance = (
            SELECT COALESCE(SUM(amount), 0)
            FROM wallet_transactions
            WHERE wallet_id = v_wallet_id
            AND status = 'RESERVED'
        ),
        lifetime_earned = (
            SELECT COALESCE(SUM(amount), 0)
            FROM wallet_transactions
            WHERE wallet_id = v_wallet_id
            AND type LIKE 'CREDIT_%'
        ),
        updated_at = NOW()
    WHERE id = v_wallet_id;

    RAISE NOTICE 'Saldo de teste adicionado com sucesso!';
    RAISE NOTICE 'Saldo disponível: R$ 210.50 (150 + 75.50 - 15)';
    RAISE NOTICE 'Saldo pendente: R$ 80.00';
    RAISE NOTICE 'Total ganho: R$ 305.50';
END $$;

-- Verificar os saldos criados
SELECT 
    pw.provider_id,
    u.name as provider_name,
    pw.available_balance,
    pw.pending_balance,
    pw.reserved_balance,
    pw.lifetime_earned,
    pw.currency,
    pw.minimum_payout
FROM provider_wallets pw
JOIN users u ON pw.provider_id = u.id
WHERE pw.provider_id = 'SEU_PROVIDER_ID_AQUI'; -- ALTERE AQUI

-- Verificar as transações criadas
SELECT 
    wt.created_at,
    wt.type,
    wt.amount,
    wt.status,
    wt.description,
    wt.available_at
FROM wallet_transactions wt
JOIN provider_wallets pw ON wt.wallet_id = pw.id
WHERE pw.provider_id = 'SEU_PROVIDER_ID_AQUI' -- ALTERE AQUI
ORDER BY wt.created_at DESC;
