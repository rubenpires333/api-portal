-- Migration: Adicionar SUBSCRIPTION_RENEWED aos tipos de notificação
-- Data: 2026-04-05
-- Descrição: Adicionar tipo SUBSCRIPTION_RENEWED para notificações de upgrade/downgrade de plano

-- 0. Verificar registros existentes com SUBSCRIPTION_RENEWED
DO $$ 
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM notification_preferences
    WHERE notification_type = 'SUBSCRIPTION_RENEWED';
    
    IF invalid_count > 0 THEN
        RAISE NOTICE 'Encontrados % registros com SUBSCRIPTION_RENEWED', invalid_count;
    END IF;
END $$;

-- 1. Remover constraint antiga de notification_preferences
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notification_preferences_notification_type_check'
    ) THEN
        ALTER TABLE notification_preferences 
        DROP CONSTRAINT notification_preferences_notification_type_check;
        RAISE NOTICE '✓ Constraint antiga de notification_preferences removida';
    END IF;
END $$;

-- 2. Adicionar nova constraint em notification_preferences incluindo SUBSCRIPTION_RENEWED
-- Usar NOT VALID para não validar dados existentes imediatamente
ALTER TABLE notification_preferences
ADD CONSTRAINT notification_preferences_notification_type_check 
CHECK (notification_type::text IN (
    'SUBSCRIPTION_REQUESTED',
    'SUBSCRIPTION_APPROVED',
    'SUBSCRIPTION_REVOKED',
    'SUBSCRIPTION_RENEWED',  -- NOVO TIPO
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
    'PAYMENT_RECEIVED'
)) NOT VALID;

DO $$ 
BEGIN
    RAISE NOTICE '✓ Constraint de notification_preferences adicionada (NOT VALID)';
END $$;

-- Validar o constraint (isso vai falhar se houver dados inválidos)
ALTER TABLE notification_preferences 
VALIDATE CONSTRAINT notification_preferences_notification_type_check;

DO $$ 
BEGIN
    RAISE NOTICE '✓ Constraint de notification_preferences validada com sucesso';
END $$;

-- 3. Remover constraint antiga de notifications
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notifications_type_check'
    ) THEN
        ALTER TABLE notifications 
        DROP CONSTRAINT notifications_type_check;
        RAISE NOTICE '✓ Constraint antiga de notifications removida';
    END IF;
END $$;

-- 4. Adicionar nova constraint em notifications incluindo SUBSCRIPTION_RENEWED
-- Usar NOT VALID para não validar dados existentes imediatamente
ALTER TABLE notifications
ADD CONSTRAINT notifications_type_check 
CHECK (type::text IN (
    'SUBSCRIPTION_REQUESTED',
    'SUBSCRIPTION_APPROVED',
    'SUBSCRIPTION_REVOKED',
    'SUBSCRIPTION_RENEWED',  -- NOVO TIPO
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
    'PAYMENT_RECEIVED'
)) NOT VALID;

DO $$ 
BEGIN
    RAISE NOTICE '✓ Constraint de notifications adicionada (NOT VALID)';
END $$;

-- Validar o constraint
ALTER TABLE notifications 
VALIDATE CONSTRAINT notifications_type_check;

DO $$ 
BEGIN
    RAISE NOTICE '✓ Constraint de notifications validada com sucesso';
END $$;

-- 5. Verificar resultado final
DO $$ 
DECLARE
    pref_count INTEGER;
    notif_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO pref_count
    FROM notification_preferences
    WHERE notification_type = 'SUBSCRIPTION_RENEWED';
    
    SELECT COUNT(*) INTO notif_count
    FROM notifications
    WHERE type = 'SUBSCRIPTION_RENEWED';
    
    RAISE NOTICE '✓ Migration completa:';
    RAISE NOTICE '  - notification_preferences com SUBSCRIPTION_RENEWED: %', pref_count;
    RAISE NOTICE '  - notifications com SUBSCRIPTION_RENEWED: %', notif_count;
END $$;
