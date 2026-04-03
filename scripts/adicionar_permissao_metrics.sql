-- ============================================================================
-- Script para Adicionar Permissão provider.metrics.read
-- ============================================================================
-- Execute no DBeaver/pgAdmin antes de acessar o Dashboard
-- ============================================================================

-- 1. Verificar se a permissão já existe
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM permissions WHERE code = 'provider.metrics.read')
        THEN '✅ Permissão já existe'
        ELSE '⚠️  Permissão não existe - será criada'
    END as status;

-- 2. Criar a permissão se não existir
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    'Ler Métricas do Provider',
    'provider.metrics.read',
    'Permite visualizar métricas das APIs do provider',
    'provider',
    'metrics.read',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'provider.metrics.read'
);

SELECT '✅ Permissão criada/verificada' as status;

-- 3. Verificar role PROVIDER
SELECT 
    'Role PROVIDER: ' || 
    CASE 
        WHEN EXISTS (SELECT 1 FROM roles WHERE code = 'PROVIDER')
        THEN 'Existe'
        ELSE 'NÃO EXISTE!'
    END as status;

-- 4. Adicionar permissão ao role PROVIDER
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM 
    roles r,
    permissions p
WHERE 
    r.code = 'PROVIDER'
    AND p.code = 'provider.metrics.read'
    AND NOT EXISTS (
        SELECT 1 
        FROM role_permissions rp
        WHERE rp.role_id = r.id 
        AND rp.permission_id = p.id
    );

SELECT '✅ Permissão atribuída ao role PROVIDER' as status;

-- 5. Verificar permissões do role PROVIDER
SELECT 
    '=== PERMISSÕES DO ROLE PROVIDER ===' as info;

SELECT 
    p.code,
    p.name,
    p.description
FROM 
    role_permissions rp
    JOIN roles r ON r.id = rp.role_id
    JOIN permissions p ON p.id = rp.permission_id
WHERE 
    r.code = 'PROVIDER'
ORDER BY p.code;

-- 6. Verificar se o usuário atual tem a permissão
SELECT 
    '=== VERIFICAR SEU USUÁRIO ===' as info;

SELECT 
    u.email,
    r.code as role,
    COUNT(DISTINCT p.id) as total_permissions,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM user_roles ur2
            JOIN role_permissions rp2 ON rp2.role_id = ur2.role_id
            JOIN permissions p2 ON p2.id = rp2.permission_id
            WHERE ur2.user_id = u.id 
            AND p2.code = 'provider.metrics.read'
        )
        THEN '✅ TEM permissão provider.metrics.read'
        ELSE '❌ NÃO TEM permissão provider.metrics.read'
    END as tem_permissao
FROM 
    users u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON r.id = ur.role_id
    LEFT JOIN role_permissions rp ON rp.role_id = r.id
    LEFT JOIN permissions p ON p.id = rp.permission_id
WHERE 
    r.code = 'PROVIDER'
GROUP BY u.id, u.email, r.code
ORDER BY u.email;

SELECT '✅ CONCLUÍDO! Faça logout e login novamente para aplicar as permissões.' as status;
