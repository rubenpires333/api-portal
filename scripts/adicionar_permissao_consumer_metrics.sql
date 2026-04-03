-- ============================================
-- Script: Adicionar Permissão consumer.metrics.read
-- Descrição: Adiciona permissão de métricas para CONSUMER
-- Data: 2026-04-03
-- ============================================

-- 1. Verificar se a permissão já existe
DO $$
DECLARE
    v_permission_id UUID;
    v_role_id UUID;
BEGIN
    -- Buscar ou criar a permissão
    SELECT id INTO v_permission_id FROM permissions WHERE code = 'consumer.metrics.read';
    
    IF v_permission_id IS NULL THEN
        INSERT INTO permissions (id, name, code, description, resource, action, active, created_at, updated_at)
        VALUES (
            gen_random_uuid(),
            'Ler Métricas do Consumer',
            'consumer.metrics.read',
            'Permite visualizar métricas de uso das APIs',
            'consumer',
            'metrics.read',
            true,
            NOW(),
            NOW()
        )
        RETURNING id INTO v_permission_id;
        
        RAISE NOTICE '✓ Permissão consumer.metrics.read criada com sucesso';
    ELSE
        RAISE NOTICE '✓ Permissão consumer.metrics.read já existe';
    END IF;
    
    -- Buscar o role CONSUMER
    SELECT id INTO v_role_id FROM roles WHERE code = 'CONSUMER';
    
    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Role CONSUMER não encontrado!';
    END IF;
    
    -- Verificar se a permissão já está associada ao role
    IF NOT EXISTS (
        SELECT 1 FROM role_permissions 
        WHERE role_id = v_role_id AND permission_id = v_permission_id
    ) THEN
        INSERT INTO role_permissions (role_id, permission_id)
        VALUES (v_role_id, v_permission_id);
        
        RAISE NOTICE '✓ Permissão consumer.metrics.read adicionada ao role CONSUMER';
    ELSE
        RAISE NOTICE '✓ Permissão consumer.metrics.read já está no role CONSUMER';
    END IF;
    
    RAISE NOTICE '✓ Script executado com sucesso!';
END $$;

-- 2. Verificar resultado
SELECT 
    r.name as role_name,
    r.code as role_code,
    COUNT(rp.permission_id) as total_permissions
FROM roles r
LEFT JOIN role_permissions rp ON r.id = rp.role_id
WHERE r.code = 'CONSUMER'
GROUP BY r.id, r.name, r.code;

-- 3. Listar permissões do CONSUMER
SELECT 
    p.name as permission_name,
    p.code as permission_code,
    p.description
FROM permissions p
INNER JOIN role_permissions rp ON p.id = rp.permission_id
INNER JOIN roles r ON rp.role_id = r.id
WHERE r.code = 'CONSUMER'
ORDER BY p.code;
