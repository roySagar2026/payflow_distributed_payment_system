CREATE TABLE payment_orders (
                                id UUID PRIMARY KEY,
                                payer_wallet_id UUID NOT NULL REFERENCES wallets(id),
                                payee_wallet_id UUID NOT NULL REFERENCES wallets(id),
                                amount NUMERIC(19,4) NOT NULL,
                                status VARCHAR(20) NOT NULL,
                                failure_reason VARCHAR(255),
                                transfer_id UUID REFERENCES transfers(id),
                                version BIGINT NOT NULL DEFAULT 0,
                                created_at TIMESTAMP NOT NULL,
                                updated_at TIMESTAMP NOT NULL,
                                CONSTRAINT chk_order_different_wallets CHECK (payer_wallet_id <> payee_wallet_id)
);

CREATE INDEX idx_payment_orders_payer ON payment_orders(payer_wallet_id);
CREATE INDEX idx_payment_orders_status ON payment_orders(status);