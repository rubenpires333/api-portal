-- =====================================================
-- Script para Corrigir Notificações de Levantamento
-- =====================================================

-- PASSO 1: Verificar se os templates existem
SELECT 
    'Templates existentes:' as info,
    type,
    channel,
    subject
FROM notification_templates
WHERE type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
ORDER BY type, channel;

-- PASSO 2: Verificar preferências existentes
SELECT 
    'Preferências existentes:' as info,
    COUNT(*) as total,
    notification_type
FROM notification_preferences
WHERE notification_type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
GROUP BY notification_type;

-- PASSO 3: Criar preferências para usuários que não têm
DO $$
DECLARE
    user_record RECORD;
    created_count INTEGER := 0;
BEGIN
    FOR user_record IN SELECT id FROM users LOOP
        -- WITHDRAWAL_REQUESTED
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
        
        IF FOUND THEN
            created_count := created_count + 1;
        END IF;

        -- WITHDRAWAL_APPROVED
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
        
        IF FOUND THEN
            created_count := created_count + 1;
        END IF;

        -- WITHDRAWAL_REJECTED
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
        
        IF FOUND THEN
            created_count := created_count + 1;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Preferências criadas: %', created_count;
END $$;

-- PASSO 4: Verificar notificações criadas
SELECT 
    'Notificações de levantamento:' as info,
    COUNT(*) as total,
    type,
    is_read
FROM notifications
WHERE type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
GROUP BY type, is_read
ORDER BY type, is_read;

-- PASSO 5: Verificar se há levantamentos pendentes sem notificação
SELECT 
    'Levantamentos sem notificação:' as info,
    wr.id,
    wr.requested_amount,
    wr.status,
    wr.requested_at,
    pw.provider_id
FROM withdrawal_requests wr
JOIN provider_wallets pw ON wr.wallet_id = pw.id
WHERE wr.status = 'PENDING_APPROVAL'
AND NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.type = 'WITHDRAWAL_REQUESTED'
    AND n.data::text LIKE '%' || wr.id::text || '%'
);

-- =====================================================
-- Mensagem de Sucesso
-- =====================================================
DO $$
BEGIN
    RAISE NOTICE '✅ Verificação e correção concluída!';
    RAISE NOTICE '📋 Verifique os resultados acima';
END $$;
