-- SCRIPT RÁPIDO: Adicionar permissão billing.manage ao SUPER_ADMIN
-- Execute este script se estiver recebendo erro 403

-- Adicionar permissão billing.manage ao SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT 
    r.id,
    p.id
FROM 
    roles r
    CROSS JOIN permissions p
WHERE 
    r.code = 'SUPER_ADMIN'
    AND p.code = 'billing.manage'
    AND NOT EXISTS (
        SELECT 1 
        FROM role_permissions rp 
        WHERE rp.role_id = r.id 
        AND rp.permission_id = p.id
    );

-- Verificar resultado
SELECT 
    'Permissão adicionada com sucesso!' as status,
    r.name as role,
    p.name as permission
FROM 
    roles r
    INNER JOIN role_permissions rp ON r.id = rp.role_id
    INNER JOIN permissions p ON rp.permission_id = p.id
WHERE 
    r.code = 'SUPER_ADMIN'
    AND p.code = 'billing.manage';
