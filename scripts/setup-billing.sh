#!/bin/bash

# API Portal - Billing Setup Script
# Este script automatiza a configuração inicial do sistema de billing

set -e

echo "=========================================="
echo "API Portal - Billing Setup"
echo "=========================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variáveis
BASE_URL="http://localhost:8080/api/v1"
TOKEN=""
ADMIN_ID=""

# Função para fazer login
login() {
    echo -e "${YELLOW}[1/7] Fazendo login como SUPERADMIN...${NC}"
    read -p "Email do SUPERADMIN: " ADMIN_EMAIL
    read -sp "Senha: " ADMIN_PASSWORD
    echo ""
    
    RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")
    
    TOKEN=$(echo $RESPONSE | jq -r '.accessToken')
    ADMIN_ID=$(echo $RESPONSE | jq -r '.userId')
    
    if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
        echo -e "${RED}❌ Erro ao fazer login. Verifique as credenciais.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Login realizado com sucesso!${NC}"
    echo ""
}

# Função para criar gateway Stripe
create_gateway() {
    echo -e "${YELLOW}[2/7] Criando configuração do Gateway Stripe...${NC}"
    
    read -p "Stripe API Key (pk_test_...): " STRIPE_KEY
    read -p "Stripe Webhook Secret (whsec_...): " WEBHOOK_SECRET
    
    RESPONSE=$(curl -s -X POST "$BASE_URL/admin/billing/gateways" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"gatewayType\": \"STRIPE\",
            \"apiKey\": \"$STRIPE_KEY\",
            \"webhookSecret\": \"$WEBHOOK_SECRET\",
            \"active\": true,
            \"testMode\": true
        }")
    
    if echo "$RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Gateway Stripe criado com sucesso!${NC}"
    else
        echo -e "${RED}❌ Erro ao criar gateway: $RESPONSE${NC}"
        exit 1
    fi
    echo ""
}

# Função para ativar gateway
activate_gateway() {
    echo -e "${YELLOW}[3/7] Ativando Gateway Stripe...${NC}"
    
    curl -s -X POST "$BASE_URL/admin/billing/gateways/STRIPE/activate" \
        -H "Authorization: Bearer $TOKEN" > /dev/null
    
    echo -e "${GREEN}✓ Gateway ativado!${NC}"
    echo ""
}

# Função para criar planos
create_plans() {
    echo -e "${YELLOW}[4/7] Criando planos da plataforma...${NC}"
    
    read -p "Stripe Price ID - Starter (price_...): " PRICE_STARTER
    read -p "Stripe Price ID - Growth (price_...): " PRICE_GROWTH
    read -p "Stripe Price ID - Business (price_...): " PRICE_BUSINESS
    
    # Criar Starter Plan
    curl -s -X POST "$BASE_URL/admin/billing/plans" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"name\": \"STARTER\",
            \"displayName\": \"Starter Plan\",
            \"description\": \"Perfect for testing and small projects\",
            \"monthlyPrice\": 0.00,
            \"currency\": \"USD\",
            \"maxApis\": 3,
            \"maxRequestsPerMonth\": 10000,
            \"maxTeamMembers\": 1,
            \"customDomain\": false,
            \"prioritySupport\": false,
            \"advancedAnalytics\": false,
            \"stripePriceId\": \"$PRICE_STARTER\",
            \"active\": true
        }" > /dev/null
    
    echo -e "${GREEN}  ✓ Starter Plan criado${NC}"
    
    # Criar Growth Plan
    curl -s -X POST "$BASE_URL/admin/billing/plans" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"name\": \"GROWTH\",
            \"displayName\": \"Growth Plan\",
            \"description\": \"For growing businesses and teams\",
            \"monthlyPrice\": 49.00,
            \"currency\": \"USD\",
            \"maxApis\": 10,
            \"maxRequestsPerMonth\": 100000,
            \"maxTeamMembers\": 5,
            \"customDomain\": true,
            \"prioritySupport\": true,
            \"advancedAnalytics\": true,
            \"stripePriceId\": \"$PRICE_GROWTH\",
            \"active\": true
        }" > /dev/null
    
    echo -e "${GREEN}  ✓ Growth Plan criado${NC}"
    
    # Criar Business Plan
    curl -s -X POST "$BASE_URL/admin/billing/plans" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"name\": \"BUSINESS\",
            \"displayName\": \"Business Plan\",
            \"description\": \"Enterprise-grade features and support\",
            \"monthlyPrice\": 149.00,
            \"currency\": \"USD\",
            \"maxApis\": -1,
            \"maxRequestsPerMonth\": -1,
            \"maxTeamMembers\": -1,
            \"customDomain\": true,
            \"prioritySupport\": true,
            \"advancedAnalytics\": true,
            \"stripePriceId\": \"$PRICE_BUSINESS\",
            \"active\": true
        }" > /dev/null
    
    echo -e "${GREEN}  ✓ Business Plan criado${NC}"
    echo ""
}

# Função para criar regras de taxas
create_fee_rules() {
    echo -e "${YELLOW}[5/7] Criando regras de taxas de levantamento...${NC}"
    
    # Bank Transfer
    curl -s -X POST "$BASE_URL/admin/billing/fee-rules?adminId=$ADMIN_ID" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"withdrawalMethod\": \"BANK_TRANSFER\",
            \"fixedFee\": 2.50,
            \"percentageFee\": 1.00,
            \"minAmount\": 10.00,
            \"maxAmount\": 10000.00,
            \"currency\": \"USD\",
            \"active\": true
        }" > /dev/null
    
    echo -e "${GREEN}  ✓ Regra Bank Transfer criada${NC}"
    
    # PayPal
    curl -s -X POST "$BASE_URL/admin/billing/fee-rules?adminId=$ADMIN_ID" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"withdrawalMethod\": \"PAYPAL\",
            \"fixedFee\": 0.50,
            \"percentageFee\": 2.50,
            \"minAmount\": 5.00,
            \"maxAmount\": 5000.00,
            \"currency\": \"USD\",
            \"active\": true
        }" > /dev/null
    
    echo -e "${GREEN}  ✓ Regra PayPal criada${NC}"
    
    # Stripe Connect
    curl -s -X POST "$BASE_URL/admin/billing/fee-rules?adminId=$ADMIN_ID" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{
            \"withdrawalMethod\": \"STRIPE_CONNECT\",
            \"fixedFee\": 0.00,
            \"percentageFee\": 0.25,
            \"minAmount\": 1.00,
            \"maxAmount\": 50000.00,
            \"currency\": \"USD\",
            \"active\": true
        }" > /dev/null
    
    echo -e "${GREEN}  ✓ Regra Stripe Connect criada${NC}"
    echo ""
}

# Função para verificar configuração
verify_setup() {
    echo -e "${YELLOW}[6/7] Verificando configuração...${NC}"
    
    # Verificar gateways
    GATEWAYS=$(curl -s -X GET "$BASE_URL/admin/billing/gateways" \
        -H "Authorization: Bearer $TOKEN")
    GATEWAY_COUNT=$(echo $GATEWAYS | jq '. | length')
    echo -e "${GREEN}  ✓ Gateways configurados: $GATEWAY_COUNT${NC}"
    
    # Verificar planos
    PLANS=$(curl -s -X GET "$BASE_URL/admin/billing/plans" \
        -H "Authorization: Bearer $TOKEN")
    PLAN_COUNT=$(echo $PLANS | jq '. | length')
    echo -e "${GREEN}  ✓ Planos criados: $PLAN_COUNT${NC}"
    
    # Verificar regras de taxas
    FEE_RULES=$(curl -s -X GET "$BASE_URL/admin/billing/fee-rules" \
        -H "Authorization: Bearer $TOKEN")
    FEE_COUNT=$(echo $FEE_RULES | jq '. | length')
    echo -e "${GREEN}  ✓ Regras de taxas criadas: $FEE_COUNT${NC}"
    echo ""
}

# Função para exibir resumo
show_summary() {
    echo -e "${YELLOW}[7/7] Resumo da Configuração${NC}"
    echo "=========================================="
    echo -e "${GREEN}✓ Sistema de billing configurado com sucesso!${NC}"
    echo ""
    echo "Próximos passos:"
    echo "1. Verificar Stripe CLI está rodando:"
    echo "   stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe"
    echo ""
    echo "2. Testar checkout:"
    echo "   curl -X POST $BASE_URL/billing/checkout \\"
    echo "     -H 'Authorization: Bearer $TOKEN' \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"planName\":\"GROWTH\",\"successUrl\":\"http://localhost:4200/success\",\"cancelUrl\":\"http://localhost:4200/cancel\"}'"
    echo ""
    echo "3. Importar collection do Postman:"
    echo "   Billing_Admin_API.postman_collection.json"
    echo ""
    echo "=========================================="
}

# Verificar dependências
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}❌ curl não está instalado${NC}"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}❌ jq não está instalado. Instale com: sudo apt-get install jq${NC}"
        exit 1
    fi
}

# Main
main() {
    check_dependencies
    login
    create_gateway
    activate_gateway
    create_plans
    create_fee_rules
    verify_setup
    show_summary
}

main
