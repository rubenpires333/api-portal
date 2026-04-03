-- Tabela de notificações
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    action_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para melhor performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_id_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_user_id_created_at ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Tabela de preferências de notificação
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_preferences_user_type UNIQUE (user_id, notification_type)
);

-- Índices
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Tabela de templates de notificação
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    language VARCHAR(5) NOT NULL DEFAULT 'pt',
    subject VARCHAR(255) NOT NULL,
    template TEXT NOT NULL,
    variables JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_templates_type_channel_lang UNIQUE (type, channel, language)
);

-- Índices
CREATE INDEX idx_notification_templates_type ON notification_templates(type);
CREATE INDEX idx_notification_templates_channel ON notification_templates(channel);

-- Comentários
COMMENT ON TABLE notifications IS 'Notificações in-app para usuários';
COMMENT ON TABLE notification_preferences IS 'Preferências de notificação por usuário e tipo';
COMMENT ON TABLE notification_templates IS 'Templates de notificação para email e in-app';

COMMENT ON COLUMN notifications.type IS 'Tipo de notificação (SUBSCRIPTION_REQUESTED, SUBSCRIPTION_APPROVED, etc)';
COMMENT ON COLUMN notifications.data IS 'Dados adicionais específicos do tipo de notificação';
COMMENT ON COLUMN notifications.action_url IS 'URL para ação relacionada à notificação';

COMMENT ON COLUMN notification_preferences.in_app_enabled IS 'Se notificações in-app estão habilitadas (sempre TRUE)';
COMMENT ON COLUMN notification_preferences.email_enabled IS 'Se notificações por email estão habilitadas';

COMMENT ON COLUMN notification_templates.channel IS 'Canal de notificação (IN_APP ou EMAIL)';
COMMENT ON COLUMN notification_templates.language IS 'Idioma do template (pt, en, etc)';
COMMENT ON COLUMN notification_templates.variables IS 'Lista de variáveis disponíveis no template';
