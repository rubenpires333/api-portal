-- =====================================================
-- Templates de Notificação para Sistema de Levantamentos
-- =====================================================
-- 
-- IMPORTANTE: Execute primeiro o script update_notification_constraints.sql
--             antes de executar este script!
-- =====================================================

-- 1. Template In-App: Nova Solicitação de Levantamento (para Admin)
INSERT INTO notification_templates (
    id,
    type,
    channel,
    language,
    subject,
    template,
    variables,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_REQUESTED',
    'IN_APP',
    'pt-PT',
    'Nova Solicitação de Levantamento',
    'Nova solicitação de levantamento de {{amount}} aguardando aprovação. Provider: {{providerName}} ({{providerEmail}}). Método: {{method}}.',
    '["amount", "providerName", "providerEmail", "method", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 2. Template Email: Nova Solicitação de Levantamento (para Admin)
INSERT INTO notification_templates (
    id,
    type,
    channel,
    language,
    subject,
    template,
    variables,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_REQUESTED',
    'EMAIL',
    'pt-PT',
    'Nova Solicitação de Levantamento - Aprovação Necessária',
    '<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8b739; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
        .info-box { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #f8b739; }
        .button { display: inline-block; padding: 12px 30px; background-color: #28a745; color: white; text-decoration: none; border-radius: 5px; margin: 10px 5px; }
        .button.reject { background-color: #dc3545; }
        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>⚠️ Nova Solicitação de Levantamento</h2>
        </div>
        <div class="content">
            <p>Olá Administrador,</p>
            <p>Uma nova solicitação de levantamento foi criada e aguarda sua aprovação.</p>
            
            <div class="info-box">
                <h3>📋 Detalhes da Solicitação</h3>
                <p><strong>Valor Solicitado:</strong> {{amount}}</p>
                <p><strong>Provider:</strong> {{providerName}}</p>
                <p><strong>Email:</strong> {{providerEmail}}</p>
                <p><strong>Método:</strong> {{method}}</p>
                <p><strong>ID da Solicitação:</strong> {{withdrawalId}}</p>
            </div>
            
            <p style="text-align: center; margin-top: 30px;">
                <a href="{{actionUrl}}" class="button">Ver Solicitação</a>
            </p>
            
            <p style="margin-top: 20px; font-size: 14px; color: #666;">
                Por favor, revise a solicitação e aprove ou rejeite conforme apropriado.
            </p>
        </div>
        <div class="footer">
            <p>Este é um email automático. Por favor, não responda.</p>
            <p>&copy; 2026 API Portal - Todos os direitos reservados</p>
        </div>
    </div>
</body>
</html>',
    '["amount", "providerName", "providerEmail", "method", "withdrawalId", "actionUrl"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 3. Template In-App: Levantamento Aprovado (para Provider)
INSERT INTO notification_templates (
    id,
    type,
    channel,
    language,
    subject,
    template,
    variables,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_APPROVED',
    'IN_APP',
    'pt-PT',
    'Levantamento Aprovado',
    'Seu levantamento de {{amount}} foi aprovado! O valor líquido de {{netAmount}} será processado via {{method}}.',
    '["amount", "netAmount", "method", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 4. Template Email: Levantamento Aprovado (para Provider)
INSERT INTO notification_templates (
    id,
    type,
    channel,
    language,
    subject,
    template,
    variables,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_APPROVED',
    'EMAIL',
    'pt-PT',
    'Levantamento Aprovado - Processamento Iniciado',
    '<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
        .success-box { background-color: #d4edda; padding: 15px; margin: 15px 0; border-left: 4px solid #28a745; border-radius: 5px; }
        .info-box { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #17a2b8; }
        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>✅ Levantamento Aprovado</h2>
        </div>
        <div class="content">
            <p>Olá {{providerName}},</p>
            
            <div class="success-box">
                <p style="margin: 0; font-size: 16px;">
                    <strong>Boa notícia!</strong> Seu levantamento foi aprovado e está sendo processado.
                </p>
            </div>
            
            <div class="info-box">
                <h3>💰 Detalhes do Levantamento</h3>
                <p><strong>Valor Solicitado:</strong> {{amount}}</p>
                <p><strong>Taxa:</strong> {{feeAmount}}</p>
                <p><strong>Valor Líquido:</strong> {{netAmount}}</p>
                <p><strong>Método:</strong> {{method}}</p>
                <p><strong>Data de Aprovação:</strong> {{approvedAt}}</p>
            </div>
            
            <p style="margin-top: 20px;">
                O valor de <strong>{{netAmount}}</strong> será transferido para sua conta via <strong>{{method}}</strong> 
                nos próximos dias úteis.
            </p>
            
            <p style="margin-top: 20px; font-size: 14px; color: #666;">
                Você pode acompanhar o status do seu levantamento no painel da carteira.
            </p>
        </div>
        <div class="footer">
            <p>Este é um email automático. Por favor, não responda.</p>
            <p>&copy; 2026 API Portal - Todos os direitos reservados</p>
        </div>
    </div>
</body>
</html>',
    '["providerName", "amount", "feeAmount", "netAmount", "method", "approvedAt", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 5. Template In-App: Levantamento Rejeitado (para Provider)
INSERT INTO notification_templates (
    id,
    type,
    channel,
    language,
    subject,
    template,
    variables,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_REJECTED',
    'IN_APP',
    'pt-PT',
    'Levantamento Rejeitado',
    'Seu levantamento de {{amount}} foi rejeitado. Motivo: {{reason}}. O valor foi devolvido para sua carteira.',
    '["amount", "reason", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- 6. Template Email: Levantamento Rejeitado (para Provider)
INSERT INTO notification_templates (
    id,
    type,
    channel,
    language,
    subject,
    template,
    variables,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_REJECTED',
    'EMAIL',
    'pt-PT',
    'Levantamento Rejeitado - Saldo Devolvido',
    '<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
        .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
        .warning-box { background-color: #f8d7da; padding: 15px; margin: 15px 0; border-left: 4px solid #dc3545; border-radius: 5px; }
        .info-box { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #17a2b8; }
        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h2>❌ Levantamento Rejeitado</h2>
        </div>
        <div class="content">
            <p>Olá {{providerName}},</p>
            
            <div class="warning-box">
                <p style="margin: 0; font-size: 16px;">
                    Infelizmente, seu levantamento foi rejeitado pelo administrador.
                </p>
            </div>
            
            <div class="info-box">
                <h3>📋 Detalhes</h3>
                <p><strong>Valor Solicitado:</strong> {{amount}}</p>
                <p><strong>Método:</strong> {{method}}</p>
                <p><strong>Motivo da Rejeição:</strong></p>
                <p style="background-color: #f8f9fa; padding: 10px; border-radius: 5px;">{{reason}}</p>
            </div>
            
            <p style="margin-top: 20px;">
                O valor de <strong>{{amount}}</strong> foi devolvido para o saldo disponível da sua carteira 
                e você pode solicitar um novo levantamento a qualquer momento.
            </p>
            
            <p style="margin-top: 20px; font-size: 14px; color: #666;">
                Se tiver dúvidas sobre a rejeição, entre em contato com o suporte.
            </p>
        </div>
        <div class="footer">
            <p>Este é um email automático. Por favor, não responda.</p>
            <p>&copy; 2026 API Portal - Todos os direitos reservados</p>
        </div>
    </div>
</body>
</html>',
    '["providerName", "amount", "method", "reason", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- Criar preferências padrão para usuários existentes
DO $
DECLARE
    user_record RECORD;
BEGIN
    FOR user_record IN SELECT id FROM users LOOP
        -- WITHDRAWAL_REQUESTED (para admins)
        INSERT INTO notification_preferences (
            id,
            user_id,
            notification_type,
            in_app_enabled,
            email_enabled,
            created_at,
            updated_at
        ) VALUES (
            gen_random_uuid(),
            user_record.id,
            'WITHDRAWAL_REQUESTED',
            true,
            true,
            NOW(),
            NOW()
        ) ON CONFLICT (user_id, notification_type) DO NOTHING;

        -- WITHDRAWAL_APPROVED (para providers)
        INSERT INTO notification_preferences (
            id,
            user_id,
            notification_type,
            in_app_enabled,
            email_enabled,
            created_at,
            updated_at
        ) VALUES (
            gen_random_uuid(),
            user_record.id,
            'WITHDRAWAL_APPROVED',
            true,
            true,
            NOW(),
            NOW()
        ) ON CONFLICT (user_id, notification_type) DO NOTHING;

        -- WITHDRAWAL_REJECTED (para providers)
        INSERT INTO notification_preferences (
            id,
            user_id,
            notification_type,
            in_app_enabled,
            email_enabled,
            created_at,
            updated_at
        ) VALUES (
            gen_random_uuid(),
            user_record.id,
            'WITHDRAWAL_REJECTED',
            true,
            true,
            NOW(),
            NOW()
        ) ON CONFLICT (user_id, notification_type) DO NOTHING;
    END LOOP;
    
    RAISE NOTICE 'Preferências criadas para % usuários', (SELECT COUNT(*) FROM users);
END $;

-- =====================================================
-- Verificação
-- =====================================================

-- Verificar templates criados
SELECT 
    type,
    channel,
    language,
    subject
FROM notification_templates
WHERE type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
ORDER BY type, channel;

-- Verificar preferências criadas
SELECT 
    COUNT(*) as total_preferences,
    notification_type,
    SUM(CASE WHEN in_app_enabled THEN 1 ELSE 0 END) as in_app_enabled_count,
    SUM(CASE WHEN email_enabled THEN 1 ELSE 0 END) as email_enabled_count
FROM notification_preferences
WHERE notification_type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
GROUP BY notification_type;

-- =====================================================
-- Mensagem de Sucesso
-- =====================================================
DO $
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ TEMPLATES CRIADOS COM SUCESSO!';
    RAISE NOTICE '📧 6 templates: 3 tipos x 2 canais (IN_APP + EMAIL)';
    RAISE NOTICE '⚙️  Preferências configuradas para todos os usuários';
    RAISE NOTICE '========================================';
END $;
