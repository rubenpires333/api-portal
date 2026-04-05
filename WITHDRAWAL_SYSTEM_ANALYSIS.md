# Análise: Sistema de Saques (Withdrawals)

## Status: ✅ 90% IMPLEMENTADO

O sistema de saques já está quase completamente implementado! Falta apenas integração com gateways de pagamento reais.

---

## O que JÁ EXISTE ✅

### 1. Modelos de Dados

**WithdrawalRequest** - Pedido de saque
```java
- requestedAmount: BigDecimal      // Valor solicitado
- feePercentage: BigDecimal        // % de taxa
- feeAmount: BigDecimal            // Valor da taxa
- netAmount: BigDecimal            // Valor líquido (após taxa)
- method: WithdrawalMethod         // Método de pagamento
- destinationDetails: String       // Detalhes encriptados (IBAN, email, etc)
- status: WithdrawalStatus         // Status do pedido
- approvedBy: UUID                 // Admin que aprovou
- rejectionReason: String          // Motivo de rejeição
- requestedAt: LocalDateTime       // Data do pedido
- processedAt: LocalDateTime       // Data de processamento
```

**WithdrawalFeeRule** - Regras de taxas por método
```java
- withdrawalMethod: WithdrawalMethod
- feePercentage: BigDecimal        // Taxa percentual
- fixedFee: BigDecimal             // Taxa fixa
- minimumAmount: BigDecimal        // Valor mínimo
- maximumAmount: BigDecimal        // Valor máximo
- active: boolean                  // Ativo/Inativo
```

### 2. Enums

**WithdrawalStatus**
- `PENDING_APPROVAL` - Aguardando aprovação do admin
- `APPROVED` - Aprovado, aguardando processamento
- `PROCESSING` - Em processamento
- `COMPLETED` - Concluído com sucesso
- `REJECTED` - Rejeitado pelo admin
- `CANCELLED` - Cancelado pelo provider

**WithdrawalMethod**
- `VINTI4` - Pagamento móvel Cabo Verde
- `BANK_TRANSFER` - Transferência bancária
- `PAYPAL` - PayPal
- `WISE` - Wise (TransferWise)
- `PLATFORM_CREDIT` - Crédito na plataforma (0% taxa)

### 3. Serviços Implementados

**WithdrawalService** ✅
- `requestWithdrawal()` - Solicitar saque
  - Valida saldo disponível
  - Valida valor mínimo
  - Calcula taxas automaticamente
  - Reserva saldo (available → reserved)
  - Auto-aprova se abaixo do threshold
  - Notifica admins se pendente
  
- `getProviderWithdrawals()` - Histórico de saques do provider
- `getWithdrawalById()` - Detalhes de um saque
- `cancelWithdrawal()` - Cancelar saque pendente
  - Devolve saldo (reserved → available)
  
- `approveWithdrawal()` - Aprovar saque (admin)
  - Remove saldo reservado
  - Atualiza transação para COMPLETED
  - Notifica provider
  
- `rejectWithdrawal()` - Rejeitar saque (admin)
  - Devolve saldo (reserved → available)
  - Atualiza transação para CANCELLED
  - Notifica provider com motivo

**WithdrawalProcessingService** ✅
- Job agendado (a cada 5 minutos)
- Processa saques aprovados automaticamente
- Atualiza status: APPROVED → PROCESSING → COMPLETED
- Integração com gateways (TODO)

### 4. Controllers Implementados

**WithdrawalController** (Provider) ✅
- `POST /api/v1/provider/wallet/withdraw` - Solicitar saque
- `GET /api/v1/provider/wallet/withdrawals` - Listar meus saques
- `GET /api/v1/provider/wallet/withdraw/{id}` - Ver status de saque
- `DELETE /api/v1/provider/wallet/withdraw/{id}` - Cancelar saque

**AdminWithdrawalController** (Admin) ✅
- `GET /api/v1/admin/withdrawals` - Listar todos os saques
- `GET /api/v1/admin/withdrawals/pending` - Listar saques pendentes
- `GET /api/v1/admin/withdrawals/pending/count` - Contar pendentes
- `POST /api/v1/admin/withdrawals/{id}/approve` - Aprovar saque
- `POST /api/v1/admin/withdrawals/{id}/reject` - Rejeitar saque

### 5. Funcionalidades Implementadas

✅ **Validações**
- Saldo disponível suficiente
- Valor mínimo de saque
- Método de pagamento suportado

✅ **Cálculo de Taxas**
- Taxa percentual + taxa fixa
- Configurável por método de pagamento
- Valor líquido calculado automaticamente

✅ **Aprovação Automática**
- Saques abaixo do threshold são auto-aprovados
- Configurável via `billing.auto-approve-threshold`

✅ **Gestão de Saldos**
- Reserva saldo ao solicitar (available → reserved)
- Devolve saldo ao cancelar/rejeitar (reserved → available)
- Remove saldo ao completar (reserved → 0)

✅ **Notificações**
- Notifica admins quando saque pendente
- Notifica provider quando aprovado
- Notifica provider quando rejeitado

✅ **Histórico**
- Todas transações registradas em `wallet_transactions`
- Status rastreável em tempo real
- Auditoria completa

---

## O que FALTA IMPLEMENTAR 🔧

### 1. Integração com Gateways de Pagamento

**Stripe Payouts** (Recomendado)
```java
// TODO: Implementar em WithdrawalProcessingService
private boolean processStripePayment(WithdrawalRequest request) {
    // Usar Stripe Payouts API
    // https://stripe.com/docs/payouts
    
    Payout payout = Payout.create(
        PayoutCreateParams.builder()
            .setAmount(request.getNetAmount().multiply(100).longValue())
            .setCurrency("eur")
            .setDestination(request.getDestinationDetails()) // Stripe account ID
            .setMetadata(Map.of(
                "withdrawalId", request.getId().toString(),
                "providerId", request.getWallet().getProviderId().toString()
            ))
            .build()
    );
    
    return payout.getStatus().equals("paid");
}
```

**Vinti4 API** (Cabo Verde)
```java
// TODO: Implementar integração com Vinti4
private boolean processVinti4Payment(WithdrawalRequest request) {
    // Usar API Vinti4 para pagamento móvel
    // Documentação: https://vinti4.com/api-docs
    
    // Exemplo:
    // POST https://api.vinti4.com/v1/payouts
    // {
    //   "amount": 50.00,
    //   "currency": "CVE",
    //   "phone": "+238XXXXXXX",
    //   "reference": "withdrawal-uuid"
    // }
}
```

**PayPal Payouts**
```java
// TODO: Implementar PayPal Payouts API
private boolean processPayPalPayment(WithdrawalRequest request) {
    // https://developer.paypal.com/docs/payouts/
}
```

**Wise (TransferWise)**
```java
// TODO: Implementar Wise API
private boolean processWisePayment(WithdrawalRequest request) {
    // https://api-docs.wise.com/
}
```

### 2. Dados Iniciais (Seed Data)

Criar regras de taxas padrão:

```sql
-- Inserir regras de taxas padrão
INSERT INTO withdrawal_fee_rules (
    id, withdrawal_method, fee_percentage, fixed_fee, 
    fixed_fee_currency, minimum_amount, maximum_amount, active, updated_at
) VALUES
-- Vinti4: 2% + 10 CVE
(gen_random_uuid(), 'VINTI4', 2.00, 10.00, 'CVE', 100.00, 50000.00, true, NOW()),

-- Bank Transfer: 1% + 5 EUR
(gen_random_uuid(), 'BANK_TRANSFER', 1.00, 5.00, 'EUR', 50.00, 100000.00, true, NOW()),

-- PayPal: 2.5% + 0.50 EUR
(gen_random_uuid(), 'PAYPAL', 2.50, 0.50, 'EUR', 10.00, 50000.00, true, NOW()),

-- Wise: 0.5% + 1 EUR
(gen_random_uuid(), 'WISE', 0.50, 1.00, 'EUR', 20.00, 100000.00, true, NOW()),

-- Platform Credit: 0% + 0 (sem taxa)
(gen_random_uuid(), 'PLATFORM_CREDIT', 0.00, 0.00, 'EUR', 1.00, 100000.00, true, NOW());
```

### 3. Melhorias Opcionais

**Webhook de Confirmação**
- Receber confirmação do gateway quando pagamento é processado
- Atualizar status automaticamente

**Retry Logic**
- Tentar novamente se pagamento falhar
- Limite de tentativas (ex: 3x)
- Backoff exponencial

**Relatórios**
- Dashboard de saques para admin
- Estatísticas (total processado, taxas cobradas, etc)
- Exportar para CSV/Excel

---

## Fluxo Completo Implementado

### Fluxo do Provider (Solicitar Saque)

```
1. Provider tem saldo disponível (ex: 100 EUR)
   ↓
2. Solicita saque de 50 EUR via PayPal
   ↓
3. Sistema calcula taxa: 2.5% + 0.50 = 1.75 EUR
   ↓
4. Valor líquido: 50 - 1.75 = 48.25 EUR
   ↓
5. Valida saldo disponível (100 >= 50) ✅
   ↓
6. Reserva saldo:
   - available: 100 → 50
   - reserved: 0 → 50
   ↓
7. Verifica threshold (ex: 50 <= 100)
   ↓
8. Auto-aprova OU envia para aprovação manual
   ↓
9. Se pendente: Notifica admins
```

### Fluxo de Aprovação (Admin)

```
1. Admin vê lista de saques pendentes
   ↓
2. Revisa detalhes do saque
   ↓
3. Aprova OU Rejeita
   ↓
4. Se APROVADO:
   - Status: PENDING_APPROVAL → APPROVED
   - Notifica provider
   - Job processa automaticamente
   ↓
5. Se REJEITADO:
   - Status: PENDING_APPROVAL → REJECTED
   - Devolve saldo (reserved → available)
   - Notifica provider com motivo
```

### Fluxo de Processamento (Job Automático)

```
Job executa a cada 5 minutos
   ↓
1. Busca saques com status APPROVED
   ↓
2. Para cada saque:
   - Atualiza status: APPROVED → PROCESSING
   - Chama gateway de pagamento
   - Se sucesso: PROCESSING → COMPLETED
   - Se falha: PROCESSING → APPROVED (retry)
   ↓
3. Se COMPLETED:
   - Remove saldo reservado
   - Atualiza transação
   - Notifica provider
```

---

## Configuração Necessária

### application.properties
```properties
# Threshold para auto-aprovação (EUR)
billing.auto-approve-threshold=50.00

# Stripe Payouts
billing.stripe.payout-enabled=true

# Vinti4
billing.vinti4.api-key=${VINTI4_API_KEY:}
billing.vinti4.merchant-id=${VINTI4_MERCHANT_ID:}

# PayPal
billing.paypal.client-id=${PAYPAL_CLIENT_ID:}
billing.paypal.client-secret=${PAYPAL_CLIENT_SECRET:}

# Wise
billing.wise.api-key=${WISE_API_KEY:}
```

### .env
```env
# Auto-approve threshold
AUTO_APPROVE_THRESHOLD=50.00

# Gateway credentials
VINTI4_API_KEY=
VINTI4_MERCHANT_ID=
PAYPAL_CLIENT_ID=
PAYPAL_CLIENT_SECRET=
WISE_API_KEY=
```

---

## Testes

### 1. Testar Solicitação de Saque

```bash
# Solicitar saque
curl -X POST http://localhost:8080/api/v1/provider/wallet/withdraw \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.00,
    "method": "PAYPAL",
    "destinationDetails": "provider@email.com"
  }' \
  -G --data-urlencode "providerId=$PROVIDER_ID"
```

### 2. Testar Listagem de Saques

```bash
# Listar meus saques
curl -X GET "http://localhost:8080/api/v1/provider/wallet/withdrawals?providerId=$PROVIDER_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Testar Aprovação (Admin)

```bash
# Aprovar saque
curl -X POST "http://localhost:8080/api/v1/admin/withdrawals/$WITHDRAWAL_ID/approve?adminId=$ADMIN_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 4. Verificar no Banco

```sql
-- Ver saques pendentes
SELECT * FROM withdrawal_requests 
WHERE status = 'PENDING_APPROVAL'
ORDER BY requested_at DESC;

-- Ver saldos
SELECT 
    provider_id,
    available_balance,
    reserved_balance,
    pending_balance
FROM provider_wallets;

-- Ver transações de saque
SELECT * FROM wallet_transactions 
WHERE type = 'DEBIT_WITHDRAWAL'
ORDER BY created_at DESC;
```

---

## Próximos Passos Recomendados

### Opção A: Implementar Stripe Payouts (Mais Simples)
1. Adicionar dependência Stripe (já existe)
2. Implementar `processStripePayment()` em `WithdrawalProcessingService`
3. Configurar Stripe Connect para providers
4. Testar com conta Stripe de teste

### Opção B: Implementar Vinti4 (Cabo Verde)
1. Obter credenciais API Vinti4
2. Implementar `processVinti4Payment()`
3. Testar com números de teste

### Opção C: Seed Data + Testes
1. Criar script SQL com regras de taxas
2. Testar fluxo completo sem gateway real
3. Simular processamento bem-sucedido

---

## Conclusão

O sistema de saques está **90% implementado**! Falta apenas:

1. ✅ Modelos - COMPLETO
2. ✅ Serviços - COMPLETO
3. ✅ Controllers - COMPLETO
4. ✅ Validações - COMPLETO
5. ✅ Notificações - COMPLETO
6. ✅ Jobs - COMPLETO
7. 🔧 Integração com Gateways - PENDENTE (10%)
8. 🔧 Seed Data - PENDENTE

Recomendo começar com **Opção C** (Seed Data + Testes) para validar o fluxo completo, depois implementar integração real com Stripe Payouts.
