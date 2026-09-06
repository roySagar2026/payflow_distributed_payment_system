CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255) NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL
);

CREATE TABLE wallets (
                         id UUID PRIMARY KEY,
                         user_id UUID NOT NULL UNIQUE REFERENCES users(id),
                         balance NUMERIC(19,4) NOT NULL DEFAULT 0,
                         currency VARCHAR(3) NOT NULL,
                         version BIGINT NOT NULL DEFAULT 0,
                         created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_wallets_user_id ON wallets(user_id);