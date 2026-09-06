CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               user_wallet_id UUID NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               message TEXT NOT NULL,
                               payment_order_id UUID,
                               created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notifications_wallet ON notifications(user_wallet_id);