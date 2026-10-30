$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot 'infra\.env'
if (Test-Path -LiteralPath $envFile) { Write-Host 'Keeping existing local configuration.'; exit 0 }
$projectConfig = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'project.json') -Raw | ConvertFrom-Json
function New-LocalPassword {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return -join ($bytes | ForEach-Object { $_.ToString('x2') })
}
$lines = @(
    ('COMPOSE_PROJECT_NAME=' + $projectConfig.name)
    ('DB_NAME=' + $projectConfig.database)
    ('BACKEND_PORT=' + $projectConfig.backendPort)
    ('WEB_PORT=' + $projectConfig.webPort)
    ('ADMIN_PORT=' + $projectConfig.adminPort)
    ('MYSQL_ROOT_PASSWORD=' + (New-LocalPassword))
    ('DB_PASSWORD=' + (New-LocalPassword))
    ('REDIS_PASSWORD=' + (New-LocalPassword))
)
[System.IO.File]::WriteAllLines($envFile, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host 'Created infra/.env with random local passwords. Do not commit this file.'
