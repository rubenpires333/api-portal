# Exemplos de Logging

## Setup Básico

### Opção 1: Usando Lombok @Slf4j (Recomendado)

```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MyService {
    
    public void myMethod() {
        log.info("Método executado");
    }
}
```

### Opção 2: Usando Logger Manual

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    
    public void myMethod() {
        log.info("Método executado");
    }
}
```

## Exemplos por Cenário

### 1. Controller - Requisições HTTP

```java
@RestController
@RequestMapping("/api/billing")
@Slf4j
public class BillingController {
    
    @PostMapping("/withdraw")
    public ResponseEntity<?> createWithdrawal(@RequestBody WithdrawalDTO dto) {
        log.info("Recebida requisição de levantamento: userId={}, amount={}", 
                 dto.getUserId(), dto.getAmount());
        
        try {
            WithdrawalRequest result = withdrawalService.create(dto);
            log.info("Levantamento criado com sucesso: id={}", result.getId());
            return ResponseEntity.ok(result);
        } catch (InsufficientFundsException e) {
            log.warn("Saldo insuficiente para levantamento: userId={}, requested={}, available={}", 
                     dto.getUserId(), dto.getAmount(), e.getAvailableBalance());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao criar levantamento: userId={}", dto.getUserId(), e);
            return ResponseEntity.status(500).body("Erro interno");
        }
    }
}
```

### 2. Service - Lógica de Negócio

```java
@Service
@Slf4j
public class WithdrawalService {
    
    @Transactional
    public WithdrawalRequest create(WithdrawalDTO dto) {
        log.debug("Iniciando criação de levantamento: {}", dto);
        
        // Validar saldo
        Wallet wallet = walletService.getByUserId(dto.getUserId());
        log.debug("Saldo disponível: {}, Solicitado: {}", 
                  wallet.getAvailableBalance(), dto.getAmount());
        
        if (wallet.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            log.warn("Saldo insuficiente: userId={}, available={}, requested={}", 
                     dto.getUserId(), wallet.getAvailableBalance(), dto.getAmount());
            throw new InsufficientFundsException(wallet.getAvailableBalance());
        }
        
        // Criar levantamento
        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(dto.getUserId());
        request.setAmount(dto.getAmount());
        request.setStatus(WithdrawalStatus.PENDING);
        
        WithdrawalRequest saved = repository.save(request);
        log.info("Levantamento criado: id={}, userId={}, amount={}", 
                 saved.getId(), saved.getUserId(), saved.getAmount());
        
        return saved;
    }
    
    @Transactional
    public void approve(Long withdrawalId, String adminId) {
        log.info("Aprovando levantamento: id={}, admin={}", withdrawalId, adminId);
        
        WithdrawalRequest request = repository.findById(withdrawalId)
            .orElseThrow(() -> {
                log.error("Levantamento não encontrado: id={}", withdrawalId);
                return new NotFoundException("Levantamento não encontrado");
            });
        
        if (request.getStatus() != WithdrawalStatus.PENDING) {
            log.warn("Tentativa de aprovar levantamento em status inválido: id={}, status={}", 
                     withdrawalId, request.getStatus());
            throw new InvalidStatusException("Apenas levantamentos pendentes podem ser aprovados");
        }
        
        request.setStatus(WithdrawalStatus.APPROVED);
        request.setApprovedBy(adminId);
        request.setApprovedAt(LocalDateTime.now());
        repository.save(request);
        
        log.info("Levantamento aprovado com sucesso: id={}, admin={}", withdrawalId, adminId);
    }
}
```

### 3. Scheduled Job - Tarefas Agendadas

```java
@Service
@Slf4j
public class WithdrawalProcessingService {
    
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void processApprovedWithdrawals() {
        log.info("=== Iniciando job de processamento de levantamentos ===");
        long startTime = System.currentTimeMillis();
        
        List<WithdrawalRequest> approvedRequests = repository
            .findByStatus(WithdrawalStatus.APPROVED);
        
        log.info("Encontrados {} levantamentos aprovados para processar", 
                 approvedRequests.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (WithdrawalRequest request : approvedRequests) {
            try {
                processWithdrawal(request);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("Erro ao processar levantamento: id={}, error={}", 
                         request.getId(), e.getMessage(), e);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("=== Job concluído: success={}, errors={}, duration={}ms ===", 
                 successCount, errorCount, duration);
    }
    
    private void processWithdrawal(WithdrawalRequest request) {
        log.debug("Processando levantamento: id={}, method={}, amount={}", 
                  request.getId(), request.getPaymentMethod(), request.getAmount());
        
        // Processar pagamento
        PaymentResult result = paymentGateway.process(request);
        
        if (result.isSuccess()) {
            request.setStatus(WithdrawalStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            request.setTransactionId(result.getTransactionId());
            repository.save(request);
            
            log.info("Levantamento processado com sucesso: id={}, transactionId={}", 
                     request.getId(), result.getTransactionId());
        } else {
            request.setStatus(WithdrawalStatus.FAILED);
            request.setFailureReason(result.getErrorMessage());
            repository.save(request);
            
            log.error("Falha ao processar levantamento: id={}, reason={}", 
                      request.getId(), result.getErrorMessage());
        }
    }
}
```

### 4. Gateway/Integração Externa

```java
@Service
@Slf4j
public class StripeGateway {
    
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Iniciando pagamento via Stripe: amount={}, currency={}", 
                 request.getAmount(), request.getCurrency());
        
        try {
            // Log da requisição (sem dados sensíveis)
            log.debug("Stripe request: customerId={}, paymentMethodId={}", 
                      request.getCustomerId(), request.getPaymentMethodId());
            
            PaymentIntent intent = PaymentIntent.create(buildParams(request));
            
            log.info("Pagamento criado no Stripe: intentId={}, status={}", 
                     intent.getId(), intent.getStatus());
            
            return PaymentResult.success(intent.getId());
            
        } catch (StripeException e) {
            log.error("Erro ao processar pagamento no Stripe: code={}, message={}", 
                      e.getCode(), e.getMessage(), e);
            return PaymentResult.failure(e.getMessage());
        }
    }
    
    public void handleWebhook(String payload, String signature) {
        log.info("Recebido webhook do Stripe");
        
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            log.info("Webhook validado: type={}, id={}", event.getType(), event.getId());
            
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSuccess(event);
                    break;
                case "payment_intent.failed":
                    handlePaymentFailure(event);
                    break;
                default:
                    log.debug("Evento não tratado: type={}", event.getType());
            }
            
        } catch (SignatureVerificationException e) {
            log.error("Falha na verificação de assinatura do webhook Stripe", e);
            throw new SecurityException("Invalid webhook signature");
        }
    }
}
```

### 5. Repository - Queries Customizadas

```java
@Repository
@Slf4j
public class CustomWithdrawalRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<WithdrawalRequest> findPendingOlderThan(int days) {
        log.debug("Buscando levantamentos pendentes há mais de {} dias", days);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        String jpql = "SELECT w FROM WithdrawalRequest w " +
                     "WHERE w.status = :status AND w.createdAt < :cutoffDate";
        
        List<WithdrawalRequest> results = entityManager
            .createQuery(jpql, WithdrawalRequest.class)
            .setParameter("status", WithdrawalStatus.PENDING)
            .setParameter("cutoffDate", cutoffDate)
            .getResultList();
        
        log.debug("Encontrados {} levantamentos pendentes antigos", results.size());
        
        return results;
    }
}
```

### 6. Exception Handler - Tratamento Global

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException e) {
        log.warn("Recurso não encontrado: {}", e.getMessage());
        return ResponseEntity.status(404).body(e.getMessage());
    }
    
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<?> handleInsufficientFunds(InsufficientFundsException e) {
        log.warn("Saldo insuficiente: available={}, requested={}", 
                 e.getAvailableBalance(), e.getRequestedAmount());
        return ResponseEntity.status(400).body(e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Erro não tratado: {}", e.getMessage(), e);
        return ResponseEntity.status(500).body("Erro interno do servidor");
    }
}
```

### 7. Security/Auth - Autenticação

```java
@Service
@Slf4j
public class AuthService {
    
    public AuthResponse login(LoginRequest request) {
        log.info("Tentativa de login: username={}", request.getUsername());
        
        try {
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado: username={}", request.getUsername());
                    return new BadCredentialsException("Credenciais inválidas");
                });
            
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Senha incorreta: username={}", request.getUsername());
                throw new BadCredentialsException("Credenciais inválidas");
            }
            
            String token = jwtService.generateToken(user);
            log.info("Login bem-sucedido: username={}, userId={}", 
                     request.getUsername(), user.getId());
            
            return new AuthResponse(token);
            
        } catch (BadCredentialsException e) {
            log.error("Falha na autenticação: username={}", request.getUsername());
            throw e;
        }
    }
}
```

## Boas Práticas Demonstradas

1. ✅ **Contexto nos logs**: Sempre inclua IDs, usernames, valores relevantes
2. ✅ **Níveis apropriados**: DEBUG para detalhes, INFO para fluxo, WARN para avisos, ERROR para erros
3. ✅ **Placeholders**: Use `{}` em vez de concatenação
4. ✅ **Stack trace**: Sempre passe a exceção como último parâmetro
5. ✅ **Início e fim**: Logue início e conclusão de operações importantes
6. ✅ **Métricas**: Inclua contadores e duração quando relevante
7. ❌ **Sem dados sensíveis**: Nunca logue senhas, tokens, números de cartão

## Configuração no .env para Debug

Para ver todos esses logs em desenvolvimento:

```env
# Habilitar logs detalhados
APP_LOG_LEVEL=DEBUG
AUTH_LOG_LEVEL=DEBUG
BILLING_LOG_LEVEL=DEBUG
API_LOG_LEVEL=DEBUG

# Arquivo de log
LOG_FILE_PATH=logs/application.log

# Ver também queries SQL se necessário
HIBERNATE_SHOW_SQL=true
HIBERNATE_FORMAT_SQL=true
HIBERNATE_LOG_LEVEL=DEBUG
```

Os logs aparecerão tanto no console quanto no arquivo `logs/application.log`.
