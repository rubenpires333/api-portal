# Script para testar se o backend inicia corretamente
Write-Host "=== Testando inicialização do Backend ===" -ForegroundColor Cyan

# Limpar e compilar
Write-Host "`nLimpando projeto..." -ForegroundColor Yellow
mvn clean -q

Write-Host "Compilando projeto..." -ForegroundColor Yellow
mvn compile -DskipTests -q

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ Compilação bem-sucedida!" -ForegroundColor Green
    Write-Host "`nAgora você pode iniciar o backend com:" -ForegroundColor Cyan
    Write-Host "  mvn spring-boot:run" -ForegroundColor White
} else {
    Write-Host "`n✗ Erro na compilação!" -ForegroundColor Red
    exit 1
}
