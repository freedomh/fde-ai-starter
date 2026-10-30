$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot 'infra\.env'
if (Test-Path -LiteralPath $envFile) { Write-Host 'Keeping existing local configuration.'; exit 0 }
$projectConfig = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'project.json') -Raw | ConvertFrom-Json
$pathHasher = [System.Security.Cryptography.SHA256]::Create()
try {
    $rootBytes = [System.Text.Encoding]::UTF8.GetBytes([System.IO.Path]::GetFullPath($PSScriptRoot).ToLowerInvariant())
    $rootHash = (-join ($pathHasher.ComputeHash($rootBytes) | ForEach-Object { $_.ToString('x2') })).Substring(0,8)
} finally { $pathHasher.Dispose() }
function New-LocalPassword {
    $bytes = New-Object byte[] 24
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return -join ($bytes | ForEach-Object { $_.ToString('x2') })
}
$lines = @(
    ('COMPOSE_PROJECT_NAME=' + $projectConfig.name + '-' + $rootHash)
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
