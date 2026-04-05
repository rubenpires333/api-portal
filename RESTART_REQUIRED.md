# ⚠️ REINICIAR APLICAÇÃO NECESSÁRIO

## Alteração Realizada

Configuração de auto-aprovação de levantamentos foi alterada:

**ANTES:**
```
AUTO_APPROVE_THRESHOLD=50.00
```
Levantamentos <= 50 EUR eram aprovados automaticamente.

**AGORA:**
```
AUTO_APPROVE_THRESHOLD=0.00
```
TODOS os levantamentos precisam aprovação manual do admin.

---

## Como Reiniciar

### Opção 1: Via IDE (IntelliJ/Eclipse)
1. Parar a aplicação (botão Stop)
2. Iniciar novamente (botão Run)

### Opção 2: Via Maven
```bash
# Parar aplicação atual (Ctrl+C)
# Iniciar novamente
mvn spring-boot:run
```

### Opção 3: Via JAR
```bash
# Parar aplicação atual (Ctrl+C)
# Iniciar novamente
java -jar target/api-portal-backend-0.0.1-SNAPSHOT.jar
```

---

## Verificar Configuração Carregada

Após reiniciar, verifique nos logs:

```bash
grep "auto-approve-threshold" logs/application.log
```

Ou faça um teste:
1. Solicite levantamento de qualquer valor (ex: 10 EUR)
2. Verifique que status é `PENDING_APPROVAL` (não `APPROVED`)

---

## Arquivos Alterados

- `.env` - Configuração local
- `.env.example` - Template de configuração
- `src/main/resources/application.properties` - Valor padrão
- `src/main/resources/application-billing.properties` - Valor padrão
- `WITHDRAWAL_TESTING_GUIDE.md` - Documentação atualizada

---

## ✅ Após Reiniciar

Todos os novos levantamentos ficarão com status `PENDING_APPROVAL` e precisarão ser aprovados manualmente pelo admin via:

```bash
POST /api/v1/admin/withdrawals/{id}/approve?adminId={ADMIN_ID}
```

Ou rejeitados via:

```bash
POST /api/v1/admin/withdrawals/{id}/reject?adminId={ADMIN_ID}&reason=motivo
```
