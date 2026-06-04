param(
    [string]$TransactionBaseUrl = "http://localhost:8081",
    [string]$AuditBaseUrl = "http://localhost:8083",
    [string]$MasterDataBaseUrl = "http://localhost:8084",
    [string]$RiskEngineBaseUrl = "http://localhost:8082"
)

$ErrorActionPreference = "Stop"
$RunId = Get-Date -Format "yyyyMMddHHmmss"

function Invoke-BankGuardJson {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $request = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $request.ContentType = "application/json"
        $request.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    Invoke-RestMethod @request
}

function Wait-Health {
    param(
        [string]$Name,
        [string]$Url
    )

    for ($i = 1; $i -le 30; $i++) {
        try {
            $response = Invoke-BankGuardJson -Method GET -Url "$Url/actuator/health"
            if ($response.status -eq "UP") {
                Write-Host "UP $Name"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Health check failed for $Name"
}

function Login {
    param(
        [string]$Username,
        [string]$Password,
        [string]$ExpectedRole
    )

    $response = Invoke-BankGuardJson -Method POST -Url "$TransactionBaseUrl/api/v1/auth/login" -Headers @{
        "X-Request-Id" = "demo-login-$Username-$RunId"
    } -Body @{
        username = $Username
        password = $Password
    }
    if ($response.roles -notcontains $ExpectedRole) {
        throw "Login role mismatch for $Username"
    }
    Write-Host "LOGIN $Username $ExpectedRole"
    $response.accessToken
}

function Submit-Transaction {
    param(
        [string]$Token,
        [string]$IdempotencyKey,
        [string]$SourceAccount,
        [string]$DestinationAccount,
        [decimal]$Amount,
        [string]$DeviceId,
        [string]$Location
    )

    $response = Invoke-BankGuardJson -Method POST -Url "$TransactionBaseUrl/api/v1/transactions" -Headers @{
        Authorization = "Bearer $Token"
        "Idempotency-Key" = $IdempotencyKey
        "X-Request-Id" = $IdempotencyKey
    } -Body @{
        sourceAccountNumber = $SourceAccount
        destinationAccountNumber = $DestinationAccount
        amount = $Amount
        currency = "IDR"
        channel = "MOBILE_BANKING"
        deviceId = $DeviceId
        ipAddress = "36.77.88.12"
        location = $Location
    }
    Write-Host "SUBMIT $IdempotencyKey $($response.transactionRef)"
    $response.transactionRef
}

function Wait-TransactionDecision {
    param(
        [string]$Token,
        [string]$TransactionRef,
        [string]$ExpectedDecision,
        [string]$ExpectedFactor = ""
    )

    for ($i = 1; $i -le 30; $i++) {
        $detail = Invoke-BankGuardJson -Method GET -Url "$TransactionBaseUrl/api/v1/transactions/$TransactionRef" -Headers @{
            Authorization = "Bearer $Token"
            "X-Request-Id" = "demo-detail-$RunId"
        }
        if ($detail.riskDecision -eq $ExpectedDecision) {
            if ($ExpectedFactor -and (($detail.riskFactors | ForEach-Object { $_.code }) -notcontains $ExpectedFactor)) {
                throw "Transaction $TransactionRef reached $ExpectedDecision without expected factor $ExpectedFactor"
            }
            Write-Host "DECISION $TransactionRef $ExpectedDecision score=$($detail.riskScore)"
            return $detail
        }
        Start-Sleep -Seconds 2
    }
    throw "Transaction $TransactionRef did not reach $ExpectedDecision"
}

function Wait-AuditDecision {
    param(
        [string]$Token,
        [string]$Decision,
        [string]$TransactionRef
    )

    $encodedRef = [uri]::EscapeDataString($TransactionRef)
    for ($i = 1; $i -le 30; $i++) {
        $result = Invoke-BankGuardJson -Method GET -Url "$AuditBaseUrl/api/v1/audits/search?transactionRef=$encodedRef&decision=$Decision&page=0&size=20" -Headers @{
            Authorization = "Bearer $Token"
            "X-Request-Id" = "demo-audit-$RunId"
        }
        if ($result.total -ge 1) {
            Write-Host "AUDIT $TransactionRef $Decision"
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Audit document not searchable for $TransactionRef"
}

function Invoke-Report {
    param(
        [string]$Token,
        [string]$Path
    )

    $response = Invoke-BankGuardJson -Method GET -Url "$TransactionBaseUrl$Path" -Headers @{
        Authorization = "Bearer $Token"
        "X-Request-Id" = "demo-report-$RunId"
    }
    Write-Host "REPORT $Path total=$($response.total)"
}

Wait-Health -Name "transaction-service" -Url $TransactionBaseUrl
Wait-Health -Name "risk-engine-service" -Url $RiskEngineBaseUrl
Wait-Health -Name "audit-search-service" -Url $AuditBaseUrl
Wait-Health -Name "master-data-service" -Url $MasterDataBaseUrl

$AdminToken = Login -Username "admin" -Password "admin123" -ExpectedRole "ROLE_ADMIN"
$BackofficeToken = Login -Username "backoffice" -Password "backoffice123" -ExpectedRole "ROLE_BACKOFFICE"
$AnalystToken = Login -Username "analyst" -Password "analyst123" -ExpectedRole "ROLE_FRAUD_ANALYST"

$LowRiskRef = Submit-Transaction `
    -Token $BackofficeToken `
    -IdempotencyKey "demo-low-risk-$RunId" `
    -SourceAccount "1234567890" `
    -DestinationAccount "5550001110" `
    -Amount 1000000 `
    -DeviceId "DEVICE-TRUSTED-001" `
    -Location "Jakarta"
Wait-TransactionDecision -Token $BackofficeToken -TransactionRef $LowRiskRef -ExpectedDecision "APPROVED" | Out-Null
Wait-AuditDecision -Token $AnalystToken -Decision "APPROVED" -TransactionRef $LowRiskRef

$ReviewRef = Submit-Transaction `
    -Token $BackofficeToken `
    -IdempotencyKey "demo-review-$RunId" `
    -SourceAccount "1234567890" `
    -DestinationAccount "5550002220" `
    -Amount 25000000 `
    -DeviceId "DEVICE-UNTRUSTED-001" `
    -Location "Surabaya"
Wait-TransactionDecision -Token $BackofficeToken -TransactionRef $ReviewRef -ExpectedDecision "REVIEW" -ExpectedFactor "HIGH_AMOUNT" | Out-Null
Wait-AuditDecision -Token $AnalystToken -Decision "REVIEW" -TransactionRef $ReviewRef

$BlockedRef = Submit-Transaction `
    -Token $BackofficeToken `
    -IdempotencyKey "demo-blocked-$RunId" `
    -SourceAccount "2234567890" `
    -DestinationAccount "9876543210" `
    -Amount 25000000 `
    -DeviceId "DEVICE-BLOCKED-$RunId" `
    -Location "Medan"
Wait-TransactionDecision -Token $BackofficeToken -TransactionRef $BlockedRef -ExpectedDecision "BLOCKED" -ExpectedFactor "BLACKLISTED_DESTINATION" | Out-Null
Wait-AuditDecision -Token $AnalystToken -Decision "BLOCKED" -TransactionRef $BlockedRef

$LastVelocityRef = $null
for ($i = 1; $i -le 5; $i++) {
    $LastVelocityRef = Submit-Transaction `
        -Token $BackofficeToken `
        -IdempotencyKey "demo-velocity-$RunId-$i" `
        -SourceAccount "1234567890" `
        -DestinationAccount "55500033$i" `
        -Amount 1000000 `
        -DeviceId "DEVICE-TRUSTED-001" `
        -Location "Jakarta"
}
Wait-TransactionDecision -Token $BackofficeToken -TransactionRef $LastVelocityRef -ExpectedDecision "APPROVED" -ExpectedFactor "HIGH_FREQUENCY_TRANSACTION_COUNT" | Out-Null

docker exec bankguard-redis redis-cli GET risk:velocity:count:1234567890:10m
docker exec bankguard-redis redis-cli GET risk:velocity:amount:1234567890:10m

Invoke-BankGuardJson -Method POST -Url "$MasterDataBaseUrl/api/v1/blacklisted-accounts" -Headers @{
    Authorization = "Bearer $AdminToken"
    "X-Request-Id" = "demo-blacklist-$RunId"
} -Body @{
    accountNumber = "777888$($RunId.Substring($RunId.Length - 4))"
    reason = "Demo smoke blacklist"
} | Out-Null
Write-Host "MASTER_DATA blacklist create/invalidation path OK"

$StartDate = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
$EndDate = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/high-risk-transactions?minimumRiskScore=50&startDate=$StartDate&endDate=$EndDate&page=0&size=20"
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/transaction-velocity?minimumCount=5&page=0&size=20"
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/top-risk-customers?startDate=$StartDate&endDate=$EndDate&page=0&size=20"
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/suspicious-destination-accounts?startDate=$StartDate&endDate=$EndDate&page=0&size=20"
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/daily-fraud-trend?startDate=$StartDate&endDate=$EndDate"
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/risk-score-distribution?startDate=$StartDate&endDate=$EndDate"
Invoke-Report -Token $AnalystToken -Path "/api/v1/reports/daily-top-risky-customers?startDate=$StartDate&endDate=$EndDate&topN=10"

Write-Host "BANKGUARD DEMO SMOKE PASSED"
