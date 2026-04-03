-- Adicionar configurações de email ao platform_settings
INSERT INTO platform_settings (id, category, setting_key, setting_value, setting_type, description, is_secret, is_public, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'EMAIL', 'mail.enabled', 'false', 'BOOLEAN', 'Habilitar envio de emails', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'EMAIL', 'mail.host', 'smtp.gmail.com', 'STRING', 'Servidor SMTP', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'EMAIL', 'mail.port', '587', 'NUMBER', 'Porta SMTP', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'EMAIL', 'mail.username', '', 'STRING', 'Usuário SMTP', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'EMAIL', 'mail.password', '', 'SECRET', 'Senha SMTP', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'EMAIL', 'mail.from.email', 'noreply@apiportal.com', 'STRING', 'Email remetente', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'EMAIL', 'mail.from.name', 'API Portal', 'STRING', 'Nome do remetente', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;
