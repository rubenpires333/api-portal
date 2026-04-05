# Update Platform Plans Script
# Run this to update the plans in the database

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Atualizando Planos da Plataforma" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Database connection details
$dbHost = "localhost"
$dbPort = "5432"
$dbName = "db_portal_api"
$dbUser = "apiportal"
$dbPassword = "apiportal"

# Path to psql (adjust if needed)
$psqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe"

if (-not (Test-Path $psqlPath)) {
    $psqlPath = "C:\Program Files\PostgreSQL\15\bin\psql.exe"
}

if (-not (Test-Path $psqlPath)) {
    Write-Host "❌ psql não encontrado. Por favor, instale o PostgreSQL ou ajuste o caminho." -ForegroundColor Red
    Write-Host ""
    Write-Host "Você pode executar o SQL manualmente:" -ForegroundColor Yellow
    Write-Host "1. Abra pgAdmin ou outro cliente PostgreSQL" -ForegroundColor Yellow
    Write-Host "2. Execute o arquivo: scripts/insert_default_plans.sql" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ psql encontrado: $psqlPath" -ForegroundColor Green

# Set password environment variable
$env:PGPASSWORD = $dbPassword

try {
    Write-Host ""
    Write-Host "Executando script SQL..." -ForegroundColor Yellow
    
    & $psqlPath -h $dbHost -p $dbPort -U $dbUser -d $dbName -f "scripts/insert_default_plans.sql"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "✅ PLANOS ATUALIZADOS COM SUCESSO!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Próximos passos:" -ForegroundColor Cyan
        Write-Host "1. Reinicie o backend se estiver rodando" -ForegroundColor White
        Write-Host "2. Acesse http://localhost:4200/provider/plans" -ForegroundColor White
        Write-Host "3. Verifique se as descrições estão aparecendo" -ForegroundColor White
    } else {
        Write-Host ""
        Write-Host "❌ Erro ao executar o script SQL" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host ""
    Write-Host "❌ Erro: $_" -ForegroundColor Red
    exit 1
} finally {
    # Clear password
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
