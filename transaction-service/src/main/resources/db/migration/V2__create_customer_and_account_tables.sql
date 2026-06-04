CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    cif_number VARCHAR(30) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone_number VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_customers_cif_number UNIQUE (cif_number),
    CONSTRAINT chk_customers_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')
    )
);

CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_full_name ON customers(full_name);

CREATE TABLE customer_risk_profiles (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    risk_reason TEXT,
    last_reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_customer_risk_profiles_customer_id UNIQUE (customer_id),
    CONSTRAINT fk_customer_risk_profiles_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT chk_customer_risk_profiles_level CHECK (
        risk_level IN ('LOW', 'MEDIUM', 'HIGH')
    )
);

CREATE INDEX idx_customer_risk_profiles_risk_level ON customer_risk_profiles(risk_level);

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'IDR',
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT chk_accounts_type CHECK (
        account_type IN ('SAVINGS', 'CURRENT', 'PAYROLL')
    ),
    CONSTRAINT chk_accounts_currency CHECK (
        currency IN ('IDR')
    ),
    CONSTRAINT chk_accounts_status CHECK (
        status IN ('ACTIVE', 'FROZEN', 'BLOCKED', 'CLOSED')
    ),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_currency ON accounts(currency);
