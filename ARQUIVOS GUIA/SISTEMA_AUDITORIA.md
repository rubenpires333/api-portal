# Sistema de Auditoria - API Portal

## Visão Geral

O sistema de auditoria foi implementado para rastrear automaticamente todas as alterações nas entidades do sistema e registrar todas as requisições HTTP feitas à API.

## Componentes Principais

### 1. Auditoria Automática de Entidades (JPA Auditing)

Todas as entidades que estendem `Auditable` possuem rastreamento automático de:

- `createdAt`: Data/hora de criação
- `updatedAt`: Data/hora da última atualização
- `createdBy`: ID do usuário que criou o registro
- `lastModifiedBy`: ID do usuário que fez a última modificação

#### Entidades com Auditoria Automática

- `Api`
- `ApiVersion`
- `ApiEndpoint`
- `ApiCategory`
- `ApiKey`
- `AuditLog`

#### Como Funciona

O sistema captura automaticamente o usuário autenticado do JWT token através do `AuditorAwareImpl` e preenche os campos de auditoria antes de salvar no banco de dados.

```java
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class MinhaEntidade extends Auditable {
    // Seus campos aqui
    // Os campos de auditoria são herdados automaticamente
}
```

### 2. Log de Requisições HTTP

Todas as requisições HTTP são registradas na tabela `audit_logs` com as seguintes informações:

- Método HTTP (GET, POST, PUT, DELETE, etc.)
- URI da requisição
- Parâmetros da query string
- Request body (para POST/PUT)
- Response body
- Status HTTP da resposta
- Tempo de processamento (em ms)
- Endereço IP do cliente
- User Agent
- ID do usuário autenticado
- Data/hora da requisição

#### Como Funciona

1. `ContentCachingFilter`: Captura o corpo da requisição e resposta
2. `AuditInterceptor`: Intercepta a requisição e coleta metadados
3. `AuditService`: Salva o log de forma assíncrona no banco de dados

### 3. Cache com Redis

O sistema utiliza Redis para cache de dados frequentemente acessados, reduzindo consultas ao banco de dados.

#### Caches Configurados

- `categories`: Cache de categorias de APIs (TTL: 1 hora)
- `apis`: Cache de APIs (TTL: 30 minutos)
- `apiVersions`: Cache de versões de APIs (TTL: 30 minutos)
- `apiEndpoints`: Cache de endpoints de APIs (TTL: 15 minutos)

#### Estratégias de Cache

- `@Cacheable`: Armazena o resultado no cache
- `@CachePut`: Atualiza o cache após a operação
- `@CacheEvict`: Remove entradas do cache

## Configuração

### Variáveis de Ambiente (.env)

```properties
# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### Docker Compose

O Redis está configurado no `docker-compose.yml`:

```yaml
redis:
  image: redis:7-alpine
  container_name: apiportal-redis
  restart: unless-stopped
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  command: redis-server --appendonly yes
```

## Endpoints de Auditoria

### Listar Logs de Auditoria (SUPER_ADMIN apenas)

```http
GET /api/v1/audit/logs?page=0&size=20
Authorization: Bearer {token}
```

### Buscar Logs por Usuário

```http
GET /api/v1/audit/logs/user/{userId}?page=0&size=20
Authorization: Bearer {token}
```

### Buscar Logs por Método HTTP

```http
GET /api/v1/audit/logs/method/{method}?page=0&size=20
Authorization: Bearer {token}
```

Métodos disponíveis: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`

### Buscar Logs por URI

```http
GET /api/v1/audit/logs/uri?uri=/api/v1/apis&page=0&size=20
Authorization: Bearer {token}
```

### Buscar Logs por Período

```http
GET /api/v1/audit/logs/date-range?startDate=2026-03-01T00:00:00&endDate=2026-03-28T23:59:59&page=0&size=20
Authorization: Bearer {token}
```

## Exemplo de Resposta

```json
{
  "content": [
    {
      "id": 1,
      "method": "POST",
      "uri": "/api/v1/apis",
      "queryParams": null,
      "requestBody": "{\"name\":\"API Teste\",\"baseUrl\":\"https://api.teste.cv\"}",
      "responseBody": "{\"id\":\"123e4567-e89b-12d3-a456-426614174000\",\"name\":\"API Teste\"}",
      "statusCode": 201,
      "executionTime": 245,
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "userId": "2ac41dbe-2781-4305-9700-51c92e55a70a",
      "timestamp": "2026-03-28T10:30:45",
      "createdAt": "2026-03-28T10:30:45",
      "updatedAt": "2026-03-28T10:30:45",
      "createdBy": "2ac41dbe-2781-4305-9700-51c92e55a70a",
      "lastModifiedBy": "2ac41dbe-2781-4305-9700-51c92e55a70a"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

## Iniciar Redis

### Via Docker Compose

```bash
docker-compose up -d redis
```

### Verificar Status

```bash
docker-compose ps redis
```

### Ver Logs

```bash
docker-compose logs -f redis
```

### Conectar ao Redis CLI

```bash
docker exec -it apiportal-redis redis-cli
```

Comandos úteis no Redis CLI:

```redis
# Ver todas as chaves
KEYS *

# Ver valor de uma chave
GET categories::all

# Limpar todo o cache
FLUSHALL

# Ver informações do servidor
INFO

# Monitorar comandos em tempo real
MONITOR
```

## Performance

### Processamento Assíncrono

Os logs de auditoria são salvos de forma assíncrona usando `@Async`, não impactando o tempo de resposta das requisições.

### Cache Redis

O cache Redis reduz significativamente o número de consultas ao banco de dados:

- Primeira requisição: Consulta o banco e armazena no cache
- Requisições subsequentes: Retorna diretamente do cache
- Atualizações: Invalidam o cache automaticamente

## Boas Práticas

1. **Não desabilitar auditoria**: Todas as entidades importantes devem estender `Auditable`
2. **Monitorar logs regularmente**: Use os endpoints de auditoria para identificar problemas
3. **Limpar logs antigos**: Implemente rotinas de limpeza para logs muito antigos
4. **Proteger endpoints de auditoria**: Apenas SUPER_ADMIN deve ter acesso
5. **Monitorar Redis**: Verifique o uso de memória e performance do Redis

## Troubleshooting

### Redis não conecta

```bash
# Verificar se o Redis está rodando
docker-compose ps redis

# Reiniciar Redis
docker-compose restart redis

# Ver logs de erro
docker-compose logs redis
```

### Cache não está funcionando

1. Verificar se o Redis está rodando
2. Verificar configurações no `application.properties`
3. Verificar se as anotações de cache estão corretas
4. Limpar o cache: `FLUSHALL` no Redis CLI

### Logs de auditoria não aparecem

1. Verificar se o usuário está autenticado
2. Verificar se o `AuditInterceptor` está registrado
3. Verificar logs da aplicação para erros
4. Verificar se a tabela `audit_logs` existe no banco

## Próximos Passos

- [ ] Implementar rotina de limpeza de logs antigos
- [ ] Adicionar dashboard de auditoria
- [ ] Implementar alertas para ações suspeitas
- [ ] Adicionar exportação de logs em CSV/Excel
- [ ] Implementar filtros avançados de busca
