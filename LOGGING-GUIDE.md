# Guia de Logging da Aplicação

## Visão Geral

A aplicação está configurada para escrever logs tanto no console quanto em arquivo, com controle granular por módulo.

## Localização dos Logs

Por padrão, os logs são salvos em:
```
logs/application.log
```

Você pode alterar o caminho no `.env`:
```env
LOG_FILE_PATH=logs/application.log
```

## Configuração no .env

### Configuração de Arquivo de Log

```env
# Caminho do arquivo de log
LOG_FILE_PATH=logs/application.log

# Tamanho máximo de cada arquivo (quando atingir, cria um novo)
LOG_FILE_MAX_SIZE=10MB

# Número de arquivos históricos a manter
LOG_FILE_MAX_HISTORY=30

# Tamanho total máximo de todos os arquivos de log
LOG_FILE_TOTAL_SIZE=100MB
```

### Níveis de Log por Módulo

```env
# Nível geral da aplicação
APP_LOG_LEVEL=DEBUG

# Módulo de autenticação
AUTH_LOG_LEVEL=DEBUG

# Módulo de billing/pagamentos
BILLING_LOG_LEVEL=DEBUG

# Módulo de APIs
API_LOG_LEVEL=DEBUG
```

## Níveis de Log

### DEBUG (Desenvolvimento)
- Mostra TUDO: debug, info, warn, error
- Útil para desenvolvimento e troubleshooting
- Gera muitos logs

```env
APP_LOG_LEVEL=DEBUG
AUTH_LOG_LEVEL=DEBUG
BILLING_LOG_LEVEL=DEBUG
API_LOG_LEVEL=DEBUG
```

### INFO (Padrão)
- Mostra: info, warn, error
- Oculta: debug
- Bom equilíbrio para staging

```env
APP_LOG_LEVEL=INFO
AUTH_LOG_LEVEL=INFO
BILLING_LOG_LEVEL=INFO
API_LOG_LEVEL=INFO
```

### WARN (Produção Leve)
- Mostra: warn, error
- Oculta: debug, info
- Menos verboso

```env
APP_LOG_LEVEL=WARN
AUTH_LOG_LEVEL=WARN
BILLING_LOG_LEVEL=WARN
API_LOG_LEVEL=WARN
```

### ERROR (Produção Mínima)
- Mostra apenas: error
- Mínimo de logs
- Melhor performance

```env
APP_LOG_LEVEL=ERROR
AUTH_LOG_LEVEL=ERROR
BILLING_LOG_LEVEL=ERROR
API_LOG_LEVEL=ERROR
```

## Como Usar nos Controllers/Services

### Exemplo Básico

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WithdrawalProcessingService {
    private static final Logger log = LoggerFactory.getLogger(WithdrawalProcessingService.class);
    
    public void processWithdrawal(Long withdrawalId) {
        log.debug("Iniciando processamento do withdrawal ID: {}", withdrawalId);
        
        try {
            // Lógica de processamento
            log.info("Withdrawal {} processado com sucesso", withdrawalId);
        } catch (Exception e) {
            log.error("Erro ao processar withdrawal {}: {}", withdrawalId, e.getMessage(), e);
            throw e;
        }
    }
}
```

### Níveis de Log no Código

```java
// DEBUG - Informações detalhadas para debug
log.debug("Valor calculado: {}, Taxa: {}", amount, fee);

// INFO - Informações importantes do fluxo
log.info("Usuário {} realizou login com sucesso", username);

// WARN - Avisos que não são erros mas merecem atenção
log.warn("Tentativa de acesso a recurso inexistente: {}", resourceId);

// ERROR - Erros que precisam ser investigados
log.error("Falha ao processar pagamento: {}", e.getMessage(), e);
```

## Formato dos Logs

### No Arquivo (logs/application.log)
```
2026-04-05 10:30:15.123 [http-nio-8080-exec-1] INFO  c.a.b.m.b.s.WithdrawalProcessingService - Withdrawal 123 processado com sucesso
2026-04-05 10:30:16.456 [http-nio-8080-exec-2] ERROR c.a.b.m.a.s.AuthService - Erro ao autenticar usuário: Invalid credentials
```

### No Console (mais limpo)
```
10:30:15.123 INFO  c.a.b.m.b.s.WithdrawalProcessingService - Withdrawal 123 processado com sucesso
10:30:16.456 ERROR c.a.b.m.a.s.AuthService - Erro ao autenticar usuário: Invalid credentials
```

## Rotação de Logs

Os logs são automaticamente rotacionados quando:
1. O arquivo atinge `LOG_FILE_MAX_SIZE` (padrão: 10MB)
2. Arquivos antigos são mantidos por `LOG_FILE_MAX_HISTORY` dias (padrão: 30)
3. Total de logs não excede `LOG_FILE_TOTAL_SIZE` (padrão: 100MB)

Exemplo de arquivos gerados:
```
logs/
├── application.log           (arquivo atual)
├── application.log.2026-04-04.0.gz
├── application.log.2026-04-03.0.gz
└── application.log.2026-04-02.0.gz
```

## Configurações Recomendadas

### Desenvolvimento Local
```env
LOG_FILE_PATH=logs/application.log
LOG_FILE_MAX_SIZE=10MB
LOG_FILE_MAX_HISTORY=7
LOG_FILE_TOTAL_SIZE=50MB

APP_LOG_LEVEL=DEBUG
AUTH_LOG_LEVEL=DEBUG
BILLING_LOG_LEVEL=DEBUG
API_LOG_LEVEL=DEBUG

# Habilitar SQL se precisar debugar queries
HIBERNATE_SHOW_SQL=false
HIBERNATE_LOG_LEVEL=ERROR
```

### Staging/Testes
```env
LOG_FILE_PATH=logs/application.log
LOG_FILE_MAX_SIZE=20MB
LOG_FILE_MAX_HISTORY=14
LOG_FILE_TOTAL_SIZE=200MB

APP_LOG_LEVEL=INFO
AUTH_LOG_LEVEL=INFO
BILLING_LOG_LEVEL=DEBUG
API_LOG_LEVEL=INFO

HIBERNATE_SHOW_SQL=false
HIBERNATE_LOG_LEVEL=WARN
```

### Produção
```env
LOG_FILE_PATH=/var/log/api-portal/application.log
LOG_FILE_MAX_SIZE=50MB
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE=1GB

APP_LOG_LEVEL=INFO
AUTH_LOG_LEVEL=WARN
BILLING_LOG_LEVEL=INFO
API_LOG_LEVEL=WARN

HIBERNATE_SHOW_SQL=false
HIBERNATE_LOG_LEVEL=ERROR
```

## Visualizar Logs em Tempo Real

### Windows PowerShell
```powershell
# Ver últimas linhas
Get-Content logs/application.log -Tail 50

# Seguir logs em tempo real
Get-Content logs/application.log -Wait -Tail 50
```

### Linux/Mac
```bash
# Ver últimas linhas
tail -n 50 logs/application.log

# Seguir logs em tempo real
tail -f logs/application.log

# Filtrar por nível
grep "ERROR" logs/application.log

# Filtrar por módulo
grep "WithdrawalProcessingService" logs/application.log
```

## Troubleshooting

### Logs não estão sendo gerados
1. Verifique se a pasta `logs/` existe (será criada automaticamente)
2. Verifique permissões de escrita na pasta
3. Verifique se `LOG_FILE_PATH` está configurado no `.env`
4. Reinicie a aplicação

### Arquivo de log muito grande
1. Reduza `LOG_FILE_MAX_SIZE` no `.env`
2. Reduza `LOG_FILE_MAX_HISTORY` no `.env`
3. Mude níveis de log para INFO ou WARN
4. Desabilite logs de módulos específicos

### Muitos logs de Hibernate
```env
HIBERNATE_SHOW_SQL=false
HIBERNATE_LOG_LEVEL=ERROR
```

### Logs de um módulo específico
Para debugar apenas o módulo de billing:
```env
APP_LOG_LEVEL=INFO
AUTH_LOG_LEVEL=INFO
BILLING_LOG_LEVEL=DEBUG  # Apenas billing em DEBUG
API_LOG_LEVEL=INFO
```

## Integração com Ferramentas

### ELK Stack (Elasticsearch, Logstash, Kibana)
Os logs estão no formato adequado para ingestão no Logstash.

### Splunk
Configure o Splunk para monitorar a pasta `logs/`.

### CloudWatch (AWS)
Use o CloudWatch Agent para enviar logs para AWS CloudWatch.

### Application Insights (Azure)
Configure o Application Insights SDK para enviar logs.

## Boas Práticas

1. ✅ Use níveis apropriados (DEBUG para detalhes, INFO para fluxo, ERROR para erros)
2. ✅ Inclua contexto nos logs (IDs, usernames, valores relevantes)
3. ✅ Use placeholders `{}` em vez de concatenação de strings
4. ✅ Sempre logue exceções com stack trace: `log.error("msg", e)`
5. ❌ Não logue informações sensíveis (senhas, tokens, cartões de crédito)
6. ❌ Não use `System.out.println()` - use o logger
7. ❌ Não logue em loops intensivos sem necessidade
8. ✅ Em produção, use INFO ou WARN para reduzir volume de logs
