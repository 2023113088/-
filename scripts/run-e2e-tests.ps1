$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root "target\e2e-logs"
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
$mvn = "mvn"

$ports = @{
    eureka = 18761
    user = 19001
    book1 = 19002
    book2 = 19012
    borrow = 19003
    gateway = 18080
}

$processes = @()
$results = @()

function Add-Result([string]$name, [bool]$passed, [string]$detail) {
    $script:results += [pscustomobject]@{
        Test = $name
        Result = if ($passed) { "PASS" } else { "FAIL" }
        Detail = $detail
    }
}

function Start-App([string]$name, [string]$jar, [string]$arguments) {
    $out = Join-Path $logs "$name.out.log"
    $err = Join-Path $logs "$name.err.log"
    $proc = Start-Process -FilePath $java `
        -ArgumentList "-jar `"$jar`" $arguments" `
        -RedirectStandardOutput $out `
        -RedirectStandardError $err `
        -WindowStyle Hidden `
        -PassThru
    $script:processes += $proc
    return $proc
}

function Wait-Url([string]$name, [string]$url, [int]$seconds = 90) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 3 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Timed out waiting for $name at $url"
}

function Wait-Request([string]$name, [scriptblock]$request, [int]$seconds = 90) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            & $request | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Timed out waiting for $name"
}

function Invoke-Json([string]$method, [string]$url, $body = $null, [hashtable]$headers = @{}) {
    $params = @{
        Method = $method
        Uri = $url
        Headers = $headers
        UseBasicParsing = $true
    }
    if ($null -ne $body) {
        $params.ContentType = "application/json"
        $params.Body = ($body | ConvertTo-Json -Depth 8)
    }
    return Invoke-RestMethod @params
}

function Get-StatusCode([scriptblock]$request) {
    try {
        & $request | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

try {
    New-Item -ItemType Directory -Force -Path $logs | Out-Null

    Write-Host "Building project..."
    Push-Location $root
    & $mvn -q -DskipTests package
    Pop-Location

    $eurekaJar = Join-Path $root "eureka-server\target\eureka-server-1.0.0.jar"
    $userJar = Join-Path $root "user-service\target\user-service-1.0.0.jar"
    $bookJar = Join-Path $root "book-service\target\book-service-1.0.0.jar"
    $borrowJar = Join-Path $root "borrow-service\target\borrow-service-1.0.0.jar"
    $gatewayJar = Join-Path $root "gateway-service\target\gateway-service-1.0.0.jar"
    $eurekaUrl = "http://localhost:$($ports.eureka)/eureka/"

    Write-Host "Starting temporary services..."
    Start-App "eureka" $eurekaJar "--server.port=$($ports.eureka)"
    Wait-Url "eureka" "http://localhost:$($ports.eureka)"

    $eurekaClientArgs = "--eureka.client.service-url.defaultZone=$eurekaUrl --eureka.client.registry-fetch-interval-seconds=5"
    Start-App "user" $userJar "--server.port=$($ports.user) $eurekaClientArgs" | Out-Null
    $book1Proc = Start-App "book-1" $bookJar "--server.port=$($ports.book1) $eurekaClientArgs"
    $book2Proc = Start-App "book-2" $bookJar "--server.port=$($ports.book2) $eurekaClientArgs"
    Start-App "borrow" $borrowJar "--server.port=$($ports.borrow) $eurekaClientArgs" | Out-Null
    Start-App "gateway" $gatewayJar "--server.port=$($ports.gateway) $eurekaClientArgs" | Out-Null

    Wait-Url "user-service" "http://localhost:$($ports.user)/actuator/health"
    Wait-Url "book-service-1" "http://localhost:$($ports.book1)/actuator/health"
    Wait-Url "book-service-2" "http://localhost:$($ports.book2)/actuator/health"
    Wait-Url "borrow-service" "http://localhost:$($ports.borrow)/actuator/health"
    Wait-Url "gateway-service" "http://localhost:$($ports.gateway)/actuator/health"
    $base = "http://localhost:$($ports.gateway)"
    Wait-Url "gateway route to book-service" "$base/api/books"

    $books = Invoke-Json "GET" "$base/api/books"
    Add-Result "List books through gateway" ($books.Count -ge 2) "books=$($books.Count)"

    $rateLimitStatuses = @()
    1..21 | ForEach-Object {
        $rateLimitStatuses += Get-StatusCode { Invoke-Json "GET" "$base/api/books" }
    }
    $rateLimited = @($rateLimitStatuses | Where-Object { $_ -eq 429 }).Count -ge 1
    Add-Result "Gateway rate limits burst traffic" $rateLimited ("statuses=" + ($rateLimitStatuses -join ","))

    $portsSeen = @()
    1..8 | ForEach-Object {
        $instance = Invoke-Json "GET" "$base/api/books/instance"
        $portsSeen += $instance.port
    }
    $uniquePorts = @($portsSeen | Sort-Object -Unique)
    Add-Result "Load balance across BOOK-SERVICE instances" ($uniquePorts.Count -ge 2) ("ports=" + ($uniquePorts -join ","))

    $reader = Invoke-Json "POST" "$base/api/users/register" @{
        username = "reader1"
        password = "123456"
        email = "reader1@test.com"
    }
    Add-Result "Register reader" ($reader.role -eq "READER") "role=$($reader.role)"

    $admin = Invoke-Json "POST" "$base/api/users/register" @{
        username = "admin1"
        password = "123456"
        email = "admin1@test.com"
        role = "ADMIN"
    }
    Add-Result "Register admin" ($admin.role -eq "ADMIN") "role=$($admin.role)"

    $readerLogin = Invoke-Json "POST" "$base/api/auth/login" @{
        username = "reader1"
        password = "123456"
    }
    Add-Result "Reader login" (![string]::IsNullOrWhiteSpace($readerLogin.token)) "token returned"

    $adminLogin = Invoke-Json "POST" "$base/api/auth/login" @{
        username = "admin1"
        password = "123456"
    }
    Add-Result "Admin login" (![string]::IsNullOrWhiteSpace($adminLogin.token)) "token returned"

    $unauthStatus = Get-StatusCode { Invoke-Json "GET" "$base/api/borrows" }
    Add-Result "Gateway rejects unauthenticated borrow request" ($unauthStatus -eq 401) "status=$unauthStatus"

    $readerHeaders = @{ Authorization = "Bearer $($readerLogin.token)" }
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.token)" }
    Wait-Request "borrow-service route through gateway" {
        Invoke-Json "GET" "$base/api/borrows" $null $readerHeaders
    }

    $forbiddenStatus = Get-StatusCode {
        Invoke-Json "POST" "$base/api/books" @{
            title = "Reader Book"
            author = "Tester"
            isbn = "978-READER-0001"
            publisher = "Test Press"
            category = "Computer"
            stock = 1
            price = 10
        } $readerHeaders
    }
    Add-Result "Reader cannot manage books" ($forbiddenStatus -eq 403) "status=$forbiddenStatus"

    $createdBook = Invoke-Json "POST" "$base/api/books" @{
        title = "Admin Book"
        author = "Tester"
        isbn = "978-ADMIN-0001"
        publisher = "Test Press"
        category = "Computer"
        stock = 2
        price = 66
    } $adminHeaders
    Add-Result "Admin creates book" ($createdBook.id -gt 0) "bookId=$($createdBook.id)"

    $borrow = Invoke-Json "POST" "$base/api/borrows" @{ bookId = 1 } $readerHeaders
    Add-Result "Borrow book" ($borrow.status -eq "BORROWED") "borrowId=$($borrow.id)"

    $duplicateStatus = Get-StatusCode {
        Invoke-Json "POST" "$base/api/borrows" @{ bookId = 1 } $readerHeaders
    }
    Add-Result "Reject duplicate borrow" ($duplicateStatus -eq 409) "status=$duplicateStatus"

    $returned = Invoke-Json "PUT" "$base/api/borrows/$($borrow.id)/return" $null $readerHeaders
    Add-Result "Return book" ($returned.status -like "RETURNED*") "status=$($returned.status)"

    Write-Host "Stopping BOOK-SERVICE instances to test fallback..."
    foreach ($proc in @($book1Proc, $book2Proc)) {
        if (!$proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    }
    Start-Sleep -Seconds 5

    $fallbackStatus = Get-StatusCode {
        Invoke-Json "POST" "$base/api/borrows" @{ bookId = 1 } $readerHeaders
    }
    Add-Result "Feign fallback when book service unavailable" ($fallbackStatus -eq 503) "status=$fallbackStatus"
}
catch {
    Add-Result "Unhandled test runner error" $false $_.Exception.Message
}
finally {
    Write-Host ""
    Write-Host "E2E test results"
    Write-Host "----------------"
    $results | Format-Table -AutoSize

    foreach ($proc in $processes) {
        if ($proc -and !$proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    }

    $failed = @($results | Where-Object { $_.Result -eq "FAIL" }).Count
    if ($failed -gt 0) {
        exit 1
    }
}
