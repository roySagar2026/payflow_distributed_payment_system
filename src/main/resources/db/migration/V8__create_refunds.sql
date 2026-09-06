CREATE TABLE refunds (
                         id UUID PRIMARY KEY,
                         original_payment_order_id UUID NOT NULL REFERENCES payment_orders(id),
                         refund_transfer_id UUID REFERENCES transfers(id),
                         amount NUMERIC(19,4) NOT NULL,
                         reason VARCHAR(255),
                         status VARCHAR(20) NOT NULL,
                         created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_refunds_original_order ON refunds(original_payment_order_id);