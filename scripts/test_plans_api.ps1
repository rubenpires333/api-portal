# Test Plans API
# This script tests if the plans API is returning data correctly

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testando API de Planos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$apiUrl = "http://localhost:8080/api/v1/billing/plans"

Write-Host "URL: $apiUrl" -ForegroundColor Yellow
Write-Host ""

try {
    Write-Host "Fazendo requisição..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri $apiUrl -Method Get -ContentType "application/json"
    
    Write-Host "✅ Sucesso! Planos encontrados: $($response.Count)" -ForegroundColor Green
    Write-Host ""
    
    foreach ($plan in $response) {
        Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
        Write-Host "Plano: $($plan.displayName) ($($plan.name))" -ForegroundColor White
        Write-Host "Descrição: $($plan.description)" -ForegroundColor Gray
        Write-Host "Preço: `$$($plan.monthlyPrice)/$($plan.currency)" -ForegroundColor Yellow
        Write-Host "Max APIs: $($plan.maxApis)" -ForegroundColor Gray
        Write-Host "Max Requests: $($plan.maxRequestsPerMonth)" -ForegroundColor Gray
        Write-Host "Custom Domain: $($plan.customDomain)" -ForegroundColor Gray
        Write-Host "Priority Support: $($plan.prioritySupport)" -ForegroundColor Gray
        Write-Host "Advanced Analytics: $($plan.advancedAnalytics)" -ForegroundColor Gray
        Write-Host "Ativo: $($plan.active)" -ForegroundColor Gray
        Write-Host ""
    }
    
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✅ API está funcionando corretamente!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Erro ao chamar a API" -ForegroundColor Red
    Write-Host ""
    Write-Host "Detalhes do erro:" -ForegroundColor Yellow
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Possíveis causas:" -ForegroundColor Yellow
    Write-Host "1. Backend não está rodando (inicie com: mvn spring-boot:run)" -ForegroundColor White
    Write-Host "2. Porta 8080 está sendo usada por outro processo" -ForegroundColor White
    Write-Host "3. Banco de dados não está acessível" -ForegroundColor White
    Write-Host "4. Planos não foram inseridos no banco" -ForegroundColor White
}
