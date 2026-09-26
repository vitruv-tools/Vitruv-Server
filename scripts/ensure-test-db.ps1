# Creates the Maven test database if it does not already exist.
param(
    [string]$Container = "vitruvius-server-postgres",
    [string]$User = "postgres",
    [string]$Db = "vitruvius-server-test"
)

$exists = docker exec $Container psql -U $User -tAc "SELECT 1 FROM pg_database WHERE datname='$Db'" 2>$null
if ($exists -match "1") {
    Write-Host "Database '$Db' already exists."
    exit 0
}

docker exec $Container psql -U $User -c "CREATE DATABASE `"$Db`";"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create database '$Db'. Is Docker running and container '$Container' up?"
    exit 1
}
Write-Host "Created database '$Db'."
