# Backup VitruviusServer Postgres + vsum-storage (resists accidental wipe).
param(
    [string]$BackupRoot = $(Join-Path $PSScriptRoot "..\backups")
)

$ErrorActionPreference = "Stop"
$serverRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dest = Join-Path $BackupRoot $stamp
New-Item -ItemType Directory -Force -Path $dest | Out-Null

Write-Output "Backing up to $dest"

# SQL dump (VSUM list + view updates)
$sqlFile = Join-Path $dest "vitruvius-server.sql"
docker exec vitruvius-server-postgres pg_dump -U postgres vitruvius-server | Set-Content -Path $sqlFile -Encoding utf8
Write-Output "Wrote $sqlFile"

# Model files
$storage = Join-Path $serverRoot "vsum-storage"
if (Test-Path $storage) {
    Copy-Item -Recurse $storage (Join-Path $dest "vsum-storage")
    Write-Output "Copied vsum-storage"
}

Write-Output "Backup complete."
