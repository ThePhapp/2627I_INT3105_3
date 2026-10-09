param([ValidateSet('setup', 'start', 'stop', 'status')][string]$Action = 'start')
$ErrorActionPreference = 'Stop'
$repoPath = Split-Path $PSScriptRoot -Parent
$envPath = Join-Path $repoPath '.env'
$runtimePath = Join-Path $repoPath '.tools/local'
$frontendPath = Join-Path $repoPath 'frontend'

function Read-LocalEnv {
    if (-not (Test-Path -LiteralPath $envPath)) { throw 'Run scripts/local.ps1 -Action setup first.' }
    $values = @{}
    Get-Content -LiteralPath $envPath | ForEach-Object {
        if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
    return $values
}
function Random-Value([int]$count) {
    $bytes = New-Object byte[] $count
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
function Require-Success([string]$step) {
    if ($LASTEXITCODE -ne 0) { throw "$step failed (exit $LASTEXITCODE)." }
}
function Wait-LocalUrl([string]$url) {
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try { $null = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 2; return }
        catch { Start-Sleep -Milliseconds 500 }
    }
    throw "Service did not become ready at $url. Check .tools/local logs."
}
function Managed-Frontend {
    $statePath = Join-Path $runtimePath 'frontend.json'
    if (-not (Test-Path -LiteralPath $statePath)) { return $null }
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($state.pid)" -ErrorAction SilentlyContinue
    if ($process -and $process.Name -eq 'node.exe' -and $process.CommandLine.Contains($frontendPath) -and
            $process.CreationDate.ToUniversalTime().ToString('o') -eq $state.createdAt) { return $process }
    return $null
}

Push-Location $repoPath
try {
    if ($Action -eq 'setup') {
        if (-not (Test-Path -LiteralPath $envPath)) { Copy-Item -LiteralPath '.env.example' -Destination $envPath }
        $values = Read-LocalEnv
        $text = [IO.File]::ReadAllText($envPath)
        $defaults = [ordered]@{
            POSTGRES_PASSWORD = (Random-Value 24)
            JWT_SECRET_BASE64 = (Random-Value 32)
            SPRING_PROFILES_ACTIVE = 'demo'
            DEMO_CITIZEN_ONE_EMAIL = 'citizen.one@example.test'
            DEMO_CITIZEN_ONE_PASSWORD = (Random-Value 18)
            DEMO_CITIZEN_TWO_EMAIL = 'citizen.two@example.test'
            DEMO_CITIZEN_TWO_PASSWORD = (Random-Value 18)
            DEMO_AUTHORITY_EMAIL = 'authority@example.test'
            DEMO_AUTHORITY_PASSWORD = (Random-Value 18)
        }
        if ($values.POSTGRES_PASSWORD -and $values.POSTGRES_PASSWORD -notlike 'replace-*') {
            $defaults.POSTGRES_PASSWORD = $values.POSTGRES_PASSWORD
        }
        $defaults['DB_PASSWORD'] = $defaults.POSTGRES_PASSWORD
        foreach ($entry in $defaults.GetEnumerator()) {
            if (-not $values[$entry.Key] -or $values[$entry.Key] -like 'replace-*') {
                $pattern = '(?m)^' + [regex]::Escape($entry.Key) + '=.*\r?$'
                $line = "$($entry.Key)=$($entry.Value)"
                if ([regex]::IsMatch($text, $pattern)) { $text = [regex]::Replace($text, $pattern, $line) }
                else { $text = $text.TrimEnd() + "`r`n$line`r`n" }
            }
        }
        [IO.File]::WriteAllText($envPath, $text, [Text.UTF8Encoding]::new($false))
        Write-Output 'Local .env prepared. Existing configured values preserved. Read DEMO_* credentials in .env; no credentials are printed.'
        return
    }
    $values = Read-LocalEnv
    $backendPort = if ($values.APP_PORT) { $values.APP_PORT } else { '8080' }
    if ($Action -eq 'start') {
        if ((node --version) -ne 'v22.14.0') { throw 'Use Node 22.14.0 as pinned in frontend/.nvmrc.' }
        foreach ($key in @('JWT_SECRET_BASE64','POSTGRES_PASSWORD')) {
            if (-not $values[$key] -or $values[$key] -like 'replace-*') { throw "Configure $key using -Action setup first." }
        }
        docker compose --env-file $envPath up --build -d --wait --wait-timeout 180 database backend
        Require-Success 'Compose startup'
        Wait-LocalUrl "http://127.0.0.1:$backendPort/actuator/health"
        if (-not (Managed-Frontend)) {
            if (Get-NetTCPConnection -State Listen -LocalPort 5173 -ErrorAction SilentlyContinue) {
                throw 'Port 5173 is occupied by an unmanaged process. Stop it before starting this frontend.'
            }
            Push-Location $frontendPath
            try { npm.cmd ci; Require-Success 'npm ci' } finally { Pop-Location }
            New-Item -ItemType Directory -Force -Path $runtimePath | Out-Null
            $env:API_PROXY_TARGET = "http://127.0.0.1:$backendPort"
            $vitePath = Join-Path $frontendPath 'node_modules/vite/bin/vite.js'
            $process = Start-Process -FilePath (Get-Command node).Source -ArgumentList @("`"$vitePath`"", '--host', '127.0.0.1') -WorkingDirectory $frontendPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimePath 'frontend.log') -RedirectStandardError (Join-Path $runtimePath 'frontend-error.log')
            $managed = Get-CimInstance Win32_Process -Filter "ProcessId = $($process.Id)"
            @{ pid=$process.Id; createdAt=$managed.CreationDate.ToUniversalTime().ToString('o') } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtimePath 'frontend.json')
        }
        Wait-LocalUrl 'http://127.0.0.1:5173/login'
        Write-Output "Frontend: http://127.0.0.1:5173/login | Backend: http://127.0.0.1:$backendPort | Credentials: .env"
        return
    }
    if ($Action -eq 'stop') {
        $process = Managed-Frontend
        if ($process) { Stop-Process -Id $process.ProcessId }
        docker compose --env-file $envPath stop backend database
        Require-Success 'Compose stop'
        Write-Output 'Stopped local services. Database volume and accounts preserved.'
        return
    }
    docker compose --env-file $envPath ps
    $process = Managed-Frontend
    Write-Output "Managed frontend running: $([bool]$process)"
} finally { Pop-Location }
