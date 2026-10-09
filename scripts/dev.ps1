#requires -Version 5.1
<#
.SYNOPSIS
    Manage the local GDRN foundation. PowerShell 5.1 or PowerShell 7.
.EXAMPLE
    .\scripts\dev.ps1 up
.EXAMPLE
    .\scripts\dev.ps1 smoke -BaseUrl http://localhost:8081
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('init', 'up', 'status', 'smoke', 'down', 'verify')]
    [string] $Action = 'status',

    [string] $BaseUrl,

    [ValidateRange(30, 600)]
    [int] $WaitTimeout = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot

function Invoke-Docker {
    param([string[]] $DockerArguments)
    & docker @DockerArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed (exit $LASTEXITCODE). See its diagnostic above."
    }
}

function Assert-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker is missing. Install/start Docker Desktop with Linux containers.'
    }
    $engine = Invoke-Docker -DockerArguments @('info', '--format', '{{.OSType}}')
    if ($engine -ne 'linux') {
        throw 'Select Linux containers in Docker Desktop before continuing.'
    }
    Invoke-Docker -DockerArguments @('compose', 'version', '--short') | Out-Null
}

function Initialize-Environment {
    $environmentFile = Join-Path $repositoryRoot '.env'
    if (Test-Path -LiteralPath $environmentFile) {
        Write-Host 'Existing .env preserved. Credentials and database data are unchanged.'
        return
    }

    # Hex avoids Compose interpolation/quoting and is safe in JDBC credentials.
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $bytes = New-Object byte[] 32
        $random.GetBytes($bytes)
        $password = [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
        $random.GetBytes($bytes)
        $jwtKey = [Convert]::ToBase64String($bytes)
    } finally {
        $random.Dispose()
    }
    $template = [IO.File]::ReadAllText((Join-Path $repositoryRoot '.env.example'))
    $contents = $template.Replace('replace-with-a-local-password', $password)
    $contents = $contents.Replace('replace-with-random-base64-key', $jwtKey)
    # CreateNew also prevents overwriting a file created concurrently.
    $stream = [IO.File]::Open($environmentFile, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
    try {
        $encoded = [Text.UTF8Encoding]::new($false).GetBytes($contents)
        $stream.Write($encoded, 0, $encoded.Length)
    } finally {
        $stream.Dispose()
    }
    Write-Host 'Created ignored .env with a random local password and JWT key. No secret is printed.'
    Write-Host 'For an existing database volume, configure its original credentials before up.'
}

function Assert-Environment {
    if (-not (Test-Path -LiteralPath (Join-Path $repositoryRoot '.env'))) {
        throw 'Missing .env. Run scripts/dev.ps1 init and configure it first.'
    }
    # --quiet avoids printing resolved passwords from the Compose model.
    Invoke-Docker -DockerArguments @('compose', 'config', '--quiet')
}

function Get-ApplicationUrl {
    if ($BaseUrl) {
        $uri = $null
        if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref] $uri) -or
            $uri.Scheme -notin @('http', 'https') -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
            throw 'BaseUrl must be an absolute HTTP(S) URL without credentials, query or fragment.'
        }
        return $BaseUrl.TrimEnd('/')
    }
    Assert-Docker
    Assert-Environment
    $binding = Invoke-Docker -DockerArguments @('compose', 'port', 'backend', '8080')
    if ($binding -notmatch '^127\.0\.0\.1:(\d+)$') {
        throw 'Cannot resolve the backend port. Start Compose or pass -BaseUrl for a host JVM.'
    }
    return "http://127.0.0.1:$($Matches[1])"
}

function Test-Foundation {
    $url = Get-ApplicationUrl
    foreach ($path in @('/actuator/health', '/actuator/health/liveness', '/actuator/health/readiness')) {
        # PS 5.1 treats Actuator's vendor JSON content type as bytes unless JSON is requested.
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$url$path" -TimeoutSec 10 `
            -Headers @{ Accept = 'application/json' }
        $health = $response.Content | ConvertFrom-Json
        if ($response.StatusCode -ne 200 -or $health.status -ne 'UP') {
            throw "Foundation check failed: $path is not UP."
        }
        if ($health.PSObject.Properties.Name -contains 'components' -or
            $health.PSObject.Properties.Name -contains 'details') {
            throw "Foundation check failed: $path exposes health details."
        }
        Write-Host "PASS $path (UP)"
    }
    $api = Invoke-WebRequest -UseBasicParsing -Uri "$url/v3/api-docs" -TimeoutSec 10 `
        -Headers @{ Accept = 'application/json' }
    $document = $api.Content | ConvertFrom-Json
    if ($api.StatusCode -ne 200 -or -not $document.openapi -or
        $document.info.title -ne 'Global Disaster Response Network API') {
        throw 'Foundation check failed: GDRN OpenAPI document is unavailable.'
    }
    Write-Host 'PASS /v3/api-docs (GDRN OpenAPI)'
    $swagger = Invoke-WebRequest -UseBasicParsing -Uri "$url/swagger-ui/index.html" -TimeoutSec 10
    if ($swagger.StatusCode -ne 200 -or $swagger.Content -notmatch 'Swagger UI') {
        throw 'Foundation check failed: Swagger UI is unavailable.'
    }
    Write-Host 'PASS /swagger-ui/index.html'
    Write-Host "Swagger: $url/swagger-ui/index.html"
}

Push-Location -LiteralPath $repositoryRoot
try {
    switch ($Action) {
        'init' { Initialize-Environment }
        'up' {
            Assert-Docker
            Initialize-Environment
            Assert-Environment
            Invoke-Docker -DockerArguments @('compose', 'up', '--build', '-d', '--wait',
                '--wait-timeout', "$WaitTimeout")
            Test-Foundation
        }
        'status' {
            Assert-Docker
            Assert-Environment
            Invoke-Docker -DockerArguments @('compose', 'ps')
        }
        'smoke' { Test-Foundation }
        'down' {
            Assert-Docker
            Assert-Environment
            Invoke-Docker -DockerArguments @('compose', 'down')
            Write-Host 'Containers stopped; database volume retained.'
        }
        'verify' {
            Assert-Docker
            if ($env:OS -eq 'Windows_NT') {
                & (Join-Path $repositoryRoot 'mvnw.cmd') clean verify
            } else {
                & bash (Join-Path $repositoryRoot 'mvnw') clean verify
            }
            if ($LASTEXITCODE -ne 0) { throw "Maven verification failed (exit $LASTEXITCODE)." }
        }
    }
} catch {
    Write-Error -Message $_.Exception.Message -ErrorAction Continue
    exit 1
} finally {
    Pop-Location
}
