# API Portal - Billing Setup Script (PowerShell)
# Este script automatiza a configuração inicial do sistema de billing

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "API Portal - Billing Setup" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Variáveis
$BaseUrl = "http://localhost:8080/api/v1"
$Token = ""
$AdminId = ""

# Função para fazer login
function Login {
    Write-Host "[1/7] Fazendo login como SUPERADMIN..." -ForegroundColor Yellow
    $AdminEmail = Read-Host "Email do SUPERADMIN"
    $AdminPassword = Read-Host "Senha" -AsSecureString
    $AdminPasswordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($AdminPassword))
    
    $Body = @{
        email = $AdminEmail
        password = $AdminPasswordPlain
    } | ConvertTo-Json
    
    try {
        $Response = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
            -Method Post `
            -ContentType "application/json" `
            -Body $Body
        
        $script:Token = $Response.accessToken
        $script:AdminId = $Response.userId
        
        Write-Host "✓ Login realizado com sucesso!" -ForegroundColor Green
        Write-Host ""
    }
    catch {
        Write-Host "❌ Erro ao fazer login. Verifique as credenciais." -ForegroundColor Red
        exit 1
    }
}

# Função para criar gateway Stripe
function Create-Gateway {
    Write-Host "[2/7] Criando configuração do Gateway Stripe..." -ForegroundColor Yellow
    
    $StripeKey = Read-Host "Stripe API Key (pk_test_...)"
    $WebhookSecret = Read-Host "Stripe Webhook Secret (whsec_...)"
    
    $Body = @{
        gatewayType = "STRIPE"
        apiKey = $StripeKey
        webhookSecret = $WebhookSecret
        active = $true
        testMode = $true
    } | ConvertTo-Json
    
    try {
        $Headers = @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }
        
        $Response = Invoke-RestMethod -Uri "$BaseUrl/admin/billing/gateways" `
            -Method Post `
            -Headers $Headers `
            -Body $Body
        
        Write-Host "✓ Gateway Stripe criado com sucesso!" -ForegroundColor Green
        Write-Host ""
    }
    catch {
        Write-Host "❌ Erro ao criar gateway: $_" -ForegroundColor Red
        exit 1
    }
}

# Função para ativar gateway
function Activate-Gateway {
    Write-Host "[3/7] Ativando Gateway Stripe..." -ForegroundColor Yellow
    
    $Headers = @{
        "Authorization" = "Bearer $Token"
    }
    
    try {
        Invoke-RestMethod -Uri "$BaseUrl/admin/billing/gateways/STRIPE/activate" `
            -Method Post `
            -Headers $Headers | Out-Null
        
        Write-Host "✓ Gateway ativado!" -ForegroundColor Green
        Write-Host ""
    }
    catch {
        Write-Host "❌ Erro ao ativar gateway: $_" -ForegroundColor Red
        exit 1
    }
}

# Função para criar planos
function Create-Plans {
    Write-Host "[4/7] Criando planos da plataforma..." -ForegroundColor Yellow
    
    $PriceStarter = Read-Host "Stripe Price ID - Starter (price_...)"
    $PriceGrowth = Read-Host "Stripe Price ID - Growth (price_...)"
    $PriceBusiness = Read-Host "Stripe Price ID - Business (price_...)"
    
    $Headers = @{
        "Authorization" = "Bearer $Token"
        "Content-Type" = "application/json"
    }
    
    # Criar Starter Plan
    $StarterBody = @{
        name = "STARTER"
        displayName = "Starter Plan"
        description = "Perfect for testing and small projects"
        monthlyPrice = 0.00
        currency = "USD"
        maxApis = 3
        maxRequestsPerMonth = 10000
        maxTeamMembers = 1
        customDomain = $false
        prioritySupport = $false
        advancedAnalytics = $false
        stripePriceId = $PriceStarter
        active = $true
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$BaseUrl/admin/billing/plans" `
        -Method Post `
        -Headers $Headers `
        -Body $StarterBody | Out-Null
    
    Write-Host "  ✓ Starter Plan criado" -ForegroundColor Green
    
    # Criar Growth Plan
    $GrowthBody = @{
        name = "GROWTH"
        displayName = "Growth Plan"
        description = "For growing businesses and teams"
        monthlyPrice = 49.00
        currency = "USD"
        maxApis = 10
        maxRequestsPerMonth = 100000
        maxTeamMembers = 5
        customDomain = $true
        prioritySupport = $true
        advancedAnalytics = $true
        stripePriceId = $PriceGrowth
        active = $true
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$BaseUrl/admin/billing/plans" `
        -Method Post `
        -Headers $Headers `
        -Body $GrowthBody | Out-Null
    
    Write-Host "  ✓ Growth Plan criado" -ForegroundColor Green
    
    # Criar Business Plan
    $BusinessBody = @{
        name = "BUSINESS"
        displayName = "Business Plan"
        description = "Enterprise-grade features and support"
        monthlyPrice = 149.00
        currency = "USD"
        maxApis = -1
        maxRequestsPerMonth = -1
        maxTeamMembers = -1
        customDomain = $true
        prioritySupport = $true
        advancedAnalytics = $true
        stripePriceId = $PriceBusiness
        active = $true
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$BaseUrl/admin/billing/plans" `
        -Method Post `
        -Headers $Headers `
        -Body $BusinessBody | Out-Null
    
    Write-Host "  ✓ Business Plan criado" -ForegroundColor Green
    Write-Host ""
}

# Função para criar regras de taxas
function Create-FeeRules {
    Write-Host "[5/7] Criando regras de taxas de levantamento..." -ForegroundColor Yellow
    
    $Headers = @{
        "Authorization" = "Bearer $Token"
        "Content-Type" = "application/json"
    }
    
    # Bank Transfer
    $BankBody = @{
        withdrawalMethod = "BANK_TRANSFER"
        fixedFee = 2.50
        percentageFee = 1.00
        minAmount = 10.00
        maxAmount = 10000.00
        currency = "USD"
        active = $true
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$BaseUrl/admin/billing/fee-rules?adminId=$AdminId" `
        -Method Post `
        -Headers $Headers `
        -Body $BankBody | Out-Null
    
    Write-Host "  ✓ Regra Bank Transfer criada" -ForegroundColor Green
    
    # PayPal
    $PayPalBody = @{
        withdrawalMethod = "PAYPAL"
        fixedFee = 0.50
        percentageFee = 2.50
        minAmount = 5.00
        maxAmount = 5000.00
        currency = "USD"
        active = $true
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$BaseUrl/admin/billing/fee-rules?adminId=$AdminId" `
        -Method Post `
        -Headers $Headers `
        -Body $PayPalBody | Out-Null
    
    Write-Host "  ✓ Regra PayPal criada" -ForegroundColor Green
    
    # Stripe Connect
    $StripeBody = @{
        withdrawalMethod = "STRIPE_CONNECT"
        fixedFee = 0.00
        percentageFee = 0.25
        minAmount = 1.00
        maxAmount = 50000.00
        currency = "USD"
        active = $true
    } | ConvertTo-Json
    
    Invoke-RestMethod -Uri "$BaseUrl/admin/billing/fee-rules?adminId=$AdminId" `
        -Method Post `
        -Headers $Headers `
        -Body $StripeBody | Out-Null
    
    Write-Host "  ✓ Regra Stripe Connect criada" -ForegroundColor Green
    Write-Host ""
}

# Função para verificar configuração
function Verify-Setup {
    Write-Host "[6/7] Verificando configuração..." -ForegroundColor Yellow
    
    $Headers = @{
        "Authorization" = "Bearer $Token"
    }
    
    # Verificar gateways
    $Gateways = Invoke-RestMethod -Uri "$BaseUrl/admin/billing/gateways" `
        -Method Get `
        -Headers $Headers
    Write-Host "  ✓ Gateways configurados: $($Gateways.Count)" -ForegroundColor Green
    
    # Verificar planos
    $Plans = Invoke-RestMethod -Uri "$BaseUrl/admin/billing/plans" `
        -Method Get `
        -Headers $Headers
    Write-Host "  ✓ Planos criados: $($Plans.Count)" -ForegroundColor Green
    
    # Verificar regras de taxas
    $FeeRules = Invoke-RestMethod -Uri "$BaseUrl/admin/billing/fee-rules" `
        -Method Get `
        -Headers $Headers
    Write-Host "  ✓ Regras de taxas criadas: $($FeeRules.Count)" -ForegroundColor Green
    Write-Host ""
}

# Função para exibir resumo
function Show-Summary {
    Write-Host "[7/7] Resumo da Configuração" -ForegroundColor Yellow
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "✓ Sistema de billing configurado com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Próximos passos:" -ForegroundColor Cyan
    Write-Host "1. Verificar Stripe CLI está rodando:"
    Write-Host "   stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Testar checkout com Postman ou curl"
    Write-Host ""
    Write-Host "3. Importar collection do Postman:"
    Write-Host "   Billing_Admin_API.postman_collection.json" -ForegroundColor Gray
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
}

# Main
try {
    Login
    Create-Gateway
    Activate-Gateway
    Create-Plans
    Create-FeeRules
    Verify-Setup
    Show-Summary
}
catch {
    Write-Host "❌ Erro durante a execução: $_" -ForegroundColor Red
    exit 1
}
