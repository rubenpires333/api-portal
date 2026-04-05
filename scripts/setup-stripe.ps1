# =====================================================
# Script de Setup Automático do Stripe
# =====================================================

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  SETUP AUTOMÁTICO DO STRIPE" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se o Stripe CLI está instalado
Write-Host "Verificando Stripe CLI..." -ForegroundColor Yellow
$stripeCli = Get-Command stripe -ErrorAction SilentlyContinue

if (-not $stripeCli) {
    Write-Host "❌ Stripe CLI não encontrado!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Instale o Stripe CLI:" -ForegroundColor Yellow
    Write-Host "  1. Via Scoop: scoop install stripe" -ForegroundColor White
    Write-Host "  2. Ou baixe de: https://github.com/stripe/stripe-cli/releases" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host "✅ Stripe CLI encontrado" -ForegroundColor Green
Write-Host ""

# Solicitar API Key
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  CONFIGURAÇÃO DAS CHAVES" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Obtenha suas chaves em: https://dashboard.stripe.com/test/apikeys" -ForegroundColor Yellow
Write-Host ""

$stripeApiKey = Read-Host "Cole sua Stripe Secret Key (sk_test_...)"
$stripePublishableKey = Read-Host "Cole sua Stripe Publishable Key (pk_test_...)"

if (-not $stripeApiKey -or -not $stripePublishableKey) {
    Write-Host "❌ Chaves não fornecidas!" -ForegroundColor Red
    exit 1
}

# Criar produtos no Stripe
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  CRIANDO PRODUTOS NO STRIPE" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Fazer login no Stripe
Write-Host "Fazendo login no Stripe..." -ForegroundColor Yellow
stripe login

# Criar Starter Plan
Write-Host ""
Write-Host "Criando Starter Plan (Free)..." -ForegroundColor Yellow
$starterProduct = stripe products create --name="Starter Plan" --description="Free plan for testing APIs" | ConvertFrom-Json
$starterPriceId = (stripe prices create --product=$starterProduct.id --unit-amount=0 --currency=usd --recurring[interval]=month | ConvertFrom-Json).id
Write-Host "✅ Starter Plan criado: $starterPriceId" -ForegroundColor Green

# Criar Growth Plan
Write-Host ""
Write-Host "Criando Growth Plan ($49/mês)..." -ForegroundColor Yellow
$growthProduct = stripe products create --name="Growth Plan" --description="Professional plan with advanced features" | ConvertFrom-Json
$growthPriceId = (stripe prices create --product=$growthProduct.id --unit-amount=4900 --currency=usd --recurring[interval]=month | ConvertFrom-Json).id
Write-Host "✅ Growth Plan criado: $growthPriceId" -ForegroundColor Green

# Criar Business Plan
Write-Host ""
Write-Host "Criando Business Plan ($149/mês)..." -ForegroundColor Yellow
$businessProduct = stripe products create --name="Business Plan" --description="Enterprise plan with unlimited features" | ConvertFrom-Json
$businessPriceId = (stripe prices create --product=$businessProduct.id --unit-amount=14900 --currency=usd --recurring[interval]=month | ConvertFrom-Json).id
Write-Host "✅ Business Plan criado: $businessPriceId" -ForegroundColor Green

# Atualizar .env
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  ATUALIZANDO .ENV" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$envPath = ".env"
$envContent = @"

# =====================================================
# Stripe Configuration (Auto-generated)
# =====================================================
STRIPE_API_KEY=$stripeApiKey
STRIPE_PUBLISHABLE_KEY=$stripePublishableKey
STRIPE_WEBHOOK_SECRET=whsec_CONFIGURE_DEPOIS

# Stripe Price IDs
STRIPE_PRICE_ID_STARTER=$starterPriceId
STRIPE_PRICE_ID_GROWTH=$growthPriceId
STRIPE_PRICE_ID_BUSINESS=$businessPriceId
"@

Add-Content -Path $envPath -Value $envContent
Write-Host "✅ .env atualizado" -ForegroundColor Green

# Criar script SQL para atualizar banco
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  CRIANDO SCRIPT SQL" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$sqlPath = "scripts/update_stripe_price_ids.sql"
$sqlContent = @"
-- =====================================================
-- Atualizar Price IDs do Stripe
-- Auto-gerado em $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
-- =====================================================

-- Atualizar Starter Plan
UPDATE platform_plans 
SET stripe_price_id = '$starterPriceId'
WHERE name = 'STARTER';

-- Atualizar Growth Plan
UPDATE platform_plans 
SET stripe_price_id = '$growthPriceId'
WHERE name = 'GROWTH';

-- Atualizar Business Plan
UPDATE platform_plans 
SET stripe_price_id = '$businessPriceId'
WHERE name = 'BUSINESS';

-- Verificar
SELECT 
    name,
    display_name,
    monthly_price,
    stripe_price_id,
    CASE 
        WHEN stripe_price_id IS NOT NULL THEN '✅ Configurado'
        ELSE '❌ Faltando'
    END as status
FROM platform_plans
ORDER BY monthly_price;

-- Mensagem
DO `$`$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE '✅ PRICE IDS ATUALIZADOS COM SUCESSO!';
    RAISE NOTICE '=========================================';
END `$`$;
"@

Set-Content -Path $sqlPath -Value $sqlContent
Write-Host "✅ Script SQL criado: $sqlPath" -ForegroundColor Green

# Executar SQL
Write-Host ""
$executeSql = Read-Host "Deseja executar o script SQL agora? (s/n)"

if ($executeSql -eq "s" -or $executeSql -eq "S") {
    Write-Host ""
    Write-Host "Executando script SQL..." -ForegroundColor Yellow
    
    $dbUser = Read-Host "Usuário do PostgreSQL (padrão: apiportal)"
    if (-not $dbUser) { $dbUser = "apiportal" }
    
    $dbName = Read-Host "Nome do banco (padrão: db_portal_api)"
    if (-not $dbName) { $dbName = "db_portal_api" }
    
    psql -U $dbUser -d $dbName -f $sqlPath
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Script SQL executado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Erro ao executar script SQL" -ForegroundColor Red
        Write-Host "Execute manualmente: psql -U $dbUser -d $dbName -f $sqlPath" -ForegroundColor Yellow
    }
}

# Instruções finais
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  PRÓXIMOS PASSOS" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Configure o webhook:" -ForegroundColor Yellow
Write-Host "   stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe" -ForegroundColor White
Write-Host ""
Write-Host "2. Copie o webhook secret (whsec_...) e atualize no .env" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. Reinicie o backend:" -ForegroundColor Yellow
Write-Host "   mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "4. Teste o health check:" -ForegroundColor Yellow
Write-Host "   curl http://localhost:8080/api/v1/billing/health" -ForegroundColor White
Write-Host ""
Write-Host "5. Leia o guia completo:" -ForegroundColor Yellow
Write-Host "   STRIPE_INTEGRATION_COMPLETE_GUIDE.md" -ForegroundColor White
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  ✅ SETUP CONCLUÍDO!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
