CREATE TABLE transfers (
                           id UUID PRIMARY KEY,
                           sender_wallet_id UUID NOT NULL REFERENCES wallets(id),
                           receiver_wallet_id UUID NOT NULL REFERENCES wallets(id),
                           amount NUMERIC(19,4) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP NOT NULL,
                           CONSTRAINT chk_different_wallets CHECK (sender_wallet_id <> receiver_wallet_id)
);

CREATE INDEX idx_transfers_sender ON transfers(sender_wallet_id);
CREATE INDEX idx_transfers_receiver ON transfers(receiver_wallet_id);

-- Extend wallet_transactions to support linking two entries as one transfer
ALTER TABLE wallet_transactions ADD COLUMN transfer_id UUID REFERENCES transfers(id);
ALTER TABLE wallet_transactions ADD COLUMN counterparty_wallet_id UUID;