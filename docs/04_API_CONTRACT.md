# API Contract
# BankGuard - Real-Time Fraud Detection and Transaction Intelligence Platform

---

## 1. Document Overview

### 1.1 Purpose

This document defines the REST API contract for **BankGuard**.

The API contract provides implementation-level details for request headers, authentication, authorization, endpoint behavior, request payloads, response payloads, validation rules, error responses, pagination, sorting, and API ownership.

This document is aligned with:

- `01_PRD.md`
- `02_SYSTEM_DESIGN_ARCHITECTURE.md`
- `03_TRD_TECHNICAL_REQUIREMENTS.md`

### 1.2 API Scope

This contract covers APIs required for the MVP:

1. Authentication.
2. Transaction submission.
3. Transaction detail and investigation.
4. Fraud audit search.
5. Master data management.
6. Native SQL reporting.
7. Health check endpoints.

### 1.3 Service Ownership

| API Area | Owning Service |
|---|---|
| Authentication | Transaction Service for MVP |
| Transaction Submission | Transaction Service |
| Transaction Detail | Transaction Service |
| Native SQL Reporting | Transaction Service |
| Fraud Audit Search | Audit Search Service |
| Blacklisted Account Management | Master Data Service |
| Trusted Device Management | Master Data Service |
| Customer Risk Profile Management | Master Data Service |
| Risk Rule Configuration | Master Data Service |

---

## 2. Common API Standards

### 2.1 Base URL Convention

Each service runs independently.

Local development examples:

```text
Transaction Service     http://localhost:18081
Risk Engine Service     internal Kafka consumer only
Audit Search Service    http://localhost:18083
Master Data Service     http://localhost:18084
```

Docker Compose keeps the internal service ports as `8081` to `8084`, but maps host ports to `18081` to `18084` by default to avoid common local port reservations.

All public API paths must start with:

```text
/api/v1
```

### 2.2 Content Type

All APIs must use JSON.

```http
Content-Type: application/json
Accept: application/json
```

### 2.3 Authentication

All protected APIs must include:

```http
Authorization: Bearer <jwt>
```

### 2.4 Request Correlation

All APIs should accept:

```http
X-Request-Id: <request-id>
```

If the client does not provide `X-Request-Id`, the receiving service must generate one and include it in logs and error responses.

### 2.5 Date and Time Format

All date-time fields must use ISO-8601 UTC format.

Example:

```text
2026-06-04T10:00:00Z
```

Date-only query parameters must use:

```text
yyyy-MM-dd
```

Example:

```text
2026-06-04
```

### 2.6 Currency

The MVP uses `IDR` as the default currency.

Currency must be sent as ISO currency code.

Example:

```json
{
  "currency": "IDR"
}
```

### 2.7 Money Format

All monetary values are represented as JSON numbers.

Example:

```json
{
  "amount": 25000000
}
```

Implementation must use `BigDecimal` internally.

### 2.8 Account Number Masking

All account numbers returned by APIs must be masked.

Masking rule:

```text
If account number length >= 7:
    keep first 3 characters
    keep last 3 characters
    replace middle characters with ****

Example:
1234567890 -> 123****890
```

If account number length is less than 7:

```text
Mask all except last 2 characters.
```

### 2.9 Pagination Standard

Paginated APIs must use:

| Parameter | Type | Required | Default | Description |
|---|---:|---:|---:|---|
| page | integer | no | 0 | Zero-based page number |
| size | integer | no | 20 | Page size |

Maximum allowed size:

```text
100
```

Paginated response format:

```json
{
  "data": [],
  "page": 0,
  "size": 20,
  "total": 0
}
```

### 2.10 Sorting Standard

If sorting is supported, APIs use:

| Parameter | Type | Required | Example |
|---|---|---:|---|
| sortBy | string | no | createdAt |
| sortDirection | string | no | DESC |

Allowed sort directions:

```text
ASC
DESC
```

---

## 3. Common Error Contract

### 3.1 Error Response Format

All API errors must use the following format:

```json
{
  "timestamp": "2026-06-04T10:00:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be greater than zero"
    }
  ],
  "path": "/api/v1/transactions",
  "requestId": "req-001"
}
```

### 3.2 Error Detail Object

| Field | Type | Required | Description |
|---|---|---:|---|
| field | string | no | Field related to the error |
| message | string | yes | Human-readable error detail |

### 3.3 Standard Error Codes

| HTTP Status | Error Code | Description |
|---:|---|---|
| 400 | VALIDATION_ERROR | Request body or parameter validation failed |
| 400 | IDEMPOTENCY_KEY_REQUIRED | Idempotency-Key header is missing |
| 400 | INVALID_CHANNEL | Transaction channel is invalid |
| 400 | INVALID_IP_ADDRESS | IP address format is invalid |
| 400 | INVALID_DATE_RANGE | Start date or end date is invalid |
| 400 | INVALID_PAGE_SIZE | Page size exceeds maximum allowed value |
| 401 | UNAUTHORIZED | JWT is missing, invalid, or expired |
| 403 | FORBIDDEN | Authenticated user does not have required role |
| 404 | TRANSACTION_NOT_FOUND | Transaction reference does not exist |
| 404 | SOURCE_ACCOUNT_NOT_FOUND | Source account does not exist |
| 404 | CUSTOMER_NOT_FOUND | Customer does not exist |
| 404 | BLACKLISTED_ACCOUNT_NOT_FOUND | Blacklisted account record does not exist |
| 404 | RISK_RULE_NOT_FOUND | Risk rule configuration does not exist |
| 409 | SOURCE_ACCOUNT_INACTIVE | Source account exists but is not active |
| 409 | DUPLICATE_RESOURCE | Resource already exists |
| 500 | INTERNAL_SERVER_ERROR | Unexpected server error |
| 500 | EVENT_PUBLISH_FAILED | Kafka event publishing failed |
| 503 | DEPENDENCY_UNAVAILABLE | External dependency such as PostgreSQL, Kafka, Redis, or Elasticsearch is unavailable |

---

## 4. Authorization Matrix

### 4.1 Roles

| Role | Description |
|---|---|
| ROLE_ADMIN | Manages system configuration and master data |
| ROLE_BACKOFFICE | Submits and reviews transactions |
| ROLE_FRAUD_ANALYST | Searches suspicious activity and reads reports |
| ROLE_SYSTEM | Internal service-to-service role if required |

### 4.2 Endpoint Access Matrix

| Endpoint | ROLE_ADMIN | ROLE_BACKOFFICE | ROLE_FRAUD_ANALYST |
|---|---:|---:|---:|
| POST /api/v1/auth/login | Yes | Yes | Yes |
| POST /api/v1/transactions | Yes | Yes | No |
| GET /api/v1/transactions/{transactionRef} | Yes | Yes | Yes |
| GET /api/v1/audits/search | Yes | No | Yes |
| GET /api/v1/reports/** | Yes | No | Yes |
| POST /api/v1/blacklisted-accounts | Yes | No | No |
| GET /api/v1/blacklisted-accounts | Yes | No | No |
| GET /api/v1/blacklisted-accounts/{id} | Yes | No | No |
| PUT /api/v1/blacklisted-accounts/{id} | Yes | No | No |
| DELETE /api/v1/blacklisted-accounts/{id} | Yes | No | No |
| POST /api/v1/customers/{customerId}/trusted-devices | Yes | No | No |
| GET /api/v1/customers/{customerId}/trusted-devices | Yes | No | No |
| PUT /api/v1/customers/{customerId}/trusted-devices/{deviceId} | Yes | No | No |
| GET /api/v1/customers/{customerId}/risk-profile | Yes | No | Yes |
| PUT /api/v1/customers/{customerId}/risk-profile | Yes | No | No |
| GET /api/v1/risk-rules | Yes | No | Yes |
| GET /api/v1/risk-rules/{ruleCode} | Yes | No | Yes |
| PUT /api/v1/risk-rules/{ruleCode} | Yes | No | No |

---

## 5. Authentication API

## 5.1 Login

### Endpoint

```http
POST /api/v1/auth/login
```

### Owning Service

```text
Transaction Service
```

### Description

Issues a JWT token for predefined internal users in the MVP.

### Request Headers

```http
Content-Type: application/json
X-Request-Id: req-001
```

### Request Body

```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Request Fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| username | string | yes | non-blank |
| password | string | yes | non-blank |

### Success Response

```http
200 OK
```

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "roles": [
    "ROLE_ADMIN"
  ]
}
```

### Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Missing username or password | 400 | VALIDATION_ERROR |
| Invalid credential | 401 | UNAUTHORIZED |

---

# 6. Transaction Service APIs

## 6.1 Submit Transaction

### Endpoint

```http
POST /api/v1/transactions
```

### PRD Mapping

```text
MVP-01 Transaction Submission
MVP-03 Event-Based Transaction Processing
```

### Owning Service

```text
Transaction Service
```

### Description

Creates a transaction with `PENDING_RISK_CHECK` status and publishes `transaction.created` after successful persistence.

### Request Headers

```http
Authorization: Bearer <jwt>
Idempotency-Key: unique-request-key
Content-Type: application/json
X-Request-Id: req-001
```

### Request Body

```json
{
  "sourceAccountNumber": "1234567890",
  "destinationAccountNumber": "9876543210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "deviceId": "IPHONE-15-DEVICE-001",
  "ipAddress": "36.77.88.12",
  "location": "Jakarta"
}
```

### Request Fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| sourceAccountNumber | string | yes | non-blank |
| destinationAccountNumber | string | yes | non-blank |
| amount | number | yes | greater than 0 |
| currency | string | yes | valid currency code; MVP supports IDR |
| channel | string | yes | allowed transaction channel |
| deviceId | string | no | max 100 characters |
| ipAddress | string | no | valid IPv4 or IPv6 when present |
| location | string | no | max 100 characters |

Allowed channels:

```text
MOBILE_BANKING
INTERNET_BANKING
ATM
BRANCH
BACK_OFFICE
```

### Success Response

```http
201 Created
```

```json
{
  "transactionRef": "TRX-20260604-000001",
  "status": "PENDING_RISK_CHECK",
  "message": "Transaction submitted for risk scoring"
}
```

### Idempotent Response

If the same `Idempotency-Key` is submitted again, the service must not create a new transaction.

```http
200 OK
```

```json
{
  "transactionRef": "TRX-20260604-000001",
  "status": "PENDING_RISK_CHECK",
  "message": "Existing transaction returned for the provided idempotency key"
}
```

### Side Effects

```text
1. Insert record into transactions.
2. Insert event log with PUBLISHED status after successful event publishing.
3. Publish Kafka event: transaction.created.
```

### Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Missing Authorization header | 401 | UNAUTHORIZED |
| Missing Idempotency-Key header | 400 | IDEMPOTENCY_KEY_REQUIRED |
| User does not have ADMIN or BACKOFFICE role | 403 | FORBIDDEN |
| Invalid request field | 400 | VALIDATION_ERROR |
| Invalid channel | 400 | INVALID_CHANNEL |
| Invalid IP address | 400 | INVALID_IP_ADDRESS |
| Source account not found | 404 | SOURCE_ACCOUNT_NOT_FOUND |
| Source account inactive | 409 | SOURCE_ACCOUNT_INACTIVE |
| Kafka publish failed | 500 | EVENT_PUBLISH_FAILED |

---

## 6.2 Get Transaction Detail

### Endpoint

```http
GET /api/v1/transactions/{transactionRef}
```

### PRD Mapping

```text
MVP-06 Transaction Investigation API
```

### Owning Service

```text
Transaction Service
```

### Description

Returns transaction details, risk score, risk decision, and risk factors.

### Request Headers

```http
Authorization: Bearer <jwt>
X-Request-Id: req-001
```

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---:|---|
| transactionRef | string | yes | Unique transaction reference |

### Success Response

```http
200 OK
```

```json
{
  "transactionRef": "TRX-20260604-000001",
  "sourceAccountNumber": "123****890",
  "destinationAccountNumber": "987****210",
  "amount": 25000000,
  "currency": "IDR",
  "channel": "MOBILE_BANKING",
  "deviceId": "IPHONE-15-DEVICE-001",
  "ipAddress": "36.77.88.12",
  "location": "Jakarta",
  "status": "REVIEW",
  "riskScore": 72,
  "riskDecision": "REVIEW",
  "riskFactors": [
    {
      "code": "HIGH_AMOUNT",
      "description": "Transaction amount is above configured threshold",
      "score": 25,
      "metadata": {
        "threshold": 10000000,
        "actualAmount": 25000000
      }
    },
    {
      "code": "NEW_DEVICE",
      "description": "Device is not trusted for this customer",
      "score": 20,
      "metadata": {
        "deviceId": "IPHONE-15-DEVICE-001"
      }
    }
  ],
  "createdAt": "2026-06-04T10:00:00Z",
  "updatedAt": "2026-06-04T10:00:03Z"
}
```

### Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Missing or invalid token | 401 | UNAUTHORIZED |
| User does not have allowed role | 403 | FORBIDDEN |
| Transaction does not exist | 404 | TRANSACTION_NOT_FOUND |

---

# 7. Audit Search Service APIs

## 7.1 Search Fraud Audit

### Endpoint

```http
GET /api/v1/audits/search
```

### PRD Mapping

```text
MVP-05 Elasticsearch Audit Search
MVP-06 Transaction Investigation API
```

### Owning Service

```text
Audit Search Service
```

### Description

Searches scored transaction audit documents from Elasticsearch. PostgreSQL remains the source of truth; this API is optimized for fraud investigation search.

### Request Headers

```http
Authorization: Bearer <jwt>
X-Request-Id: req-001
```

### Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---:|---:|---|
| keyword | string | no | - | Searches transactionRef, customerName, location, and riskFactors |
| transactionRef | string | no | - | Exact transaction reference |
| customerCif | string | no | - | Exact customer CIF |
| decision | string | no | - | APPROVED, REVIEW, or BLOCKED |
| minimumRiskScore | integer | no | - | Minimum risk score |
| riskFactor | string | no | - | Exact risk factor code |
| channel | string | no | - | Transaction channel |
| location | string | no | - | Location keyword |
| startDate | date | no | - | Filter createdAt from date |
| endDate | date | no | - | Filter createdAt to date |
| page | integer | no | 0 | Page number |
| size | integer | no | 20 | Page size |
| sortBy | string | no | createdAt | Sort field |
| sortDirection | string | no | DESC | Sort direction |

Allowed decisions:

```text
APPROVED
REVIEW
BLOCKED
```

### Success Response

```http
200 OK
```

```json
{
  "data": [
    {
      "transactionRef": "TRX-20260604-000001",
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "sourceAccountMasked": "123****890",
      "destinationAccountMasked": "987****210",
      "amount": 25000000,
      "currency": "IDR",
      "channel": "MOBILE_BANKING",
      "location": "Jakarta",
      "riskScore": 72,
      "decision": "REVIEW",
      "riskFactors": [
        "HIGH_AMOUNT",
        "NEW_DEVICE"
      ],
      "createdAt": "2026-06-04T10:00:00Z",
      "scoredAt": "2026-06-04T10:00:03Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

### Search Behavior

| Filter | Behavior |
|---|---|
| keyword | Full-text search across customerName, location, transactionRef, and riskFactors |
| transactionRef | Exact match |
| customerCif | Exact match |
| decision | Exact match |
| minimumRiskScore | Range query `riskScore >= minimumRiskScore` |
| riskFactor | Exact match inside riskFactors |
| channel | Exact match |
| location | Text search |
| startDate/endDate | Range filter on createdAt |

### Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Missing or invalid token | 401 | UNAUTHORIZED |
| User does not have ADMIN or FRAUD_ANALYST role | 403 | FORBIDDEN |
| Invalid date range | 400 | INVALID_DATE_RANGE |
| Page size too large | 400 | INVALID_PAGE_SIZE |
| Elasticsearch unavailable | 503 | DEPENDENCY_UNAVAILABLE |

---

# 8. Master Data Service APIs

## 8.1 Create Blacklisted Account

### Endpoint

```http
POST /api/v1/blacklisted-accounts
```

### PRD Mapping

```text
MVP-04 Redis Risk Cache and Real-Time Risk Counters
MVP-02 Risk Scoring Engine
```

### Owning Service

```text
Master Data Service
```

### Description

Creates an active blacklisted destination account record and invalidates the related Redis key.

### Request Headers

```http
Authorization: Bearer <jwt>
Content-Type: application/json
X-Request-Id: req-001
```

### Request Body

```json
{
  "accountNumber": "9876543210",
  "reason": "Reported mule account"
}
```

### Request Fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| accountNumber | string | yes | non-blank |
| reason | string | yes | non-blank, max 500 characters |

### Success Response

```http
201 Created
```

```json
{
  "id": 1,
  "accountNumber": "9876543210",
  "reason": "Reported mule account",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2026-06-04T10:00:00Z",
  "updatedAt": null
}
```

### Side Effects

```text
DELETE blacklist:account:9876543210
```

### Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Missing or invalid token | 401 | UNAUTHORIZED |
| User is not ADMIN | 403 | FORBIDDEN |
| Invalid request field | 400 | VALIDATION_ERROR |
| Account already exists in blacklist | 409 | DUPLICATE_RESOURCE |

---

## 8.2 List Blacklisted Accounts

### Endpoint

```http
GET /api/v1/blacklisted-accounts
```

### Owning Service

```text
Master Data Service
```

### Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---:|---:|---|
| active | boolean | no | - | Filter by active status |
| accountNumber | string | no | - | Filter by account number |
| page | integer | no | 0 | Page number |
| size | integer | no | 20 | Page size |

### Success Response

```http
200 OK
```

```json
{
  "data": [
    {
      "id": 1,
      "accountNumber": "9876543210",
      "reason": "Reported mule account",
      "active": true,
      "createdBy": "admin",
      "createdAt": "2026-06-04T10:00:00Z",
      "updatedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 8.3 Get Blacklisted Account Detail

### Endpoint

```http
GET /api/v1/blacklisted-accounts/{id}
```

### Path Parameters

| Parameter | Type | Required |
|---|---|---:|
| id | long | yes |

### Success Response

```http
200 OK
```

```json
{
  "id": 1,
  "accountNumber": "9876543210",
  "reason": "Reported mule account",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2026-06-04T10:00:00Z",
  "updatedAt": null
}
```

### Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Record not found | 404 | BLACKLISTED_ACCOUNT_NOT_FOUND |

---

## 8.4 Update Blacklisted Account

### Endpoint

```http
PUT /api/v1/blacklisted-accounts/{id}
```

### Request Body

```json
{
  "reason": "Confirmed suspicious beneficiary account",
  "active": true
}
```

### Success Response

```http
200 OK
```

```json
{
  "id": 1,
  "accountNumber": "9876543210",
  "reason": "Confirmed suspicious beneficiary account",
  "active": true,
  "createdBy": "admin",
  "createdAt": "2026-06-04T10:00:00Z",
  "updatedAt": "2026-06-04T11:00:00Z"
}
```

### Side Effects

```text
DELETE blacklist:account:{accountNumber}
```

---

## 8.5 Deactivate Blacklisted Account

### Endpoint

```http
DELETE /api/v1/blacklisted-accounts/{id}
```

### Description

Soft-deactivates a blacklisted account by setting `active = false`.

### Success Response

```http
204 No Content
```

### Side Effects

```text
DELETE blacklist:account:{accountNumber}
```

---

## 8.6 Register Trusted Device

### Endpoint

```http
POST /api/v1/customers/{customerId}/trusted-devices
```

### PRD Mapping

```text
MVP-04 Redis Risk Cache and Real-Time Risk Counters
MVP-02 Risk Scoring Engine
```

### Request Body

```json
{
  "deviceId": "IPHONE-15-DEVICE-001",
  "deviceName": "Daniel iPhone"
}
```

### Success Response

```http
201 Created
```

```json
{
  "customerId": 1,
  "deviceId": "IPHONE-15-DEVICE-001",
  "deviceName": "Daniel iPhone",
  "trusted": true,
  "firstSeenAt": "2026-06-04T10:00:00Z",
  "lastUsedAt": "2026-06-04T10:00:00Z"
}
```

### Side Effects

```text
DELETE customer:trusted-device:{customerId}:IPHONE-15-DEVICE-001
```

---

## 8.7 List Trusted Devices

### Endpoint

```http
GET /api/v1/customers/{customerId}/trusted-devices
```

### Success Response

```http
200 OK
```

```json
{
  "data": [
    {
      "customerId": 1,
      "deviceId": "IPHONE-15-DEVICE-001",
      "deviceName": "Daniel iPhone",
      "trusted": true,
      "firstSeenAt": "2026-06-04T10:00:00Z",
      "lastUsedAt": "2026-06-04T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 8.8 Update Trusted Device

### Endpoint

```http
PUT /api/v1/customers/{customerId}/trusted-devices/{deviceId}
```

### Request Body

```json
{
  "deviceName": "Daniel Primary iPhone",
  "trusted": true
}
```

### Success Response

```http
200 OK
```

```json
{
  "customerId": 1,
  "deviceId": "IPHONE-15-DEVICE-001",
  "deviceName": "Daniel Primary iPhone",
  "trusted": true,
  "firstSeenAt": "2026-06-04T10:00:00Z",
  "lastUsedAt": "2026-06-04T12:00:00Z"
}
```

### Side Effects

```text
DELETE customer:trusted-device:{customerId}:{deviceId}
```

---

## 8.9 Get Customer Risk Profile

### Endpoint

```http
GET /api/v1/customers/{customerId}/risk-profile
```

### Success Response

```http
200 OK
```

```json
{
  "customerId": 1,
  "riskLevel": "LOW",
  "riskReason": "Default profile",
  "lastReviewedAt": null,
  "createdAt": "2026-06-04T10:00:00Z",
  "updatedAt": null
}
```

---

## 8.10 Update Customer Risk Profile

### Endpoint

```http
PUT /api/v1/customers/{customerId}/risk-profile
```

### Request Body

```json
{
  "riskLevel": "HIGH",
  "riskReason": "Confirmed suspicious activity"
}
```

Allowed risk levels:

```text
LOW
MEDIUM
HIGH
```

### Success Response

```http
200 OK
```

```json
{
  "customerId": 1,
  "riskLevel": "HIGH",
  "riskReason": "Confirmed suspicious activity",
  "lastReviewedAt": "2026-06-04T10:00:00Z",
  "createdAt": "2026-06-01T10:00:00Z",
  "updatedAt": "2026-06-04T10:00:00Z"
}
```

### Side Effects

```text
DELETE customer:risk-profile:{customerId}
```

---

## 8.11 List Risk Rules

### Endpoint

```http
GET /api/v1/risk-rules
```

### Success Response

```http
200 OK
```

```json
{
  "data": [
    {
      "ruleCode": "HIGH_AMOUNT",
      "ruleName": "High Amount Transaction",
      "score": 25,
      "thresholdValue": 10000000,
      "active": true,
      "description": "Triggered when transaction amount exceeds configured threshold"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 6
}
```

---

## 8.12 Get Risk Rule Detail

### Endpoint

```http
GET /api/v1/risk-rules/{ruleCode}
```

### Success Response

```http
200 OK
```

```json
{
  "ruleCode": "HIGH_AMOUNT",
  "ruleName": "High Amount Transaction",
  "score": 25,
  "thresholdValue": 10000000,
  "active": true,
  "description": "Triggered when transaction amount exceeds configured threshold"
}
```

---

## 8.13 Update Risk Rule

### Endpoint

```http
PUT /api/v1/risk-rules/{ruleCode}
```

### Request Body

```json
{
  "score": 30,
  "thresholdValue": 15000000,
  "active": true,
  "description": "Triggered when transaction amount exceeds configured threshold"
}
```

### Success Response

```http
200 OK
```

```json
{
  "ruleCode": "HIGH_AMOUNT",
  "ruleName": "High Amount Transaction",
  "score": 30,
  "thresholdValue": 15000000,
  "active": true,
  "description": "Triggered when transaction amount exceeds configured threshold"
}
```

### Side Effects

```text
DELETE risk-rule:config:HIGH_AMOUNT
```

---

# 9. Reporting APIs

## 9.1 Report API Common Rules

### Owning Service

```text
Transaction Service
```

### PRD Mapping

```text
MVP-07 Native SQL Reporting
```

### Request Headers

```http
Authorization: Bearer <jwt>
X-Request-Id: req-001
```

### Access

```text
ROLE_ADMIN
ROLE_FRAUD_ANALYST
```

### Common Query Parameters

| Parameter | Type | Required | Description |
|---|---|---:|---|
| startDate | date | yes | Report start date |
| endDate | date | yes | Report end date |
| page | integer | depends | Required for list-style reports |
| size | integer | depends | Required for list-style reports |

### Common Error Responses

| Case | HTTP Status | Error Code |
|---|---:|---|
| Missing token | 401 | UNAUTHORIZED |
| Invalid role | 403 | FORBIDDEN |
| Invalid date range | 400 | INVALID_DATE_RANGE |
| Page size too large | 400 | INVALID_PAGE_SIZE |

---

## 9.2 High-Risk Transactions Report

### Endpoint

```http
GET /api/v1/reports/high-risk-transactions
```

### Query Parameters

| Parameter | Type | Required | Default |
|---|---|---:|---:|
| minimumRiskScore | integer | no | 50 |
| startDate | date | yes | - |
| endDate | date | yes | - |
| page | integer | no | 0 |
| size | integer | no | 20 |

### Success Response

```json
{
  "data": [
    {
      "transactionRef": "TRX-20260604-000001",
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "sourceAccountNumber": "123****890",
      "destinationAccountNumber": "987****210",
      "amount": 25000000,
      "riskScore": 72,
      "decision": "REVIEW",
      "status": "REVIEW",
      "createdAt": "2026-06-04T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 9.3 Transaction Velocity Report

### Endpoint

```http
GET /api/v1/reports/transaction-velocity
```

### Query Parameters

| Parameter | Type | Required | Default |
|---|---|---:|---:|
| minimumCount | integer | no | 5 |
| minimumAmount | number | no | 50000000 |
| windowMinutes | integer | no | 10 |

### Success Response

```json
{
  "data": [
    {
      "sourceAccountNumber": "123****890",
      "transactionCount": 8,
      "totalAmount": 75000000,
      "maxRiskScore": 85
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 9.4 Top Risk Customers Report

### Endpoint

```http
GET /api/v1/reports/top-risk-customers
```

### Query Parameters

| Parameter | Type | Required | Default |
|---|---|---:|---:|
| startDate | date | yes | - |
| endDate | date | yes | - |
| minimumAverageRiskScore | number | no | 50 |
| page | integer | no | 0 |
| size | integer | no | 20 |

### Success Response

```json
{
  "data": [
    {
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "totalTransactions": 12,
      "averageRiskScore": 71.5,
      "highestRiskScore": 90,
      "totalAmount": 150000000
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 9.5 Suspicious Destination Accounts Report

### Endpoint

```http
GET /api/v1/reports/suspicious-destination-accounts
```

### Query Parameters

| Parameter | Type | Required | Default |
|---|---|---:|---:|
| startDate | date | yes | - |
| endDate | date | yes | - |
| minimumUniqueSenders | integer | no | 3 |
| minimumTotalAmount | number | no | 50000000 |
| page | integer | no | 0 |
| size | integer | no | 20 |

### Success Response

```json
{
  "data": [
    {
      "destinationAccountNumber": "987****210",
      "totalReceived": 10,
      "uniqueSenders": 5,
      "totalAmount": 85000000,
      "averageRiskScore": 76.2
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 9.6 Daily Fraud Trend Report

### Endpoint

```http
GET /api/v1/reports/daily-fraud-trend
```

### Query Parameters

| Parameter | Type | Required |
|---|---|---:|
| startDate | date | yes |
| endDate | date | yes |

### Success Response

```json
{
  "data": [
    {
      "transactionDate": "2026-06-04",
      "totalTransactions": 120,
      "approvedCount": 80,
      "reviewCount": 30,
      "blockedCount": 10,
      "averageRiskScore": 42.5,
      "totalAmount": 1250000000
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

---

## 9.7 Risk Score Distribution Report

### Endpoint

```http
GET /api/v1/reports/risk-score-distribution
```

### Query Parameters

| Parameter | Type | Required |
|---|---|---:|
| startDate | date | yes |
| endDate | date | yes |

### Success Response

```json
{
  "data": [
    {
      "riskBucket": "LOW",
      "totalTransactions": 80,
      "minimumScore": 0,
      "maximumScore": 49,
      "averageScore": 21.4
    },
    {
      "riskBucket": "MEDIUM",
      "totalTransactions": 30,
      "minimumScore": 50,
      "maximumScore": 79,
      "averageScore": 63.1
    },
    {
      "riskBucket": "HIGH",
      "totalTransactions": 10,
      "minimumScore": 80,
      "maximumScore": 100,
      "averageScore": 86.5
    }
  ],
  "page": 0,
  "size": 20,
  "total": 3
}
```

---

## 9.8 Daily Top Risky Customers Report

### Endpoint

```http
GET /api/v1/reports/daily-top-risky-customers
```

### Query Parameters

| Parameter | Type | Required | Default |
|---|---|---:|---:|
| startDate | date | yes | - |
| endDate | date | yes | - |
| topN | integer | no | 10 |

### Success Response

```json
{
  "data": [
    {
      "transactionDate": "2026-06-04",
      "riskRank": 1,
      "customerCif": "CIF001",
      "customerName": "Daniel Sinaga",
      "totalTransactions": 12,
      "totalAmount": 150000000,
      "averageRiskScore": 71.5,
      "highestRiskScore": 90
    }
  ],
  "page": 0,
  "size": 10,
  "total": 1
}
```

---

# 10. Health Check APIs

## 10.1 Application Health

### Endpoint

```http
GET /actuator/health
```

### Owning Service

```text
All services
```

### Success Response

```json
{
  "status": "UP"
}
```

### Health Dependencies

| Service | Dependencies |
|---|---|
| Transaction Service | PostgreSQL, Kafka |
| Risk Engine Service | PostgreSQL, Kafka, Redis |
| Audit Search Service | Kafka, Elasticsearch |
| Master Data Service | PostgreSQL, Redis |

---

# 11. API Traceability Matrix

| API | PRD MVP | Owning Service |
|---|---|---|
| POST /api/v1/auth/login | Security / MVP support | Transaction Service |
| POST /api/v1/transactions | MVP-01, MVP-03 | Transaction Service |
| GET /api/v1/transactions/{transactionRef} | MVP-06 | Transaction Service |
| GET /api/v1/audits/search | MVP-05, MVP-06 | Audit Search Service |
| POST /api/v1/blacklisted-accounts | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/blacklisted-accounts | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/blacklisted-accounts/{id} | MVP-02, MVP-04 | Master Data Service |
| PUT /api/v1/blacklisted-accounts/{id} | MVP-02, MVP-04 | Master Data Service |
| DELETE /api/v1/blacklisted-accounts/{id} | MVP-02, MVP-04 | Master Data Service |
| POST /api/v1/customers/{customerId}/trusted-devices | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/customers/{customerId}/trusted-devices | MVP-02, MVP-04 | Master Data Service |
| PUT /api/v1/customers/{customerId}/trusted-devices/{deviceId} | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/customers/{customerId}/risk-profile | MVP-02, MVP-04 | Master Data Service |
| PUT /api/v1/customers/{customerId}/risk-profile | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/risk-rules | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/risk-rules/{ruleCode} | MVP-02, MVP-04 | Master Data Service |
| PUT /api/v1/risk-rules/{ruleCode} | MVP-02, MVP-04 | Master Data Service |
| GET /api/v1/reports/high-risk-transactions | MVP-07 | Transaction Service |
| GET /api/v1/reports/transaction-velocity | MVP-07 | Transaction Service |
| GET /api/v1/reports/top-risk-customers | MVP-07 | Transaction Service |
| GET /api/v1/reports/suspicious-destination-accounts | MVP-07 | Transaction Service |
| GET /api/v1/reports/daily-fraud-trend | MVP-07 | Transaction Service |
| GET /api/v1/reports/risk-score-distribution | MVP-07 | Transaction Service |
| GET /api/v1/reports/daily-top-risky-customers | MVP-07 | Transaction Service |

---

# 12. API Acceptance Criteria

The API layer is considered complete when:

1. All endpoints in this contract are implemented.
2. All protected endpoints require JWT authentication.
3. Role-based authorization follows the endpoint access matrix.
4. Transaction submission requires `Idempotency-Key`.
5. Duplicate transaction submission returns the existing transaction response.
6. All validation failures return the common error response format.
7. Account numbers are masked in all external responses.
8. Transaction detail returns risk score, decision, and risk factors after scoring completes.
9. Audit search supports keyword search and structured filters.
10. Master data updates invalidate related Redis keys.
11. Reporting endpoints return paginated or structured responses according to this contract.
12. Date range validation is applied to search and report APIs.
13. API documentation is generated through OpenAPI/Swagger.
14. Postman collection or equivalent API collection is provided for local verification.
