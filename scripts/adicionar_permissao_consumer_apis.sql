-- Adicionar permissão consumer.apis.read
-- Data: 2026-04-03

-- Inserir permissão
INSERT INTO permissions (id, code, name, description, resource, action, active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'consumer.apis.read',
    'Visualizar Minhas APIs',
    'Permite visualizar APIs com subscrição ativa',
    'CONSUMER_APIS',
    'READ',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

-- Associar permissão ao role CONSUMER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CONSUMER'
  AND p.code = 'consumer.apis.read'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Verificar
SELECT 
    r.name as role_name,
    p.code as permission_code,
    p.name as permission_name
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.code = 'consumer.apis.read';

SELECT 'Permissão consumer.apis.read adicionada com sucesso!' as status;
