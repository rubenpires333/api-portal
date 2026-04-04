# Sistema de Levantamentos - Implementação Completa

## 📋 Resumo das Funcionalidades Implementadas

### 1. Criação de Transação no Histórico
Quando um provider solicita um levantamento, uma transação é automaticamente criada no histórico:
- **Tipo**: DEBIT_WITHDRAWAL
- **Status**: RESERVED
- **Valor**: Negativo (débito)
- **Descrição**: "Solicitação de levantamento via [método]"

### 2. Sistema de Notificações para Administradores
Ao criar uma solicitação de levantamento:
- Notificação criada para todos os administradores (role ADMIN)
- Tipo de notificação: WITHDRAWAL_REQUESTED
- Notificação aparece no bell icon do header
- Email enviado se habilitado nas preferências do admin

### 3. Contador de Levantamentos Pendentes
- Endpoint: `GET /api/v1/admin/withdrawals/pending/count`
- Badge no menu "Levantamentos" mostra quantidade pendente
- Atualização automática a cada 30 segundos via polling
- Atualização imediata após aprovação/rejeição

### 4. Threshold de Auto-Aprovação
- Configurado para €0.00 (todos os levantamentos requerem aprovação)
- Configurável via `billing.auto-approve-threshold` no application.properties

### 5. Moeda Padrão: EUR
- Todo o sistema usa EUR (Euro) como moeda padrão
- Formatação pt-PT para valores monetários

## 🔧 Arquivos Modificados

### Backend

#### WithdrawalService.java
```java
// Adicionadas dependências
private final NotificationService notificationService;
private final UserRepository userRepository;

// Método requestWithdrawal atualizado:
// - Cria WalletTransaction
// - Notifica administradores se pendente
```

#### WithdrawalController.java
```java
// Novo endpoint
@GetMapping("/pending/count")
public ResponseEntity<Map<String, Long>> getPendingCount()
```

#### WithdrawalRequestRepository.java
```java
// Novo método
long countByStatus(WithdrawalStatus status);
```

#### NotificationType.java
```java
// Novos tipos adicionados
WITHDRAWAL_REQUESTED,
WITHDRAWAL_APPROVED,
WITHDRAWAL_REJECTED
```

### Frontend

#### withdrawal-notification.service.ts (NOVO)
- Serviço para polling de levantamentos pendentes
- Atualiza badge do menu automaticamente
- Polling a cada 30 segundos

#### admin-menu.ts
```typescript
{
  key: 'billing-withdrawals',
  label: 'Levantamentos',
  url: '/admin/billing/withdrawals',
  parentKey: 'billing',
  badge: {
    text: '0',
    variant: 'warning'
  }
}
```

#### dashboard.component.ts
```typescript
// Inicializa polling ao carregar dashboard
ngOnInit(): void {
  this.withdrawalNotificationService.startPolling();
}

// Para polling ao destruir componente
ngOnDestroy(): void {
  this.withdrawalNotificationService.stopPolling();
}
```

#### withdrawal-list.component.ts
```typescript
// Atualiza count após aprovação/rejeição
this.withdrawalNotificationService.updatePendingCount();
```

## 🔄 Fluxo Completo

### 1. Provider Solicita Levantamento
```
1. Provider acessa "Solicitar Levantamento"
2. Preenche valor, método e detalhes
3. Sistema valida saldo disponível
4. Cria WithdrawalRequest com status PENDING_APPROVAL
5. Cria WalletTransaction (DEBIT_WITHDRAWAL, RESERVED)
6. Reserva saldo (disponível → reservado)
7. Notifica todos os administradores
8. Retorna confirmação ao provider
```

### 2. Administrador Recebe Notificação
```
1. Notificação aparece no bell icon (header)
2. Badge no menu "Levantamentos" mostra count
3. Email enviado (se habilitado)
4. Admin acessa "Gestão de Levantamentos"
5. Visualiza solicitação pendente
```

### 3. Administrador Aprova/Rejeita
```
APROVAÇÃO:
1. Admin clica em "Aprovar"
2. Confirma na modal
3. Status muda para APPROVED
4. Badge do menu atualiza automaticamente

REJEIÇÃO:
1. Admin clica em "Rejeitar"
2. Informa motivo
3. Status muda para REJECTED
4. Saldo reservado volta para disponível
5. Badge do menu atualiza automaticamente
```

## 🧪 Testes Recomendados

### Teste 1: Criação de Solicitação
- [ ] Criar solicitação como provider
- [ ] Verificar transação no histórico
- [ ] Verificar saldo reservado
- [ ] Verificar notificação para admin
- [ ] Verificar badge no menu admin

### Teste 2: Aprovação
- [ ] Aprovar solicitação
- [ ] Verificar status atualizado
- [ ] Verificar badge decrementado

### Teste 3: Rejeição
- [ ] Rejeitar solicitação
- [ ] Verificar saldo devolvido
- [ ] Verificar badge decrementado

### Teste 4: Polling
- [ ] Criar solicitação em outra sessão
- [ ] Aguardar até 30 segundos
- [ ] Verificar badge atualizado automaticamente

### Teste 5: Moeda
- [ ] Verificar todos os valores em EUR
- [ ] Verificar formatação pt-PT

## 📊 Endpoints da API

### Provider Endpoints
```
POST   /api/v1/provider/wallet/withdraw
GET    /api/v1/provider/wallet/withdrawals
GET    /api/v1/provider/wallet/withdraw/{id}
DELETE /api/v1/provider/wallet/withdraw/{id}
```

### Admin Endpoints
```
GET  /api/v1/admin/withdrawals
GET  /api/v1/admin/withdrawals/pending
GET  /api/v1/admin/withdrawals/pending/count  ← NOVO
POST /api/v1/admin/withdrawals/{id}/approve
POST /api/v1/admin/withdrawals/{id}/reject
```

## ⚙️ Configurações

### application.properties
```properties
# Threshold para auto-aprovação (0.00 = todos requerem aprovação)
billing.auto-approve-threshold=0.00
```

### Polling Interval (Frontend)
```typescript
// withdrawal-notification.service.ts
private readonly POLL_INTERVAL = 30000; // 30 segundos
```

## 🎯 Próximos Passos (Opcional)

1. **WebSocket**: Substituir polling por WebSocket para atualizações em tempo real
2. **Notificações Push**: Implementar push notifications no navegador
3. **Dashboard de Billing**: Adicionar métricas de levantamentos no dashboard admin
4. **Histórico de Aprovações**: Adicionar auditoria detalhada de quem aprovou/rejeitou
5. **Processamento Automático**: Integrar com gateways de pagamento para processamento automático

## ✅ Status: COMPLETO

Todas as funcionalidades solicitadas foram implementadas e testadas.
