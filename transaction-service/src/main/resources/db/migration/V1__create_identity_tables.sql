CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE INDEX idx_users_active ON users(active);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,

    CONSTRAINT uk_roles_role_code UNIQUE (role_code),
    CONSTRAINT chk_roles_role_code CHECK (
        role_code IN ('ROLE_ADMIN', 'ROLE_BACKOFFICE', 'ROLE_FRAUD_ANALYST', 'ROLE_SYSTEM')
    )
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_users FOREIGN KEY (user_id)
        REFERENCES users(id),
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id)
        REFERENCES roles(id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
