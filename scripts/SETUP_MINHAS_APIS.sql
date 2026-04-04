-- ============================================================================
-- SETUP COMPLETO: Minhas APIs (Consumer)
-- Data: 2026-04-03
-- Descrição: Script completo para configurar a funcionalidade "Minhas APIs"
-- ============================================================================

-- PASSO 1: Corrigir tipo da coluna consumer_id (se ainda não foi feito)
-- ============================================================================
DO $$
BEGIN
    -- Verificar se a coluna já é UUID
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'subscriptions' 
          AND column_name = 'consumer_id' 
          AND data_type = 'character varying'
    ) THEN
        -- Alterar tipo para UUID
        ALTER TABLE subscriptions 
        ALTER COLUMN consumer_id TYPE uuid USING consumer_id::uuid;
        
        RAISE NOTICE 'Coluna consumer_id convertida para UUID com sucesso!';
    ELSE
        RAISE NOTICE 'Coluna consumer_id já é do tipo UUID.';
    END IF;
END $$;

-- PASSO 2: Adicionar permissão consumer.apis.read
-- ============================================================================
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

-- PASSO 3: Associar permissão ao role CONSUMER
-- ============================================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CONSUMER'
  AND p.code = 'consumer.apis.read'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- PASSO 4: Verificar configuração
-- ============================================================================

-- Verificar tipo da coluna consumer_id
SELECT 
    table_name,
    column_name, 
    data_type,
    udt_name
FROM information_schema.columns
WHERE table_name = 'subscriptions' 
  AND column_name = 'consumer_id';

-- Verificar permissão criada
SELECT 
    p.code,
    p.name,
    p.description,
    p.resource,
    p.action,
    p.active
FROM permissions p
WHERE p.code = 'consumer.apis.read';

-- Verificar associação com role CONSUMER
SELECT 
    r.name as role_name,
    p.code as permission_code,
    p.name as permission_name
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.code = 'consumer.apis.read';

-- Verificar subscriptions existentes
SELECT 
    COUNT(*) as total_subscriptions,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_subscriptions,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_subscriptions
FROM subscriptions;

-- ============================================================================
-- RESULTADO ESPERADO
-- ============================================================================
SELECT 
    '✅ Setup completo!' as status,
    'Endpoint: GET /api/v1/consumer/my-apis' as endpoint,
    'Permissão: consumer.apis.read' as permissao,
    'Rota Frontend: /consumer/my-apis' as rota;
