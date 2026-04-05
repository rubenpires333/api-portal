-- Script simplificado para adicionar permissão de gerenciamento de FAQs de Planos
-- NOTA: O DataInitializerService já adiciona automaticamente esta permissão ao SUPER_ADMIN
-- Este script é apenas para casos onde você precisa adicionar manualmente

-- 1. Inserir a permissão (se não existir)
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

-- 2. Associar a permissão ao role SUPER_ADMIN (se necessário)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM 
    roles r,
    permissions p
WHERE 
    r.code = 'SUPER_ADMIN'
    AND p.code = 'billing.faqs.manage'
    AND NOT EXISTS (
        SELECT 1 
        FROM role_permissions rp 
        WHERE rp.role_id = r.id 
        AND rp.permission_id = p.id
    );

-- 3. Verificar resultado
SELECT 
    'Permissão billing.faqs.manage adicionada com sucesso!' as status,
    COUNT(*) as total_super_admin_permissions
FROM 
    role_permissions rp
    INNER JOIN roles r ON rp.role_id = r.id
WHERE 
    r.code = 'SUPER_ADMIN';
