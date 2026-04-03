-- ============================================================================
-- Script de Teste - Verificar se o sistema está pronto para métricas
-- ============================================================================
-- Execute este script ANTES de popular métricas para verificar pré-requisitos
-- ============================================================================

SELECT '=== VERIFICAÇÃO DE PRÉ-REQUISITOS ===' as status;

-- 1. Verificar se as tabelas existem
SELECT 
    '1. Tabelas de Métricas' as verificacao,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'api_metrics') 
        AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'api_metrics_daily')
        THEN '✅ OK - Tabelas existem'
        ELSE '❌ ERRO - Execute as migrations primeiro'
    END as resultado;

-- 2. Verificar se há APIs publicadas
SELECT 
    '2. APIs Publicadas' as verificacao,
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ OK - ' || COUNT(*) || ' API(s) publicada(s)'
        ELSE '❌ ERRO - Nenhuma API publicada. Publique pelo menos 1 API'
    END as resultado
FROM apis 
WHERE status = 'PUBLISHED';

-- 3. Verificar se há subscriptions ativas
SELECT 
    '3. Subscriptions Ativas' as verificacao,
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ OK - ' || COUNT(*) || ' subscription(s) ativa(s)'
        ELSE '❌ ERRO - Nenhuma subscription ativa. Crie pelo menos 1 subscription'
    END as resultado
FROM subscriptions 
WHERE status = 'ACTIVE';

-- 4. Verificar se há usuários consumers
SELECT 
    '4. Usuários Consumers' as verificacao,
    CASE 
        WHEN COUNT(*) > 0 THEN '✅ OK - ' || COUNT(*) || ' consumer(s) cadastrado(s)'
        ELSE '⚠️  AVISO - Nenhum consumer. Crie usuários com role CONSUMER'
    END as resultado
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE r.code = 'CONSUMER';

-- 5. Listar APIs disponíveis
SELECT '=== APIs DISPONÍVEIS PARA MÉTRICAS ===' as status;

SELECT 
    a.id,
    a.name,
    a.status,
    COUNT(s.id) as subscriptions_ativas
FROM apis a
LEFT JOIN subscriptions s ON s.api_id = a.id AND s.status = 'ACTIVE'
WHERE a.status = 'PUBLISHED'
GROUP BY a.id, a.name, a.status;

-- 6. Listar Subscriptions ativas
SELECT '=== SUBSCRIPTIONS ATIVAS ===' as status;

SELECT 
    s.id as subscription_id,
    a.name as api_name,
    u.name as consumer_name,
    u.email as consumer_email,
    s.status
FROM subscriptions s
JOIN apis a ON a.id = s.api_id
JOIN users u ON u.id = s.consumer_id
WHERE s.status = 'ACTIVE'
LIMIT 10;

-- 7. Verificar métricas existentes
SELECT '=== MÉTRICAS EXISTENTES ===' as status;

SELECT 
    'Métricas individuais' as tipo,
    COUNT(*)::text as quantidade,
    CASE 
        WHEN COUNT(*) = 0 THEN 'Nenhuma métrica. Execute um script de população.'
        ELSE 'Período: ' || MIN(DATE(created_at))::text || ' a ' || MAX(DATE(created_at))::text
    END as info
FROM api_metrics
UNION ALL
SELECT 
    'Métricas diárias',
    COUNT(*)::text,
    CASE 
        WHEN COUNT(*) = 0 THEN 'Nenhuma métrica agregada.'
        ELSE 'Período: ' || MIN(metric_date)::text || ' a ' || MAX(metric_date)::text
    END
FROM api_metrics_daily;

-- 8. Resumo final
SELECT '=== RESUMO ===' as status;

SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM apis WHERE status = 'PUBLISHED') > 0
        AND (SELECT COUNT(*) FROM subscriptions WHERE status = 'ACTIVE') > 0
        THEN '✅ SISTEMA PRONTO! Você pode executar os scripts de população de métricas.'
        ELSE '❌ SISTEMA NÃO ESTÁ PRONTO. Corrija os erros acima antes de popular métricas.'
    END as resultado;

-- 9. Próximos passos
SELECT '=== PRÓXIMOS PASSOS ===' as status;

SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM api_metrics) = 0 THEN 
            'Execute um dos scripts de população:
            - populate_metrics_minimal.sql (rápido, 7 dias)
            - populate_provider_metrics_quick.sql (recomendado, 30 dias)
            - populate_provider_metrics.sql (completo, 30 dias com padrões realistas)'
        ELSE 
            'Métricas já existem! Acesse:
            Login como Provider → Menu Estatísticas'
    END as instrucoes;
