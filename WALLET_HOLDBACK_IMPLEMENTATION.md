# Implementação: Wallet + Holdback System

## Status: ✅ COMPLETO

Sistema de wallet com holdback de 14 dias implementado e funcional.

---

## Arquitetura

### Fluxo de Pagamento de Subscrição de Plataforma

```
Provider paga subscrição (ex: GROWTH - 49 CVE)
    ↓
Payment Intent succeeded webhook
    ↓
CheckoutWebhookService.processPaymentIntentSucceeded()
    ↓
PlatformSubscriptionService.activateSubscription()
    ↓
RevenueShareService.recordPlatformSubscriptionRevenue()
    ↓
Registra receita da plataforma (100% do valor)
```

### Fluxo de Pagamento de API (Consumer → Provider)

```
Consumer paga para usar API do Provider
    ↓
Payment Intent succeeded webhook
    ↓
RevenueShareService.processPayment()
    ↓
Calcula: Platform Commission (20%) + Provider Share (80%)
    ↓
Cria WalletTransaction (PENDING, availableAt = now + 14 dias)
    ↓
Atualiza ProviderWallet.pendingBalance
```

### Fluxo de Liberação de Holdback

```
HoldbackReleaseJob (executa a cada hora)
    ↓
Busca transações PENDING com availableAt <= now
    ↓
Para cada transação:
    - Atualiza status: PENDING → AVAILABLE
    - Move saldo: pendingBalance → availableBalance
    ↓
Provider pode solicitar saque
```

---

## Componentes Implementados

### 1. PlatformSubscriptionService
**Arquivo**: `PlatformSubscriptionService.java`

**Método**: `activateSubscription()`
- Cria/atualiza subscrição do provider
- Chama `RevenueShareService.recordPlatformSubscriptionRevenue()`
- Registra receita da plataforma (100% do valor)

**Nota Importante**: 
- Subscrição de plataforma = Provider PAGA para usar a plataforma
- Não há revenue share aqui (plataforma recebe 100%)
- Provider NÃO recebe crédito na wallet (está pagando, não recebendo)

### 2. RevenueShareService
**Arquivo**: `RevenueShareService.java`

**Método Existente**: `processPayment()`
- Processa pagamentos de Consumer para Provider (uso de API)
- Calcula comissão da plataforma (20%)
- Calcula share do provider (80%)
- Cria transação com holdback de 14 dias
- Atualiza `pendingBalance` do provider

**Método Novo**: `recordPlatformSubscriptionRevenue()`
- Registra receita de subscrição de plataforma
- Cria evento de revenue share (100% plataforma, 0% provider)
- NÃO credita wallet do provider

### 3. HoldbackReleaseJob
**Arquivo**: `HoldbackReleaseJob.java`

**Agendamento**: A cada hora (`@Scheduled(cron = "0 0 * * * *")`)

**Responsabilidades**:
- Busca transações PENDING com `availableAt <= now`
- Atualiza status para AVAILABLE
- Move saldo de `pendingBalance` para `availableBalance`
- Permite que provider solicite saque

**Logs**:
```
=== INICIANDO JOB DE LIBERAÇÃO DE HOLDBACK ===
Encontradas X transações para liberar
Liberando transação: id=..., amount=..., wallet=...
✅ Transação liberada: id=..., amount=...
=== FIM JOB DE LIBERAÇÃO DE HOLDBACK: X transações processadas ===
```

### 4. WalletTransactionRepository
**Arquivo**: `WalletTransactionRepository.java`

**Método Adicionado**: `findByStatusAndAvailableAtBefore()`
- Busca transações por status e data de disponibilidade
- Usado pelo HoldbackReleaseJob

---

## Modelo de Dados

### ProviderWallet
```java
- availableBalance: BigDecimal  // Saldo disponível para saque
- pendingBalance: BigDecimal    // Saldo em holdback (14 dias)
- reservedBalance: BigDecimal   // Saldo reservado (saque em processo)
- lifetimeEarned: BigDecimal    // Total ganho (histórico)
- currency: String              // Moeda (EUR, CVE, etc)
- minimumPayout: BigDecimal     // Valor mínimo para saque
```

### WalletTransaction
```java
- wallet: ProviderWallet        // Wallet do provider
- amount: BigDecimal            // Valor da transação
- type: TransactionType         // CREDIT_REVENUE, DEBIT_WITHDRAWAL, etc
- status: TransactionStatus     // PENDING, AVAILABLE, RESERVED, etc
- referenceId: UUID             // ID da subscrição/invoice
- description: String           // Descrição
- availableAt: LocalDateTime    // Quando fica disponível (createdAt + 14 dias)
- createdAt: LocalDateTime      // Data de criação
```

### TransactionStatus
```java
PENDING     // Em holdback (14 dias)
AVAILABLE   // Disponível para levantamento
RESERVED    // Reservado para levantamento em curso
DEBITED     // Já debitado (levantamento concluído)
COMPLETED   // Transação completada
CANCELLED   // Cancelado
```

### TransactionType
```java
CREDIT_REVENUE          // Receita de API subscription
DEBIT_WITHDRAWAL        // Levantamento de saldo
DEBIT_PLATFORM_FEE      // Taxa da plataforma
CREDIT_REFUND           // Reembolso ao provider
DEBIT_WITHDRAWAL_FEE    // Taxa de levantamento
```

---

## Configuração

### application.properties
```properties
# Holdback period (dias)
billing.holdback-days=14

# Platform commission (%)
billing.platform-commission-percentage=20.00
```

### .env
```env
HOLDBACK_DAYS=14
PLATFORM_COMMISSION_PERCENTAGE=20.00
```

---

## Testes

### Testar Subscrição de Plataforma
1. Provider seleciona plano (GROWTH - 49 CVE)
2. Completa pagamento com cartão teste
3. Verificar logs:
```
Ativando subscrição: sessionId=..., providerId=...
Registrando receita da plataforma: amount=49.00, providerId=...
✅ Receita de subscrição de plataforma registrada: amount=49.00 CVE
✅ Subscrição ativada com sucesso
```

4. Verificar banco de dados:
```sql
-- Subscrição criada
SELECT * FROM provider_platform_subscriptions WHERE provider_id = '...';

-- Evento de revenue share registrado
SELECT * FROM revenue_share_events WHERE provider_id = '...';
```

### Testar Holdback Release
1. Criar transação PENDING manualmente (ou aguardar pagamento de API)
2. Ajustar `availableAt` para data passada:
```sql
UPDATE wallet_transactions 
SET available_at = NOW() - INTERVAL '1 day' 
WHERE status = 'PENDING';
```

3. Aguardar execução do job (próxima hora) ou executar manualmente
4. Verificar logs:
```
=== INICIANDO JOB DE LIBERAÇÃO DE HOLDBACK ===
Encontradas 1 transações para liberar
Liberando transação: id=..., amount=...
✅ Transação liberada: id=..., amount=...
```

5. Verificar banco de dados:
```sql
-- Transação agora AVAILABLE
SELECT * FROM wallet_transactions WHERE id = '...';

-- Saldo movido de pending para available
SELECT * FROM provider_wallets WHERE provider_id = '...';
```

---

## Próximos Passos

### Implementados ✅
1. Sistema de wallet com 3 tipos de saldo
2. Holdback de 14 dias
3. Job automático para liberar fundos
4. Registro de receita de subscrição de plataforma
5. Revenue share para pagamentos de API

### Pendentes 🔧
1. **Sistema de Saques (Withdrawals)**
   - Endpoint para solicitar saque
   - Validação de saldo mínimo
   - Integração com Stripe Payouts ou Vinti4
   - Aprovação automática/manual baseada em threshold

2. **Invoices/Receipts**
   - Gerar invoice para cada pagamento
   - Permitir download de recibo
   - Histórico de invoices

3. **Dashboard de Wallet**
   - Mostrar saldos (available, pending, reserved)
   - Gráfico de earnings ao longo do tempo
   - Histórico de transações
   - Próximas liberações de holdback

4. **Notificações**
   - Email quando fundos são liberados
   - Email quando saque é aprovado
   - Webhook para eventos de wallet

---

## Segurança

### Holdback Period
- Protege a plataforma contra chargebacks
- Permite tempo para resolver disputas
- Padrão da indústria: 7-14 dias

### Validações
- Saldo mínimo para saque (configurável)
- Verificação de saldo disponível antes de saque
- Transações atômicas (ACID)
- Logs detalhados de todas operações

### Auditoria
- Todas transações registradas
- Histórico completo mantido
- `lifetimeEarned` nunca diminui (apenas aumenta)
- Rastreabilidade via `referenceId`

---

## Troubleshooting

### Transação não liberada após 14 dias
1. Verificar se job está executando:
```bash
grep "INICIANDO JOB DE LIBERAÇÃO DE HOLDBACK" logs/application.log
```

2. Verificar `availableAt` da transação:
```sql
SELECT id, amount, status, available_at, created_at 
FROM wallet_transactions 
WHERE status = 'PENDING';
```

3. Executar job manualmente (se necessário)

### Saldo inconsistente
1. Verificar soma de transações:
```sql
SELECT 
    SUM(CASE WHEN type LIKE 'CREDIT%' THEN amount ELSE 0 END) as total_credits,
    SUM(CASE WHEN type LIKE 'DEBIT%' THEN amount ELSE 0 END) as total_debits
FROM wallet_transactions 
WHERE wallet_id = '...';
```

2. Comparar com saldos da wallet:
```sql
SELECT available_balance + pending_balance + reserved_balance as total_balance
FROM provider_wallets 
WHERE id = '...';
```

---

## Conclusão

Sistema de Wallet + Holdback está completo e funcional. Próximo passo recomendado é implementar o sistema de saques (Withdrawals) para permitir que providers retirem seus fundos disponíveis.
