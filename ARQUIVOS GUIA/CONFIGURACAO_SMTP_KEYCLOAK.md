# Configuração SMTP no Keycloak - Guia Completo

## Data
28 de Março de 2026

## Problema

Erro ao enviar email pelo Keycloak:
```
javax.net.ssl.SSLException: Unsupported or unrecognized SSL message
Could not connect to SMTP host: smtp.gmail.com, port: 587
```

## Causa

A porta 587 usa **STARTTLS** (não SSL direto). O Keycloak estava configurado incorretamente.

---

## Solução 1: Gmail com App Password (Recomendado para Desenvolvimento)

### Passo 1: Criar App Password no Gmail

1. Acessar: https://myaccount.google.com/apppasswords
2. Fazer login na conta Google
3. Selecionar "Mail" e "Other (Custom name)"
4. Nomear: "Keycloak API Portal"
5. Clicar em "Generate"
6. Copiar a senha gerada (16 caracteres sem espaços)

### Passo 2: Configurar no Keycloak

Acessar: http://localhost:8180/admin

**Realm Settings > Email:**

```
Host: smtp.gmail.com
Port: 587
From Display Name: API Portal
From: seu-email@gmail.com
Reply To: seu-email@gmail.com (opcional)
Reply To Display Name: API Portal Support (opcional)
Envelope From: (deixar vazio)

Enable StartTLS: ON
Enable SSL: OFF
Enable Authentication: ON

Username: seu-email@gmail.com
Password: xxxx xxxx xxxx xxxx (App Password de 16 dígitos)
```

### Passo 3: Testar Conexão

1. Clicar em "Save"
2. Clicar em "Test connection"
3. Deve aparecer: "Success! SMTP connection successful"

---

## Solução 2: MailHog (Recomendado para Desenvolvimento Local)

MailHog é um servidor SMTP fake que captura emails sem enviá-los.

### Instalação com Docker

```bash
docker run -d \
  --name mailhog \
  -p 1025:1025 \
  -p 8025:8025 \
  mailhog/mailhog
```

### Configuração no Keycloak

**Realm Settings > Email:**

```
Host: host.docker.internal (ou localhost se Keycloak não estiver no Docker)
Port: 1025
From: noreply@apiportal.local
Enable StartTLS: OFF
Enable SSL: OFF
Enable Authentication: OFF
```

### Acessar Interface Web

```
http://localhost:8025
```

Todos os emails enviados aparecerão aqui!

---

## Solução 3: Mailtrap (Recomendado para Testes)

Mailtrap é um serviço online para testar emails.

### Passo 1: Criar Conta

1. Acessar: https://mailtrap.io
2. Criar conta gratuita
3. Criar inbox

### Passo 2: Obter Credenciais

Em "SMTP Settings":
```
Host: sandbox.smtp.mailtrap.io
Port: 587 ou 2525
Username: (fornecido pelo Mailtrap)
Password: (fornecido pelo Mailtrap)
```

### Passo 3: Configurar no Keycloak

**Realm Settings > Email:**

```
Host: sandbox.smtp.mailtrap.io
Port: 587
From: noreply@apiportal.local
Enable StartTLS: ON
Enable SSL: OFF
Enable Authentication: ON
Username: (seu username do Mailtrap)
Password: (sua senha do Mailtrap)
```

---

## Solução 4: Outlook/Hotmail

### Configuração

```
Host: smtp-mail.outlook.com
Port: 587
From: seu-email@outlook.com
Enable StartTLS: ON
Enable SSL: OFF
Enable Authentication: ON
Username: seu-email@outlook.com
Password: sua-senha
```

**Nota:** Pode ser necessário habilitar "Less secure app access" nas configurações da conta.

---

## Solução 5: SendGrid (Produção)

### Passo 1: Criar Conta SendGrid

1. Acessar: https://sendgrid.com
2. Criar conta (plano gratuito: 100 emails/dia)
3. Verificar email

### Passo 2: Criar API Key

1. Settings > API Keys
2. Create API Key
3. Nome: "Keycloak API Portal"
4. Permissões: Full Access
5. Copiar API Key

### Passo 3: Configurar no Keycloak

```
Host: smtp.sendgrid.net
Port: 587
From: noreply@seu-dominio.com
Enable StartTLS: ON
Enable SSL: OFF
Enable Authentication: ON
Username: apikey
Password: (sua API Key do SendGrid)
```

---

## Comparação de Soluções

| Solução | Custo | Facilidade | Produção | Desenvolvimento |
|---------|-------|------------|----------|-----------------|
| Gmail | Grátis | Média | ❌ | ✅ |
| MailHog | Grátis | Fácil | ❌ | ✅✅✅ |
| Mailtrap | Grátis | Fácil | ❌ | ✅✅ |
| Outlook | Grátis | Média | ❌ | ✅ |
| SendGrid | Grátis/Pago | Média | ✅✅✅ | ✅ |

---

## Configuração Recomendada por Ambiente

### Desenvolvimento Local
**MailHog** - Não precisa de credenciais, captura todos os emails

### Testes/Staging
**Mailtrap** - Emails reais mas não são entregues, boa interface

### Produção
**SendGrid** ou **AWS SES** - Confiável, escalável, com analytics

---

## Troubleshooting

### Erro: "Unsupported or unrecognized SSL message"

**Causa:** Porta 587 com SSL habilitado

**Solução:**
- Enable StartTLS: ON
- Enable SSL: OFF

### Erro: "Authentication failed"

**Causa:** Credenciais incorretas ou 2FA habilitado

**Solução Gmail:**
1. Usar App Password (não a senha normal)
2. Desabilitar 2FA temporariamente (não recomendado)

### Erro: "Connection timeout"

**Causa:** Firewall bloqueando porta 587

**Solução:**
1. Verificar firewall
2. Tentar porta alternativa (2525 para Mailtrap)
3. Verificar se SMTP está habilitado no provedor

### Emails Vão para Spam

**Solução:**
1. Configurar SPF record no DNS
2. Configurar DKIM
3. Usar domínio verificado
4. Evitar palavras spam no assunto

---

## Testando Envio de Email

### Via Keycloak Admin

1. Realm Settings > Email
2. Configurar SMTP
3. Clicar em "Test connection"
4. Verificar mensagem de sucesso

### Via Registro de Usuário

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Verificar:**
1. Logs do backend: "Email de verificação enviado"
2. Logs do Keycloak: Sem erros
3. MailHog/Mailtrap: Email recebido

---

## Template de Email Personalizado

### Localização

```
keycloak/themes/seu-tema/email/html/email-verification.ftl
```

### Exemplo Básico

```html
<#import "template.ftl" as layout>
<@layout.emailLayout>
  <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
    <h1 style="color: #333;">Bem-vindo ao API Portal!</h1>
    
    <p>Olá ${user.firstName},</p>
    
    <p>Obrigado por se registrar no API Portal. Para completar seu cadastro, 
    por favor verifique seu email clicando no botão abaixo:</p>
    
    <div style="text-align: center; margin: 30px 0;">
      <a href="${link}" 
         style="background-color: #007bff; color: white; padding: 12px 30px; 
                text-decoration: none; border-radius: 5px; display: inline-block;">
        Verificar Email
      </a>
    </div>
    
    <p style="color: #666; font-size: 12px;">
      Este link expira em ${linkExpirationFormatter(linkExpiration)}.
    </p>
    
    <p style="color: #666; font-size: 12px;">
      Se você não criou esta conta, ignore este email.
    </p>
    
    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
    
    <p style="color: #999; font-size: 11px; text-align: center;">
      © 2026 API Portal. Todos os direitos reservados.
    </p>
  </div>
</@layout.emailLayout>
```

---

## Configuração Docker Compose (MailHog)

Adicionar ao `docker-compose.yml`:

```yaml
services:
  mailhog:
    image: mailhog/mailhog:latest
    container_name: mailhog
    ports:
      - "1025:1025"  # SMTP
      - "8025:8025"  # Web UI
    networks:
      - api-portal-network
```

Iniciar:
```bash
docker-compose up -d mailhog
```

Acessar: http://localhost:8025

---

## Variáveis de Ambiente (Opcional)

Para configurar SMTP via variáveis de ambiente no Keycloak:

```bash
KC_SMTP_HOST=smtp.gmail.com
KC_SMTP_PORT=587
KC_SMTP_FROM=noreply@apiportal.com
KC_SMTP_FROM_DISPLAY_NAME=API Portal
KC_SMTP_STARTTLS=true
KC_SMTP_SSL=false
KC_SMTP_AUTH=true
KC_SMTP_USER=seu-email@gmail.com
KC_SMTP_PASSWORD=sua-app-password
```

---

## Checklist de Configuração

- [ ] Escolher provedor SMTP (MailHog para dev)
- [ ] Obter credenciais (se necessário)
- [ ] Configurar no Keycloak (Realm Settings > Email)
- [ ] Testar conexão
- [ ] Registrar usuário de teste
- [ ] Verificar recebimento do email
- [ ] Clicar no link de verificação
- [ ] Confirmar que email foi verificado

---

## Recomendação Final

**Para Desenvolvimento:**
```
Use MailHog - Simples, rápido, sem configuração
```

**Para Produção:**
```
Use SendGrid ou AWS SES - Confiável e escalável
```

**Configuração MailHog no Keycloak:**
```
Host: localhost (ou host.docker.internal se Keycloak no Docker)
Port: 1025
From: noreply@apiportal.local
Enable StartTLS: OFF
Enable SSL: OFF
Enable Authentication: OFF
```

---

**Status**: Guia completo de configuração SMTP  
**Próximo**: Configurar MailHog e testar envio de email
