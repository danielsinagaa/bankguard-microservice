CREATE TABLE blacklisted_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(30) NOT NULL,
    reason TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_blacklisted_accounts_account_number UNIQUE (account_number)
);

CREATE INDEX idx_blacklisted_accounts_active ON blacklisted_accounts(active);
CREATE INDEX idx_blacklisted_accounts_account_active ON blacklisted_accounts(account_number, active);

CREATE TABLE customer_devices (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(100),
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_devices_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT uk_customer_devices_customer_device UNIQUE (customer_id, device_id)
);

CREATE INDEX idx_customer_devices_customer_id ON customer_devices(customer_id);
CREATE INDEX idx_customer_devices_trusted ON customer_devices(trusted);
CREATE INDEX idx_customer_devices_last_used_at ON customer_devices(last_used_at);

CREATE TABLE customer_locations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    location VARCHAR(100) NOT NULL,
    usage_count INT NOT NULL DEFAULT 1,
    first_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT fk_customer_locations_customers FOREIGN KEY (customer_id)
        REFERENCES customers(id),
    CONSTRAINT uk_customer_locations_customer_location UNIQUE (customer_id, location),
    CONSTRAINT chk_customer_locations_usage_count_positive CHECK (usage_count >= 1)
);

CREATE INDEX idx_customer_locations_customer_id ON customer_locations(customer_id);
CREATE INDEX idx_customer_locations_location ON customer_locations(location);
CREATE INDEX idx_customer_locations_last_used_at ON customer_locations(last_used_at);

CREATE TABLE risk_rule_configs (
    id BIGSERIAL PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    score INT NOT NULL,
    threshold_value NUMERIC(19,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_risk_rule_configs_rule_code UNIQUE (rule_code),
    CONSTRAINT chk_risk_rule_configs_score_non_negative CHECK (score >= 0)
);

CREATE INDEX idx_risk_rule_configs_active ON risk_rule_configs(active);
