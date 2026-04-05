#!/bin/bash
# Script para carregar variáveis de ambiente do arquivo .env
# Uso: source ./load-env.sh

ENV_FILE=".env"

if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Erro: Arquivo .env não encontrado!"
    echo "📝 Copie o arquivo .env.example para .env e configure suas credenciais:"
    echo "   cp .env.example .env"
    return 1 2>/dev/null || exit 1
fi

echo "🔄 Carregando variáveis de ambiente do arquivo .env..."

# Carregar variáveis, ignorando comentários e linhas vazias
while IFS='=' read -r key value; do
    # Ignorar linhas vazias e comentários
    [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]] && continue
    
    # Remover espaços em branco
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    
    # Remover aspas se existirem
    value="${value%\"}"
    value="${value#\"}"
    value="${value%\'}"
    value="${value#\'}"
    
    # Exportar variável
    export "$key=$value"
    echo "  ✓ $key"
done < "$ENV_FILE"

echo ""
echo "✅ Variáveis de ambiente carregadas com sucesso!"
echo "🚀 Agora você pode executar: mvn spring-boot:run"
