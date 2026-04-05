#!/bin/bash

# Script para verificar Price IDs no Stripe
# Requer: Stripe CLI instalado (https://stripe.com/docs/stripe-cli)

echo "========================================="
echo "Verificando Price IDs no Stripe"
echo "========================================="
echo ""

# Verificar se Stripe CLI está instalado
if ! command -v stripe &> /dev/null; then
    echo "❌ Stripe CLI não está instalado!"
    echo "Instale em: https://stripe.com/docs/stripe-cli"
    exit 1
fi

echo "✅ Stripe CLI encontrado"
echo ""

# Price IDs para verificar (substitua pelos seus)
STARTER_PRICE_ID="price_STARTER"  # Substituir
GROWTH_PRICE_ID="price_1TIauu7KdKMF57NaGkQ8oTah"
BUSINESS_PRICE_ID="price_BUSINESS"  # Substituir

echo "========================================="
echo "GROWTH PLAN"
echo "========================================="
stripe prices retrieve $GROWTH_PRICE_ID --format json | jq '{
  id: .id,
  active: .active,
  currency: .currency,
  unit_amount: .unit_amount,
  unit_amount_decimal: .unit_amount_decimal,
  type: .type,
  recurring: .recurring
}'

echo ""
echo "========================================="
echo "Análise:"
echo "========================================="

# Buscar unit_amount
UNIT_AMOUNT=$(stripe prices retrieve $GROWTH_PRICE_ID --format json | jq -r '.unit_amount')

if [ "$UNIT_AMOUNT" == "0" ] || [ "$UNIT_AMOUNT" == "null" ]; then
    echo "❌ PROBLEMA: unit_amount = $UNIT_AMOUNT"
    echo "   O Price está configurado como GRATUITO!"
    echo "   Esperado: 2999 (€29.99 em centavos)"
    echo ""
    echo "   SOLUÇÃO:"
    echo "   1. Acesse: https://dashboard.stripe.com/test/prices/$GROWTH_PRICE_ID"
    echo "   2. Crie um novo Price com valor €29.99"
    echo "   3. Atualize o banco de dados com o novo Price ID"
else
    echo "✅ unit_amount = $UNIT_AMOUNT centavos"
    AMOUNT_EUR=$(echo "scale=2; $UNIT_AMOUNT / 100" | bc)
    echo "   Equivalente a: €$AMOUNT_EUR"
fi

echo ""
echo "========================================="
echo "Comando para criar novo Price (se necessário):"
echo "========================================="
echo ""
echo "stripe prices create \\"
echo "  --product prod_XXXXXXXX \\"
echo "  --currency eur \\"
echo "  --unit-amount 2999 \\"
echo "  --recurring[interval]=month \\"
echo "  --metadata[plan_name]=GROWTH \\"
echo "  --metadata[environment]=test"
echo ""
