-- Migration: Adicionar novos tipos de notificação e corrigir tamanhos de campos
-- Data: 2026-04-04
-- Descrição: Atualizar constraint de notification_preferences e notifications, aumentar campos de audit_logs

-- 1. Remover constraint antiga de notification_preferences (se existir)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notification_preferences_notification_type_check'
    ) THEN
        ALTER TABLE notification_preferences 
        DROP CONSTRAINT notification_preferences_notification_type_check;
    END IF;
END $$;

-- 2. Adicionar nova constraint em notification_preferences com todos os tipos
ALTER TABLE notification_preferences
ADD CONSTRAINT notification_preferences_notification_type_check 
CHECK (notification_type::text IN (
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
    'PAYMENT_RECEIVED'
));

-- 3. Remover constraint antiga de notifications (se existir)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'notifications_type_check'
    ) THEN
        ALTER TABLE notifications 
        DROP CONSTRAINT notifications_type_check;
    END IF;
END $$;

-- 4. Adicionar nova constraint em notifications com todos os tipos
ALTER TABLE notifications
ADD CONSTRAINT notifications_type_check 
CHECK (type::text IN (
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
    'PAYMENT_RECEIVED'
));

-- 5. Aumentar tamanho dos campos de audit_logs que podem exceder 255 caracteres
ALTER TABLE audit_logs 
ALTER COLUMN endpoint TYPE VARCHAR(500);

ALTER TABLE audit_logs 
ALTER COLUMN error_message TYPE TEXT;

ALTER TABLE audit_logs 
ALTER COLUMN stack_trace TYPE TEXT;

ALTER TABLE audit_logs 
ALTER COLUMN user_agent TYPE VARCHAR(500);

-- 6. Verificar se há registros existentes que violam a constraint em notification_preferences
SELECT COUNT(*) as total_invalid_preferences
FROM notification_preferences
WHERE notification_type::text NOT IN (
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
    'PAYMENT_RECEIVED'
);

-- 7. Verificar se há notificações com os novos tipos
SELECT type, COUNT(*) as total
FROM notifications
WHERE type::text IN ('API_APPROVAL_REQUESTED', 'API_APPROVED', 'API_REJECTED')
GROUP BY type;

COMMIT;
