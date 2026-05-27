param(
    [string]$BaseUrl = "http://localhost:18080"
)

$ErrorActionPreference = "Stop"

$results = New-Object System.Collections.Generic.List[object]
$runId = Get-Date -Format "yyyyMMddHHmmss"
$readerName = "reader-$runId"
$adminName = "admin-$runId"
$bookIsbn = "978-E2E-$runId"
$readerToken = $null
$adminToken = $null
$bookId = $null
$borrowId = $null

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )
    $results.Add([pscustomobject]@{
        Test = $Name
        Status = if ($Passed) { "PASS" } else { "FAIL" }
        Detail = $Detail
    })
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
    }
    return Invoke-RestMethod @params
}

function Invoke-Status {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    try {
        $params = @{
            Method = $Method
            Uri = $Url
            Headers = $Headers
            SkipHttpErrorCheck = $true
        }
        if ($null -ne $Body) {
            $params["ContentType"] = "application/json"
            $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
        }
        return Invoke-WebRequest @params
    } catch {
        throw
    }
}

Write-Host ""
Write-Host "Library Management System E2E Test" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host "Run ID:   $runId"
Write-Host ""

try {
    $books = Invoke-Json GET "$BaseUrl/api/books"
    Add-Result "Public book list" ($null -ne $books) "GET /api/books returned successfully"
} catch {
    Add-Result "Public book list" $false $_.Exception.Message
}

try {
    $instance = Invoke-Json GET "$BaseUrl/api/books/instance"
    Add-Result "Book instance info" ($instance.service -eq "BOOK-SERVICE") "service=$($instance.service), port=$($instance.port)"
} catch {
    Add-Result "Book instance info" $false $_.Exception.Message
}

try {
    $reader = Invoke-Json POST "$BaseUrl/api/users/register" @{
        username = $readerName
        password = "123456"
        email = "$readerName@test.com"
    }
    Add-Result "Register reader" ($reader.role -eq "READER") "userId=$($reader.id), role=$($reader.role)"
} catch {
    Add-Result "Register reader" $false $_.Exception.Message
}

try {
    $admin = Invoke-Json POST "$BaseUrl/api/users/register" @{
        username = $adminName
        password = "123456"
        email = "$adminName@test.com"
        role = "ADMIN"
    }
    Add-Result "Register admin" ($admin.role -eq "ADMIN") "userId=$($admin.id), role=$($admin.role)"
} catch {
    Add-Result "Register admin" $false $_.Exception.Message
}

try {
    $readerLogin = Invoke-Json POST "$BaseUrl/api/auth/login" @{
        username = $readerName
        password = "123456"
    }
    $readerToken = $readerLogin.token
    Add-Result "Reader login" (-not [string]::IsNullOrWhiteSpace($readerToken)) "token received"
} catch {
    Add-Result "Reader login" $false $_.Exception.Message
}

try {
    $adminLogin = Invoke-Json POST "$BaseUrl/api/auth/login" @{
        username = $adminName
        password = "123456"
    }
    $adminToken = $adminLogin.token
    Add-Result "Admin login" (-not [string]::IsNullOrWhiteSpace($adminToken)) "token received"
} catch {
    Add-Result "Admin login" $false $_.Exception.Message
}

try {
    $resp = Invoke-Status GET "$BaseUrl/api/borrows"
    Add-Result "Borrow list without token" ($resp.StatusCode -eq 401) "status=$($resp.StatusCode)"
} catch {
    Add-Result "Borrow list without token" $false $_.Exception.Message
}

if ($readerToken) {
    try {
        $resp = Invoke-Json GET "$BaseUrl/api/borrows" $null @{ Authorization = "Bearer $readerToken" }
        Add-Result "Borrow list with reader token" ($null -ne $resp) "authenticated request succeeded"
    } catch {
        Add-Result "Borrow list with reader token" $false $_.Exception.Message
    }
}

if ($readerToken) {
    try {
        $resp = Invoke-Status POST "$BaseUrl/api/books" @{
            title = "Reader Cannot Create"
            author = "E2E"
            isbn = "978-READER-$runId"
            publisher = "E2E"
            category = "Computer"
            stock = 1
            price = 1
        } @{ Authorization = "Bearer $readerToken" }
        Add-Result "Reader create book forbidden" ($resp.StatusCode -eq 403) "status=$($resp.StatusCode)"
    } catch {
        Add-Result "Reader create book forbidden" $false $_.Exception.Message
    }
}

if ($adminToken) {
    try {
        $book = Invoke-Json POST "$BaseUrl/api/books" @{
            title = "E2E Book"
            author = "E2E"
            isbn = $bookIsbn
            publisher = "E2E"
            category = "Computer"
            stock = 2
            price = 66
        } @{ Authorization = "Bearer $adminToken" }
        $bookId = $book.id
        Add-Result "Admin create book" ($bookId -gt 0) "bookId=$bookId"
    } catch {
        Add-Result "Admin create book" $false $_.Exception.Message
    }
}

if ($readerToken -and $bookId) {
    try {
        $borrow = Invoke-Json POST "$BaseUrl/api/borrows" @{
            bookId = $bookId
        } @{ Authorization = "Bearer $readerToken" }
        $borrowId = $borrow.id
        Add-Result "Borrow book" ($borrow.status -eq "BORROWED") "borrowId=$borrowId, status=$($borrow.status)"
    } catch {
        Add-Result "Borrow book" $false $_.Exception.Message
    }
}

if ($readerToken -and $bookId) {
    try {
        $resp = Invoke-Status POST "$BaseUrl/api/borrows" @{
            bookId = $bookId
        } @{ Authorization = "Bearer $readerToken" }
        Add-Result "Duplicate borrow rejected" ($resp.StatusCode -eq 409) "status=$($resp.StatusCode)"
    } catch {
        Add-Result "Duplicate borrow rejected" $false $_.Exception.Message
    }
}

if ($readerToken -and $borrowId) {
    try {
        $returned = Invoke-Json PUT "$BaseUrl/api/borrows/$borrowId/return" $null @{ Authorization = "Bearer $readerToken" }
        Add-Result "Return book" ($returned.status -like "RETURNED*") "status=$($returned.status)"
    } catch {
        Add-Result "Return book" $false $_.Exception.Message
    }
}

if ($adminToken -and $bookId) {
    try {
        $resp = Invoke-Status DELETE "$BaseUrl/api/books/$bookId" $null @{ Authorization = "Bearer $adminToken" }
        Add-Result "Admin delete book" ($resp.StatusCode -eq 204) "status=$($resp.StatusCode)"
    } catch {
        Add-Result "Admin delete book" $false $_.Exception.Message
    }
}

Write-Host ""
$results | Format-Table -AutoSize

$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Host ""
Write-Host "Summary: $passed passed, $failed failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -gt 0) {
    exit 1
}
