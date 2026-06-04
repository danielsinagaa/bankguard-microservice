CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_ref VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    source_account_id BIGINT NOT NULL,
    destination_account_number VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    channel VARCHAR(30) NOT NULL,
    device_id VARCHAR(100),
    ip_address VARCHAR(50),
    location VARCHAR(100),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_RISK_CHECK',
    risk_score INT NOT NULL DEFAULT 0,
    risk_decision VARCHAR(30),
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_transactions_transaction_ref UNIQUE (transaction_ref),
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_transactions_source_account FOREIGN KEY (source_account_id)
        REFERENCES accounts(id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_currency CHECK (currency IN ('IDR')),
    CONSTRAINT chk_transactions_channel CHECK (
        channel IN ('MOBILE_BANKING', 'INTERNET_BANKING', 'ATM', 'BRANCH', 'BACK_OFFICE')
    ),
    CONSTRAINT chk_transactions_status CHECK (
        status IN ('PENDING_RISK_CHECK', 'APPROVED', 'REVIEW', 'BLOCKED', 'FAILED')
    ),
    CONSTRAINT chk_transactions_risk_decision CHECK (
        risk_decision IS NULL OR risk_decision IN ('APPROVED', 'REVIEW', 'BLOCKED')
    ),
    CONSTRAINT chk_transactions_risk_score_range CHECK (risk_score >= 0)
);

CREATE INDEX idx_transactions_source_account_id ON transactions(source_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_risk_score ON transactions(risk_score);
CREATE INDEX idx_transactions_destination_account ON transactions(destination_account_number);
CREATE INDEX idx_transactions_channel ON transactions(channel);
CREATE INDEX idx_transactions_status_created_at ON transactions(status, created_at);
CREATE INDEX idx_transactions_risk_created_at ON transactions(risk_score, created_at);
CREATE INDEX idx_transactions_decision_created_at ON transactions(risk_decision, created_at);
CREATE INDEX idx_transactions_source_created_at ON transactions(source_account_id, created_at);
CREATE INDEX idx_transactions_created_status_risk ON transactions(created_at, status, risk_score);
CREATE INDEX idx_transactions_destination_created ON transactions(destination_account_number, created_at);
CREATE INDEX idx_transactions_source_created ON transactions(source_account_id, created_at);

CREATE TABLE transaction_risk_factors (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    factor_code VARCHAR(50) NOT NULL,
    factor_description TEXT NOT NULL,
    score INT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transaction_risk_factors_transactions FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),
    CONSTRAINT chk_transaction_risk_factors_score_non_negative CHECK (score >= 0),
    CONSTRAINT uk_transaction_risk_factors_transaction_factor UNIQUE (transaction_id, factor_code)
);

CREATE INDEX idx_risk_factors_transaction_id ON transaction_risk_factors(transaction_id);
CREATE INDEX idx_risk_factors_factor_code ON transaction_risk_factors(factor_code);
CREATE INDEX idx_risk_factors_created_at ON transaction_risk_factors(created_at);
