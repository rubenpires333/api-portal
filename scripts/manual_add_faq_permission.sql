-- Script manual para adicionar permissão de FAQs ao SUPER_ADMIN
-- Use este script se o DataInitializerService não criou automaticamente

-- Passo 1: Verificar se a permissão existe
SELECT id, code, name FROM permissions WHERE code = 'billing.faqs.manage';

-- Se não existir, criar a permissão:
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    'Gerenciar FAQs de Planos',
    'billing.faqs.manage',
    'Permite gerenciar FAQs sobre planos de assinatura',
    'billing',
    'faqs.manage',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'billing.faqs.manage'
);

-- Passo 2: Buscar IDs do SUPER_ADMIN e da permissão billing.manage
SELECT 
    r.id as role_id,
    r.code as role_code,
    p.id as permission_id,
    p.code as permission_code
FROM 
    roles r,
    permissions p
WHERE 
    r.code = 'SUPER_ADMIN'
    AND p.code = 'billing.manage';

-- Passo 3: Adicionar permissão billing.manage ao SUPER_ADMIN (se não existir)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM 
    roles r,
    permissions p
WHERE 
    r.code = 'SUPER_ADMIN'
    AND p.code = 'billing.manage'
    AND NOT EXISTS (
        SELECT 1 
        FROM role_permissions rp 
        WHERE rp.role_id = r.id 
        AND rp.permission_id = p.id
    );

-- Passo 4: Verificar todas as permissões de billing do SUPER_ADMIN
SELECT 
    r.name as role_name,
    r.code as role_code,
    p.name as permission_name,
    p.code as permission_code
FROM 
    roles r
    INNER JOIN role_permissions rp ON r.id = rp.role_id
    INNER JOIN permissions p ON rp.permission_id = p.id
WHERE 
    r.code = 'SUPER_ADMIN'
    AND p.resource = 'billing'
ORDER BY 
    p.code;

-- Passo 5: Verificar total de permissões do SUPER_ADMIN
SELECT 
    r.name,
    r.code,
    COUNT(rp.permission_id) as total_permissions
FROM 
    roles r
    LEFT JOIN role_permissions rp ON r.id = rp.role_id
WHERE 
    r.code = 'SUPER_ADMIN'
GROUP BY 
    r.id, r.name, r.code;
