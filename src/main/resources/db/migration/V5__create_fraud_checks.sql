CREATE TABLE fraud_checks (
                              id UUID PRIMARY KEY,
                              payment_order_id UUID NOT NULL REFERENCES payment_orders(id),
                              risk_score INT NOT NULL,
                              decision VARCHAR(20) NOT NULL,
                              triggered_rules TEXT,
                              created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_fraud_checks_order ON fraud_checks(payment_order_id);