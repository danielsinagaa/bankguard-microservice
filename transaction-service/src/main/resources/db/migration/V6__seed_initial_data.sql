INSERT INTO roles (role_code, role_name) VALUES
('ROLE_ADMIN', 'Administrator'),
('ROLE_BACKOFFICE', 'Back Office User'),
('ROLE_FRAUD_ANALYST', 'Fraud Analyst'),
('ROLE_SYSTEM', 'System Service');

INSERT INTO users (username, password_hash, full_name, active) VALUES
('admin', '{bcrypt}$2a$10$W5T7k8r0iJrQXw9lK2qvqe1l0XLSGj1YHku1qIcOArQ1EZkGxUYIK', 'System Administrator', true),
('backoffice', '{bcrypt}$2a$10$W5T7k8r0iJrQXw9lK2qvqe1l0XLSGj1YHku1qIcOArQ1EZkGxUYIK', 'Back Office User', true),
('analyst', '{bcrypt}$2a$10$W5T7k8r0iJrQXw9lK2qvqe1l0XLSGj1YHku1qIcOArQ1EZkGxUYIK', 'Fraud Analyst', true);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'ROLE_ADMIN'
WHERE u.username = 'admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'ROLE_BACKOFFICE'
WHERE u.username = 'backoffice';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'ROLE_FRAUD_ANALYST'
WHERE u.username = 'analyst';

INSERT INTO customers (cif_number, full_name, email, phone_number, status)
VALUES
('CIF001', 'Daniel Sinaga', 'daniel.sinaga@example.com', '081111111111', 'ACTIVE'),
('CIF002', 'Maria Hutabarat', 'maria.hutabarat@example.com', '082222222222', 'ACTIVE'),
('CIF003', 'Budi Sitorus', 'budi.sitorus@example.com', '083333333333', 'SUSPENDED');

INSERT INTO customer_risk_profiles (customer_id, risk_level, risk_reason)
SELECT id, 'LOW', 'Default customer profile'
FROM customers
WHERE cif_number = 'CIF001';

INSERT INTO customer_risk_profiles (customer_id, risk_level, risk_reason)
SELECT id, 'HIGH', 'Customer requires enhanced monitoring'
FROM customers
WHERE cif_number = 'CIF002';

INSERT INTO customer_risk_profiles (customer_id, risk_level, risk_reason)
SELECT id, 'MEDIUM', 'Customer requires periodic review'
FROM customers
WHERE cif_number = 'CIF003';

INSERT INTO accounts (customer_id, account_number, account_type, currency, balance, status)
SELECT id, '1234567890', 'SAVINGS', 'IDR', 100000000, 'ACTIVE'
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO accounts (customer_id, account_number, account_type, currency, balance, status)
SELECT id, '2234567890', 'SAVINGS', 'IDR', 200000000, 'ACTIVE'
FROM customers WHERE cif_number = 'CIF002';

INSERT INTO accounts (customer_id, account_number, account_type, currency, balance, status)
SELECT id, '3234567890', 'SAVINGS', 'IDR', 50000000, 'BLOCKED'
FROM customers WHERE cif_number = 'CIF003';

INSERT INTO blacklisted_accounts (account_number, reason, active, created_by)
VALUES
('9876543210', 'Reported mule account', true, 'system'),
('8876543210', 'Previously reviewed', false, 'system');

INSERT INTO customer_devices (customer_id, device_id, device_name, trusted)
SELECT id, 'DEVICE-TRUSTED-001', 'Daniel iPhone', true
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO customer_devices (customer_id, device_id, device_name, trusted)
SELECT id, 'DEVICE-UNTRUSTED-001', 'Unknown Android', false
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO customer_locations (customer_id, location, usage_count)
SELECT id, 'Jakarta', 10
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO customer_locations (customer_id, location, usage_count)
SELECT id, 'Batam', 3
FROM customers WHERE cif_number = 'CIF001';

INSERT INTO customer_locations (customer_id, location, usage_count)
SELECT id, 'Medan', 5
FROM customers WHERE cif_number = 'CIF002';

INSERT INTO risk_rule_configs (rule_code, rule_name, score, threshold_value, active, description)
VALUES
('HIGH_AMOUNT', 'High Amount Transaction', 25, 10000000, true, 'Triggered when transaction amount exceeds threshold'),
('NEW_DEVICE', 'New or Untrusted Device', 20, NULL, true, 'Triggered when device is not trusted'),
('BLACKLISTED_DESTINATION', 'Blacklisted Destination Account', 50, NULL, true, 'Triggered when destination account is blacklisted'),
('HIGH_FREQUENCY_TRANSACTION_COUNT', 'High Frequency Transaction Count', 30, 5, true, 'Triggered when transaction count exceeds threshold within 10 minutes'),
('HIGH_FREQUENCY_TRANSACTION_AMOUNT', 'High Frequency Transaction Amount', 30, 50000000, true, 'Triggered when accumulated transaction amount exceeds threshold within 10 minutes'),
('UNUSUAL_LOCATION', 'Unusual Transaction Location', 20, NULL, true, 'Triggered when transaction location is not known for customer'),
('HIGH_RISK_CUSTOMER_PROFILE', 'High Risk Customer Profile', 25, NULL, true, 'Triggered when customer risk profile is HIGH');
