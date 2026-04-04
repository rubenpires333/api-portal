-- =====================================================
-- Atualizar Constraints para Tipos de Notificação de Withdrawal
-- =====================================================

-- 1. Atualizar constraint da tabela notifications
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notifications_type_check'
    ) THEN
        ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
        RAISE NOTICE '✓ Constraint antiga de notifications removida';
    END IF;

    ALTER TABLE notifications ADD CONSTRAINT notifications_type_check 
    CHECK (type IN (
        'SUBSCRIPTION_REQUESTED',
        'SUBSCRIPTION_APPROVED',
        'SUBSCRIPTION_REVOKED',
        'API_VERSION_RELEASED',
        'API_DEPRECATED',
        'API_APPROVAL_REQUESTED',
        'API_APPROVED',
        'API_REJECTED',
        'RATE_LIMIT_WARNING',
        'RATE_LIMIT_EXCEEDED',
        'API_MAINTENANCE',
        'API_INCIDENT',
        'PAYMENT_REQUIRED',
        'PAYMENT_RECEIVED',
        'WITHDRAWAL_REQUESTED',
        'WITHDRAWAL_APPROVED',
        'WITHDRAWAL_REJECTED'
    ));
    RAISE NOTICE '✓ Nova constraint de notifications adicionada';
END $$;

-- 2. Atualizar constraint da tabela notification_templates
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notification_templates_type_check'
    ) THEN
        ALTER TABLE notification_templates DROP CONSTRAINT notification_templates_type_check;
        RAISE NOTICE '✓ Constraint antiga de templates removida';
    END IF;

    ALTER TABLE notification_templates ADD CONSTRAINT notification_templates_type_check 
    CHECK (type IN (
        'SUBSCRIPTION_REQUESTED',
        'SUBSCRIPTION_APPROVED',
        'SUBSCRIPTION_REVOKED',
        'API_VERSION_RELEASED',
        'API_DEPRECATED',
        'API_APPROVAL_REQUESTED',
        'API_APPROVED',
        'API_REJECTED',
        'RATE_LIMIT_WARNING',
        'RATE_LIMIT_EXCEEDED',
        'API_MAINTENANCE',
        'API_INCIDENT',
        'PAYMENT_REQUIRED',
        'PAYMENT_RECEIVED',
        'WITHDRAWAL_REQUESTED',
        'WITHDRAWAL_APPROVED',
        'WITHDRAWAL_REJECTED'
    ));
    RAISE NOTICE '✓ Nova constraint de templates adicionada';
END $$;

-- 3. Atualizar constraint da tabela notification_preferences
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notification_preferences_notification_type_check'
    ) THEN
        ALTER TABLE notification_preferences DROP CONSTRAINT notification_preferences_notification_type_check;
        RAISE NOTICE '✓ Constraint antiga de preferências removida';
    END IF;

    ALTER TABLE notification_preferences ADD CONSTRAINT notification_preferences_notification_type_check 
    CHECK (notification_type IN (
        'SUBSCRIPTION_REQUESTED',
        'SUBSCRIPTION_APPROVED',
        'SUBSCRIPTION_REVOKED',
        'API_VERSION_RELEASED',
        'API_DEPRECATED',
        'API_APPROVAL_REQUESTED',
        'API_APPROVED',
        'API_REJECTED',
        'RATE_LIMIT_WARNING',
        'RATE_LIMIT_EXCEEDED',
        'API_MAINTENANCE',
        'API_INCIDENT',
        'PAYMENT_REQUIRED',
        'PAYMENT_RECEIVED',
        'WITHDRAWAL_REQUESTED',
        'WITHDRAWAL_APPROVED',
        'WITHDRAWAL_REJECTED'
    ));
    RAISE NOTICE '✓ Nova constraint de preferências adicionada';
END $$;

-- Mensagem final
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ CONSTRAINTS ATUALIZADAS COM SUCESSO!';
    RAISE NOTICE '📋 3 tabelas atualizadas:';
    RAISE NOTICE '   - notifications';
    RAISE NOTICE '   - notification_templates';
    RAISE NOTICE '   - notification_preferences';
    RAISE NOTICE '========================================';
END $$;
