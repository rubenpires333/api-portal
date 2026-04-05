# Script para verificar se o sistema de assinaturas está configurado corretamente

Write-Host "=== VERIFICAÇÃO DO SISTEMA DE ASSINATURAS ===" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar se backend está rodando
Write-Host "1. Verificando Backend..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/billing/health" -Method Get
    Write-Host "   ✅ Backend está rodando" -ForegroundColor Green
    Write-Host "   Gateway ativo: $($health.PSObject.Properties.Name)" -ForegroundColor Gray
} catch {
    Write-Host "   ❌ Backend não está respondendo" -ForegroundColor Red
    Write-Host "   Inicie o backend primeiro!" -ForegroundColor Red
    exit 1
}

# 2. Verificar planos
Write-Host ""
Write-Host "2. Verificando Planos..." -ForegroundColor Yellow
try {
    $plans = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/billing/plans" -Method Get
    Write-Host "   ✅ $($plans.Count) planos encontrados" -ForegroundColor Green
    foreach ($plan in $plans) {
        Write-Host "   - $($plan.displayName): $($plan.currency) $($plan.monthlyPrice)" -ForegroundColor Gray
        if ($plan.stripePriceId -like "prod_*") {
            Write-Host "     ⚠️  Price ID inválido (começa com prod_): $($plan.stripePriceId)" -ForegroundColor Yellow
        } elseif ($plan.stripePriceId -like "price_*") {
            Write-Host "     ✅ Price ID válido: $($plan.stripePriceId)" -ForegroundColor Green
        } else {
            Write-Host "     ❌ Price ID ausente ou inválido" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "   ❌ Erro ao buscar planos: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Verificar configuração do Stripe
Write-Host ""
Write-Host "3. Verificando Configuração Stripe..." -ForegroundColor Yellow
try {
    $config = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/billing/config" -Method Get
    if ($config.publishableKey) {
        Write-Host "   ✅ Publishable Key configurada: $($config.publishableKey.Substring(0, 20))..." -ForegroundColor Green
    } else {
        Write-Host "   ❌ Publishable Key não configurada" -ForegroundColor Red
    }
} catch {
    Write-Host "   ❌ Erro ao buscar configuração: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Verificar banco de dados
Write-Host ""
Write-Host "4. Verificando Banco de Dados..." -ForegroundColor Yellow
Write-Host "   Execute manualmente:" -ForegroundColor Gray
Write-Host "   psql -U apiportal -d db_portal_api -f scripts/check_subscriptions.sql" -ForegroundColor Cyan

# 5. Instruções finais
Write-Host ""
Write-Host "=== PRÓXIMOS PASSOS ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para testar o fluxo completo:" -ForegroundColor White
Write-Host "1. Acesse: http://localhost:4200/provider/plans" -ForegroundColor Gray
Write-Host "2. Escolha um plano e clique em 'Assinar Plano'" -ForegroundColor Gray
Write-Host "3. Use cartão de teste: 4242 4242 4242 4242" -ForegroundColor Gray
Write-Host "4. Após pagamento, verifique os logs do backend" -ForegroundColor Gray
Write-Host "5. Execute: scripts/check_subscriptions.sql para ver a assinatura criada" -ForegroundColor Gray
Write-Host ""
Write-Host "Documentação completa: TEST_SUBSCRIPTION_FLOW.md" -ForegroundColor Cyan
Write-Host ""
