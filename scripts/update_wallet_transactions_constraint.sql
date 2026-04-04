-- =====================================================
-- Atualizar Constraint de Status para Wallet Transactions
-- =====================================================

-- Remover constraint antiga se existir
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'wallet_transactions_status_check'
    ) THEN
        ALTER TABLE wallet_transactions DROP CONSTRAINT wallet_transactions_status_check;
        RAISE NOTICE '✓ Constraint antiga de wallet_transactions removida';
    END IF;
END $$;

-- Adicionar nova constraint com todos os status do enum TransactionStatus
ALTER TABLE wallet_transactions ADD CONSTRAINT wallet_transactions_status_check 
CHECK (status IN (
    'PENDING',
    'AVAILABLE',
    'RESERVED',
    'DEBITED',
    'COMPLETED',
    'CANCELLED'
));

-- Mensagem de sucesso
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ CONSTRAINT ATUALIZADA COM SUCESSO!';
    RAISE NOTICE '📋 Status permitidos:';
    RAISE NOTICE '   - PENDING (Em holdback)';
    RAISE NOTICE '   - AVAILABLE (Disponível para levantamento)';
    RAISE NOTICE '   - RESERVED (Reservado para levantamento)';
    RAISE NOTICE '   - DEBITED (Já debitado)';
    RAISE NOTICE '   - COMPLETED (Transação completada)';
    RAISE NOTICE '   - CANCELLED (Cancelado)';
    RAISE NOTICE '========================================';
END $$;
