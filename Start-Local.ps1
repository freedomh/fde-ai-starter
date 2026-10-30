$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $containerType = & docker info --format '{{.OSType}}'
    if ($LASTEXITCODE -ne 0 -or $containerType -ne 'linux') { throw 'Start Docker Desktop in Linux container mode first.' }
    & powershell -NoProfile -ExecutionPolicy Bypass -File .\Init-Local.ps1
    if ($LASTEXITCODE -ne 0) { throw 'Local configuration initialization failed.' }
    & docker compose --env-file infra/.env -f infra/compose.yaml config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose configuration is invalid.' }
    & docker compose --env-file infra/.env -f infra/compose.yaml up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'Build/startup failed. Inspect the first error above.' }
    $localConfig = @{}
    Get-Content -LiteralPath .\infra\.env | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $localConfig[$matches[1]] = $matches[2] } }
    $healthAddress = 'http://127.0.0.1:' + $localConfig['BACKEND_PORT'] + '/api/health'
    $ready = $false
    for ($retry = 0; $retry -lt 60; $retry++) {
        try { if ((Invoke-RestMethod $healthAddress -TimeoutSec 3).status -eq 'UP') { $ready = $true; break } }
        catch { Start-Sleep -Seconds 2 }
    }
    if (-not $ready) { throw 'Backend did not become ready. Use docker compose logs backend.' }
    Write-Host ('Web:   http://127.0.0.1:' + $localConfig['WEB_PORT'])
    Write-Host ('Admin: http://127.0.0.1:' + $localConfig['ADMIN_PORT'])
    Write-Host ('API:   ' + $healthAddress)
} finally { Pop-Location }
