-- =====================================================
-- SETUP COMPLETO DE NOTIFICAÇÕES DE LEVANTAMENTO
-- Execute este script para configurar tudo de uma vez
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'INICIANDO SETUP DE NOTIFICAÇÕES';
    RAISE NOTICE '=========================================';
    RAISE NOTICE '';
    RAISE NOTICE 'PASSO 1: Atualizando constraints...';
END $$;

-- PASSO 1: Atualizar Constraints

DO $$
BEGIN
    -- 1.1 Constraint de notifications
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

    -- 1.2 Constraint de notification_templates
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

    -- 1.3 Constraint de notification_preferences
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

-- PASSO 2: Criar Templates
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE 'PASSO 2: Criando templates de notificação...';
END $$;

-- Template IN_APP: WITHDRAWAL_REQUESTED
INSERT INTO notification_templates (
    id, type, channel, language, subject, template, variables, created_at, updated_at
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

-- Template EMAIL: WITHDRAWAL_REQUESTED
INSERT INTO notification_templates (
    id, type, channel, language, subject, template, variables, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_REQUESTED',
    'EMAIL',
    'pt-PT',
    'Nova Solicitação de Levantamento - Aprovação Necessária',
    '<html><body><h2>Nova Solicitação de Levantamento</h2><p>Valor: {{amount}}</p><p>Provider: {{providerName}} ({{providerEmail}})</p><p>Método: {{method}}</p></body></html>',
    '["amount", "providerName", "providerEmail", "method", "withdrawalId", "actionUrl"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- Template IN_APP: WITHDRAWAL_APPROVED
INSERT INTO notification_templates (
    id, type, channel, language, subject, template, variables, created_at, updated_at
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

-- Template EMAIL: WITHDRAWAL_APPROVED
INSERT INTO notification_templates (
    id, type, channel, language, subject, template, variables, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_APPROVED',
    'EMAIL',
    'pt-PT',
    'Levantamento Aprovado - Processamento Iniciado',
    '<html><body><h2>Levantamento Aprovado</h2><p>Valor: {{amount}}</p><p>Valor Líquido: {{netAmount}}</p><p>Método: {{method}}</p></body></html>',
    '["providerName", "amount", "feeAmount", "netAmount", "method", "approvedAt", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- Template IN_APP: WITHDRAWAL_REJECTED
INSERT INTO notification_templates (
    id, type, channel, language, subject, template, variables, created_at, updated_at
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

-- Template EMAIL: WITHDRAWAL_REJECTED
INSERT INTO notification_templates (
    id, type, channel, language, subject, template, variables, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'WITHDRAWAL_REJECTED',
    'EMAIL',
    'pt-PT',
    'Levantamento Rejeitado - Saldo Devolvido',
    '<html><body><h2>Levantamento Rejeitado</h2><p>Valor: {{amount}}</p><p>Motivo: {{reason}}</p><p>O valor foi devolvido para sua carteira.</p></body></html>',
    '["providerName", "amount", "method", "reason", "withdrawalId"]',
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- PASSO 3: Criar Preferências para Todos os Usuários
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE 'PASSO 3: Criando preferências para usuários...';
END $$;

DO $$
DECLARE
    user_record RECORD;
    created_count INTEGER := 0;
BEGIN
    FOR user_record IN SELECT id FROM users LOOP
        -- WITHDRAWAL_REQUESTED
        INSERT INTO notification_preferences (
            id, user_id, notification_type, in_app_enabled, email_enabled, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), user_record.id, 'WITHDRAWAL_REQUESTED', true, true, NOW(), NOW()
        ) ON CONFLICT (user_id, notification_type) DO NOTHING;
        
        GET DIAGNOSTICS created_count = ROW_COUNT;

        -- WITHDRAWAL_APPROVED
        INSERT INTO notification_preferences (
            id, user_id, notification_type, in_app_enabled, email_enabled, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), user_record.id, 'WITHDRAWAL_APPROVED', true, true, NOW(), NOW()
        ) ON CONFLICT (user_id, notification_type) DO NOTHING;

        -- WITHDRAWAL_REJECTED
        INSERT INTO notification_preferences (
            id, user_id, notification_type, in_app_enabled, email_enabled, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), user_record.id, 'WITHDRAWAL_REJECTED', true, true, NOW(), NOW()
        ) ON CONFLICT (user_id, notification_type) DO NOTHING;
    END LOOP;
    
    RAISE NOTICE '✓ Preferências criadas para % usuários', (SELECT COUNT(*) FROM users);
END $$;

-- PASSO 4: Verificação
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE 'PASSO 4: Verificando instalação...';
    RAISE NOTICE '';
END $$;

-- Templates criados
DO $$
DECLARE
    template_record RECORD;
BEGIN
    RAISE NOTICE 'Templates criados:';
    FOR template_record IN 
        SELECT type, channel, subject
        FROM notification_templates
        WHERE type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
        ORDER BY type, channel
    LOOP
        RAISE NOTICE '  % | % | %', template_record.type, template_record.channel, template_record.subject;
    END LOOP;
END $$;

-- Preferências por tipo
DO $$
DECLARE
    pref_record RECORD;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE 'Preferências por tipo:';
    FOR pref_record IN 
        SELECT 
            notification_type,
            COUNT(*) as total_users,
            SUM(CASE WHEN in_app_enabled THEN 1 ELSE 0 END) as in_app_enabled,
            SUM(CASE WHEN email_enabled THEN 1 ELSE 0 END) as email_enabled
        FROM notification_preferences
        WHERE notification_type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
        GROUP BY notification_type
        ORDER BY notification_type
    LOOP
        RAISE NOTICE '  % | Total: % | In-App: % | Email: %', 
            pref_record.notification_type, 
            pref_record.total_users, 
            pref_record.in_app_enabled, 
            pref_record.email_enabled;
    END LOOP;
END $$;

-- Mensagem final
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '=========================================';
    RAISE NOTICE '✅ SETUP CONCLUÍDO COM SUCESSO!';
    RAISE NOTICE '=========================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Próximos passos:';
    RAISE NOTICE '1. Reiniciar o backend';
    RAISE NOTICE '2. Limpar cache do frontend';
    RAISE NOTICE '3. Testar criando um levantamento';
    RAISE NOTICE '4. Verificar notificações no sino';
    RAISE NOTICE '';
END $$;
