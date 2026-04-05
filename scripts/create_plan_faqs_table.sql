-- Criar tabela de FAQs de Planos
CREATE TABLE IF NOT EXISTS plan_faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Criar índice para busca de FAQs ativas ordenadas
CREATE INDEX idx_plan_faqs_active_order ON plan_faqs(active, display_order);

-- Inserir FAQs padrão
INSERT INTO plan_faqs (question, answer, display_order, active) VALUES
('Posso mudar de plano depois?', 'Sim! Você pode fazer upgrade ou downgrade a qualquer momento. As alterações entram em vigor imediatamente e o valor é ajustado proporcionalmente.', 1, true),
('Como funciona o pagamento?', 'O pagamento é processado mensalmente via Stripe de forma automática. Você receberá uma fatura por email após cada cobrança.', 2, true),
('Há período de teste?', 'O plano Starter é gratuito para sempre. Você pode testar a plataforma antes de fazer upgrade para planos pagos.', 3, true),
('Posso cancelar minha assinatura?', 'Sim, você pode cancelar a qualquer momento sem taxas adicionais. O acesso permanece ativo até o final do período pago.', 4, true);

-- Comentários
COMMENT ON TABLE plan_faqs IS 'FAQs sobre planos de assinatura exibidas para providers';
COMMENT ON COLUMN plan_faqs.question IS 'Pergunta da FAQ';
COMMENT ON COLUMN plan_faqs.answer IS 'Resposta da FAQ';
COMMENT ON COLUMN plan_faqs.display_order IS 'Ordem de exibição (menor primeiro)';
COMMENT ON COLUMN plan_faqs.active IS 'Se a FAQ está ativa e visível';
