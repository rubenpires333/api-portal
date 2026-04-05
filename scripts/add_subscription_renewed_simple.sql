-- Remover constraints antigos
ALTER TABLE notification_preferences DROP CONSTRAINT IF EXISTS notification_preferences_notification_type_check;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

-- Adicionar novos constraints com SUBSCRIPTION_RENEWED (sem validar dados existentes)
ALTER TABLE notification_preferences
ADD CONSTRAINT notification_preferences_notification_type_check 
CHECK (notification_type::text IN (
    'SUBSCRIPTION_REQUESTED',
    'SUBSCRIPTION_APPROVED',
    'SUBSCRIPTION_REVOKED',
    'SUBSCRIPTION_RENEWED',
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

ALTER TABLE notifications
ADD CONSTRAINT notifications_type_check 
CHECK (type::text IN (
    'SUBSCRIPTION_REQUESTED',
    'SUBSCRIPTION_APPROVED',
    'SUBSCRIPTION_REVOKED',
    'SUBSCRIPTION_RENEWED',
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
