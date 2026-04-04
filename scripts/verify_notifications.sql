-- =====================================================
-- Verificação de Notificações de Levantamento
-- =====================================================

-- 1. Verificar notificações criadas recentemente
SELECT 
    '=== NOTIFICAÇÕES RECENTES ===' as info;

SELECT 
    n.id,
    n.type,
    n.title,
    n.is_read,
    n.created_at,
    u.email as user_email,
    u.username
FROM notifications n
JOIN users u ON n.user_id = u.id::text
WHERE n.type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
ORDER BY n.created_at DESC
LIMIT 10;

-- 2. Verificar preferências por usuário
SELECT 
    '=== PREFERÊNCIAS POR USUÁRIO ===' as info;

SELECT 
    u.username,
    u.email,
    np.notification_type,
    np.in_app_enabled,
    np.email_enabled
FROM notification_preferences np
JOIN users u ON np.user_id = u.id::text
WHERE np.notification_type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
ORDER BY u.username, np.notification_type;

-- 3. Contar notificações não lidas por usuário
SELECT 
    '=== NOTIFICAÇÕES NÃO LIDAS ===' as info;

SELECT 
    u.username,
    u.email,
    COUNT(*) as unread_count
FROM notifications n
JOIN users u ON n.user_id = u.id::text
WHERE n.is_read = false
AND n.type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
GROUP BY u.username, u.email
ORDER BY unread_count DESC;

-- 4. Verificar templates disponíveis
SELECT 
    '=== TEMPLATES DISPONÍVEIS ===' as info;

SELECT 
    type,
    channel,
    language,
    subject
FROM notification_templates
WHERE type IN ('WITHDRAWAL_REQUESTED', 'WITHDRAWAL_APPROVED', 'WITHDRAWAL_REJECTED')
ORDER BY type, channel;

-- 5. Verificar levantamentos e suas notificações
SELECT 
    '=== LEVANTAMENTOS E NOTIFICAÇÕES ===' as info;

SELECT 
    wr.id as withdrawal_id,
    wr.status,
    wr.requested_amount,
    wr.requested_at,
    COUNT(n.id) as notification_count
FROM withdrawal_requests wr
LEFT JOIN notifications n ON n.data::text LIKE '%' || wr.id::text || '%'
GROUP BY wr.id, wr.status, wr.requested_amount, wr.requested_at
ORDER BY wr.requested_at DESC
LIMIT 10;
