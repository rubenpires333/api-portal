-- Migration para criar tabelas de Help (FAQ) com UUID

-- Tabela de categorias de ajuda
CREATE TABLE IF NOT EXISTS help_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Tabela de FAQs
CREATE TABLE IF NOT EXISTS help_faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_help_faqs_category FOREIGN KEY (category_id) REFERENCES help_categories(id) ON DELETE CASCADE
);

-- Criar índices para melhor performance
CREATE INDEX idx_help_categories_active ON help_categories(active) WHERE active = true;
CREATE INDEX idx_help_categories_display_order ON help_categories(display_order);
CREATE INDEX idx_help_faqs_category_id ON help_faqs(category_id);
CREATE INDEX idx_help_faqs_active ON help_faqs(active) WHERE active = true;
CREATE INDEX idx_help_faqs_display_order ON help_faqs(display_order);

-- Inserir categorias padrão
INSERT INTO help_categories (name, description, display_order, active) VALUES
('Primeiros Passos', 'Informações básicas para começar a usar a plataforma', 1, true),
('APIs', 'Perguntas sobre uso e integração de APIs', 2, true),
('Autenticação', 'Dúvidas sobre autenticação e segurança', 3, true),
('Faturamento', 'Questões sobre planos e pagamentos', 4, true);

-- Inserir FAQs padrão
INSERT INTO help_faqs (category_id, question, answer, display_order, active)
SELECT 
    id,
    'Como criar minha primeira API Key?',
    '<p>Para criar uma API Key, acesse o menu <strong>Configurações</strong> e clique em <strong>API Keys</strong>. Em seguida, clique no botão <strong>Nova API Key</strong> e preencha as informações solicitadas.</p>',
    1,
    true    
FROM help_categories WHERE name = 'Primeiros Passos';

INSERT INTO help_faqs (category_id, question, answer, display_order, active)
SELECT 
    id,
    'Como testar uma API?',
    '<p>Você pode testar APIs diretamente na plataforma usando nossa interface interativa. Acesse a documentação da API desejada e utilize o botão <strong>Testar</strong> disponível em cada endpoint.</p>',
    2,
    true
FROM help_categories WHERE name = 'APIs';
