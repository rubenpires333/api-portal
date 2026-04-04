-- ============================================================================
-- VERIFICAR PERMISSÕES DO CONSUMER
-- Data: 2026-04-03
-- Descrição: Script para verificar se as permissões estão corretamente configuradas
-- ============================================================================

-- 1. Verificar se a permissão existe
SELECT 
    '1. PERMISSÃO' as verificacao,
    p.id,
    p.code,
    p.name,
    p.active
FROM permissions p
WHERE p.code = 'consumer.apis.read';

-- 2. Verificar se o role CONSUMER existe
SELECT 
    '2. ROLE CONSUMER' as verificacao,
    r.id,
    r.code,
    r.name,
    r.active
FROM roles r
WHERE r.code = 'CONSUMER';

-- 3. Verificar associação role-permission
SELECT 
    '3. ASSOCIAÇÃO ROLE-PERMISSION' as verificacao,
    r.code as role_code,
    r.name as role_name,
    p.code as permission_code,
    p.name as permission_name
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.code = 'consumer.apis.read';

-- 4. Verificar usuários com role CONSUMER
SELECT 
    '4. USUÁRIOS COM ROLE CONSUMER' as verificacao,
    u.id,
    u.email,
    u.name,
    u.keycloak_id,
    r.code as role_code
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE r.code = 'CONSUMER'
ORDER BY u.email;

-- 5. Verificar TODAS as permissões de um usuário CONSUMER específico
-- (substitua o email pelo seu usuário de teste)
SELECT 
    '5. PERMISSÕES DO USUÁRIO' as verificacao,
    u.email,
    r.code as role_code,
    p.code as permission_code,
    p.name as permission_name,
    p.active as permission_active
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
JOIN role_permissions rp ON rp.role_id = r.id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'CONSUMER'
  AND p.code LIKE 'consumer.%'
ORDER BY u.email, p.code;

-- 6. Contar permissões por role
SELECT 
    '6. TOTAL DE PERMISSÕES POR ROLE' as verificacao,
    r.code as role_code,
    r.name as role_name,
    COUNT(p.id) as total_permissions
FROM roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.id
LEFT JOIN permissions p ON p.id = rp.permission_id
WHERE r.code IN ('CONSUMER', 'PROVIDER', 'SUPER_ADMIN')
GROUP BY r.id, r.code, r.name
ORDER BY r.code;

-- ============================================================================
-- RESULTADO ESPERADO:
-- 1. Permissão 'consumer.apis.read' deve existir e estar ativa
-- 2. Role 'CONSUMER' deve existir e estar ativa
-- 3. Deve haver associação entre CONSUMER e consumer.apis.read
-- 4. Deve listar os usuários com role CONSUMER
-- 5. Deve mostrar todas as permissões consumer.* do usuário
-- 6. Role CONSUMER deve ter pelo menos 2 permissões (consumer.apis.read e consumer.metrics.read)
-- ============================================================================
