-- Executar manualmente se não quiser reiniciar a aplicação
-- Este é o mesmo conteúdo da migração V21

-- Verificar se as colunas já existem
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'wallet_transactions' 
        AND column_name = 'stripe_payment_intent_id'
    ) THEN
        -- Add payment details columns to wallet_transactions table
        ALTER TABLE wallet_transactions 
        ADD COLUMN stripe_payment_intent_id VARCHAR(255),
        ADD COLUMN stripe_invoice_id VARCHAR(255),
        ADD COLUMN stripe_invoice_number VARCHAR(255),
        ADD COLUMN payment_method_type VARCHAR(50),
        ADD COLUMN card_brand VARCHAR(50),
        ADD COLUMN card_last4 VARCHAR(4),
        ADD COLUMN receipt_url TEXT,
        ADD COLUMN invoice_pdf_url TEXT;

        -- Create indexes for faster lookups
        CREATE INDEX idx_wallet_transactions_payment_intent 
        ON wallet_transactions(stripe_payment_intent_id);

        CREATE INDEX idx_wallet_transactions_invoice 
        ON wallet_transactions(stripe_invoice_id);

        -- Add comments for documentation
        COMMENT ON COLUMN wallet_transactions.stripe_payment_intent_id IS 'Stripe Payment Intent ID';
        COMMENT ON COLUMN wallet_transactions.stripe_invoice_id IS 'Stripe Invoice ID';
        COMMENT ON COLUMN wallet_transactions.stripe_invoice_number IS 'Human-readable invoice number (e.g., INV-1234)';
        COMMENT ON COLUMN wallet_transactions.payment_method_type IS 'Payment method type (card, sepa_debit, etc)';
        COMMENT ON COLUMN wallet_transactions.card_brand IS 'Card brand (visa, mastercard, amex, etc)';
        COMMENT ON COLUMN wallet_transactions.card_last4 IS 'Last 4 digits of card';
        COMMENT ON COLUMN wallet_transactions.receipt_url IS 'Stripe receipt URL';
        COMMENT ON COLUMN wallet_transactions.invoice_pdf_url IS 'Stripe invoice PDF URL';

        RAISE NOTICE '✓ Colunas de payment details adicionadas com sucesso';
    ELSE
        RAISE NOTICE '⚠ Colunas já existem, nada a fazer';
    END IF;
END $$;
