# Script para carregar variáveis de ambiente do arquivo .env
# Uso: .\load-env.ps1

$envFile = ".env"

if (-Not (Test-Path $envFile)) {
    Write-Host "Erro: Arquivo .env não encontrado!" -ForegroundColor Red
    Write-Host "Copie o arquivo .env.example para .env e configure suas credenciais:" -ForegroundColor Yellow
    Write-Host "  cp .env.example .env" -ForegroundColor Cyan
    exit 1
}

Write-Host "Carregando variáveis de ambiente do arquivo .env..." -ForegroundColor Green

Get-Content $envFile | ForEach-Object {
    # Ignorar linhas vazias e comentários
    if ($_ -match '^\s*$' -or $_ -match '^\s*#') {
        return
    }
    
    # Parse linha no formato KEY=VALUE
    if ($_ -match '^([^=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        
        # Remover aspas se existirem
        $value = $value -replace '^["'']|["'']$', ''
        
        # Definir variável de ambiente
        [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        Write-Host "  ✓ $key" -ForegroundColor Gray
    }
}

Write-Host "`nVariáveis de ambiente carregadas com sucesso!" -ForegroundColor Green
Write-Host "Agora você pode executar: mvn spring-boot:run" -ForegroundColor Cyan
