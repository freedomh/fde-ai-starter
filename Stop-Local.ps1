$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & docker compose --env-file infra/.env -f infra/compose.yaml down
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop this project.' }
    Write-Host 'Stopped this project. MySQL volume is retained.'
} finally { Pop-Location }
