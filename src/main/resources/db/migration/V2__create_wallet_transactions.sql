CREATE TABLE wallet_transactions (
                                     id UUID PRIMARY KEY,
                                     wallet_id UUID NOT NULL REFERENCES wallets(id),
                                     type VARCHAR(20) NOT NULL,
                                     amount NUMERIC(19,4) NOT NULL,
                                     balance_after NUMERIC(19,4) NOT NULL,
                                     status VARCHAR(20) NOT NULL,
                                     created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);