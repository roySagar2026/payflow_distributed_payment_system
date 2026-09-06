CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY,
                            user_id UUID,
                            action VARCHAR(100) NOT NULL,
                            resource_type VARCHAR(50),
                            resource_id VARCHAR(100),
                            outcome VARCHAR(20) NOT NULL,
                            details TEXT,
                            ip_address VARCHAR(45),
                            created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);