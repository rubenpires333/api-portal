# Test Stripe API Key
# This script tests if your Stripe API key is valid

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testando Chave Stripe" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Read .env file
$envFile = ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "❌ Arquivo .env não encontrado!" -ForegroundColor Red
    exit 1
}

$stripeKey = ""
Get-Content $envFile | ForEach-Object {
    if ($_ -match "^STRIPE_API_KEY=(.+)$") {
        $stripeKey = $matches[1].Trim()
    }
}

if ([string]::IsNullOrEmpty($stripeKey)) {
    Write-Host "❌ STRIPE_API_KEY não encontrada no .env" -ForegroundColor Red
    exit 1
}

Write-Host "Chave encontrada: $($stripeKey.Substring(0, 15))..." -ForegroundColor Yellow
Write-Host ""

# Validate key format
if ($stripeKey -match "^sk_test_") {
    Write-Host "✓ Formato correto: Chave de teste (sk_test_)" -ForegroundColor Green
} elseif ($stripeKey -match "^sk_live_") {
    Write-Host "⚠ Chave de produção detectada (sk_live_)" -ForegroundColor Yellow
    Write-Host "  Certifique-se de que deseja usar produção!" -ForegroundColor Yellow
} elseif ($stripeKey -match "^pk_") {
    Write-Host "❌ ERRO: Esta é uma chave PUBLISHABLE (pk_)" -ForegroundColor Red
    Write-Host "  Você precisa usar a chave SECRET (sk_test_ ou sk_live_)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Yellow
    Write-Host "1. Acesse: https://dashboard.stripe.com/test/apikeys" -ForegroundColor White
    Write-Host "2. Copie a 'Secret key' (começa com sk_test_)" -ForegroundColor White
    Write-Host "3. Cole no arquivo .env na variável STRIPE_API_KEY" -ForegroundColor White
    exit 1
} else {
    Write-Host "⚠ Formato de chave desconhecido" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Testando conexão com Stripe..." -ForegroundColor Yellow

try {
    # Test Stripe API
    $headers = @{
        "Authorization" = "Bearer $stripeKey"
    }
    
    $response = Invoke-RestMethod -Uri "https://api.stripe.com/v1/balance" -Headers $headers -Method Get
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✅ CHAVE STRIPE VÁLIDA!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Saldo disponível:" -ForegroundColor Cyan
    foreach ($balance in $response.available) {
        $amount = $balance.amount / 100
        Write-Host "  $($balance.currency.ToUpper()): $amount" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "Próximos passos:" -ForegroundColor Cyan
    Write-Host "1. Reinicie o backend (Ctrl+C e depois: mvn spring-boot:run)" -ForegroundColor White
    Write-Host "2. Execute o script SQL: scripts/quick_update_plans.sql" -ForegroundColor White
    Write-Host "3. Teste a página de planos: http://localhost:4200/provider/plans" -ForegroundColor White
    
} catch {
    Write-Host ""
    Write-Host "❌ Erro ao testar chave Stripe" -ForegroundColor Red
    Write-Host ""
    
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "Erro 401: Chave inválida ou expirada" -ForegroundColor Red
        Write-Host ""
        Write-Host "Como corrigir:" -ForegroundColor Yellow
        Write-Host "1. Acesse: https://dashboard.stripe.com/test/apikeys" -ForegroundColor White
        Write-Host "2. Gere uma nova 'Secret key'" -ForegroundColor White
        Write-Host "3. Atualize o .env com a nova chave" -ForegroundColor White
    } else {
        Write-Host "Detalhes: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    exit 1
}
