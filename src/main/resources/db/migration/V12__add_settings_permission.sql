-- Adicionar permissão para gerenciar configurações
INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Gerenciar Configurações',
    'settings.manage',
    'Permite gerenciar configurações da plataforma (termos, políticas, etc)',
    'settings',
    'manage',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (code) DO NOTHING;

-- Associar permissão à role SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' 
  AND p.code = 'settings.manage'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
