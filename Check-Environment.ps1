$ErrorActionPreference = 'Continue'
Write-Host 'Read-only environment check. No installation or system changes.'
Get-Command git,docker -ErrorAction SilentlyContinue | Select-Object Name,Source
& git --version
& docker version
& docker compose version
& docker info --format '{{.OSType}}'
if (Get-Command wsl -ErrorAction SilentlyContinue) { & wsl --version }
Write-Host 'Next: Start Docker Desktop, then run Start-Local.ps1 in the repository root.'
