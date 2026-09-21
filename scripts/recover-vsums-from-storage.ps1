# Recover VSUM list entries from orphaned vsum-storage folders (local + optional sibling clone).
# Requires: Docker Postgres up, VitruviusServer API optional.
param(
    [string]$SiblingStorage = "D:\MSC CS KIT\SEM 2\PRACTICAL_CONTIN_SOFTWARE_ENGG\tejas-redkar-abhishek-khetre-1\VitruviusServer\vsum-storage"
)

$ErrorActionPreference = "Stop"
$serverRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$storage = Join-Path $serverRoot "vsum-storage"
New-Item -ItemType Directory -Force -Path $storage | Out-Null

function Get-VsumFolders([string]$root) {
    if (-not (Test-Path $root)) { return @() }
    Get-ChildItem $root -Directory | Where-Object {
        $_.Name -match '^(?<mm>.+)-(?<uuid>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})'
    }
}

# Copy missing sibling folders into local storage (keep newest by LastWriteTime per UUID)
$byUuid = @{}
foreach ($dir in (Get-VsumFolders $storage)) {
    if ($dir.Name -match '^(?<mm>.+)-(?<uuid>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})') {
        $byUuid[$Matches.uuid] = @{ Mm = $Matches.mm; Dir = $dir; Source = "local" }
    }
}

if (Test-Path $SiblingStorage) {
    foreach ($dir in (Get-VsumFolders $SiblingStorage)) {
        if ($dir.Name -match '^(?<mm>.+)-(?<uuid>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})') {
            $uuid = $Matches.uuid
            $mm = $Matches.mm
            $destName = $dir.Name
            $destPath = Join-Path $storage $destName
            if (-not (Test-Path $destPath)) {
                Copy-Item -Recurse $dir.FullName $destPath
                Write-Output "Copied sibling folder $destName"
            }
            $existing = $byUuid[$uuid]
            if (-not $existing -or $dir.LastWriteTime -gt $existing.Dir.LastWriteTime) {
                $byUuid[$uuid] = @{ Mm = $mm; Dir = (Get-Item $destPath); Source = "sibling" }
            }
        }
    }
}

# Refresh map from local storage after copies
$byUuid = @{}
foreach ($dir in (Get-VsumFolders $storage)) {
    if ($dir.Name -match '^(?<mm>.+)-(?<uuid>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})') {
        $uuid = $Matches.uuid
        $existing = $byUuid[$uuid]
        if (-not $existing -or $dir.LastWriteTime -gt $existing.Dir.LastWriteTime) {
            $byUuid[$uuid] = @{ Mm = $Matches.mm; Dir = $dir }
        }
    }
}

$inserts = New-Object System.Collections.Generic.List[string]
foreach ($entry in $byUuid.GetEnumerator()) {
    $uuid = $entry.Key
    $mm = $entry.Value.Mm
    $short = $uuid.Substring(0, 8)
    $name = "Recovered $mm ($short)"
    $desc = "Recovered from vsum-storage folder $($entry.Value.Dir.Name)"
    $nameEsc = $name.Replace("'", "''")
    $descEsc = $desc.Replace("'", "''")
    $mmEsc = $mm.Replace("'", "''")
    $inserts.Add(@"
INSERT INTO "vsum-infos" (id, name, description, meta_model_name)
VALUES ('$uuid', '$nameEsc', '$descEsc', '$mmEsc')
ON CONFLICT (id) DO NOTHING;
"@)
}

$sql = ($inserts -join "`n")
$sqlFile = Join-Path $env:TEMP "recover-vsums.sql"
Set-Content -Path $sqlFile -Value $sql -Encoding utf8
Write-Output "Registering $($byUuid.Count) VSUM ids into Postgres..."
Get-Content $sqlFile | docker exec -i vitruvius-server-postgres psql -U postgres -d vitruvius-server | Select-Object -Last 20

Write-Output "Done. Refresh the UI VSUM list."
