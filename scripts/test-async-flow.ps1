# Vitruvius async propagation smoke test (PowerShell-safe, no inline curl JSON)
$ErrorActionPreference = "Stop"
$base = "http://localhost:8000/api"

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Uri,
        [string]$Body = $null
    )
    $params = @{
        Uri             = $Uri
        Method          = $Method
        ContentType     = "application/json"
        UseBasicParsing = $true
    }
    if ($Body) { $params.Body = $Body }
    try {
        $response = Invoke-WebRequest @params
        return @{
            StatusCode = [int]$response.StatusCode
            Body       = $response.Content
        }
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $errorBody = $_.ErrorDetails.Message
            if ([string]::IsNullOrWhiteSpace($errorBody)) {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $errorBody = $reader.ReadToEnd()
                    $reader.Close()
                }
            }
            return @{
                StatusCode = $statusCode
                Body       = $errorBody
            }
        }
        throw
    }
}

function Assert-Status($step, $response, $expected) {
    if ($response.StatusCode -ne $expected) {
        Write-Host "FAILED at $step (HTTP $($response.StatusCode)):" -ForegroundColor Red
        Write-Host $response.Body
        exit 1
    }
}

function Parse-Json($step, $json) {
    if ([string]::IsNullOrWhiteSpace($json)) {
        Write-Host "FAILED at $step : empty response (is the server running on port 8000?)" -ForegroundColor Red
        exit 1
    }
    try {
        return $json | ConvertFrom-Json
    } catch {
        Write-Host "FAILED at $step : response is not JSON" -ForegroundColor Red
        Write-Host $json
        exit 1
    }
}

Write-Host "=== Vitruvius async propagation test ===" -ForegroundColor Cyan
Write-Host "Server: $base`n"

# Step 1: Create VSUM
$createBody = (@{
    metamodelName = "SystemRootVsum"
    name          = "Manual Async Test"
    description   = "..."
} | ConvertTo-Json -Compress)

$r = Invoke-Api -Method POST -Uri "$base/v1/vsums" -Body $createBody
Assert-Status "Step 1 (create VSUM)" $r 200
$vsumId = (Parse-Json "Step 1" $r.Body).id
Write-Host "1. VSUM created: $vsumId"

# Step 2: Selector
$r = Invoke-Api -Method POST -Uri "$base/v1/vsums/$vsumId/view-types/default/selectors"
Assert-Status "Step 2 (selector)" $r 200
$selector = Parse-Json "Step 2" $r.Body
$selectorId = $selector.id
$idx = if ($selector.selectableObjects[0].eClass -like "*System*") { 0 } else { 1 }
$selectedObjectId = $selector.selectableObjects[$idx]._id
Write-Host "2. Selector: $selectorId, object: $selectedObjectId"

# Step 3: Open view
$openBody = (@{
    vsumId            = $vsumId
    selectorId        = $selectorId
    selectedObjectIds = @($selectedObjectId)
} | ConvertTo-Json -Compress)

$r = Invoke-Api -Method POST -Uri "$base/v1/views" -Body $openBody
Assert-Status "Step 3 (open view)" $r 200
$viewId = (Parse-Json "Step 3" $r.Body).id
Write-Host "3. View opened: $viewId"

# Step 4: Commit
$commitBody = '[{"uri":"/example.model","content":{"eClass":"http://vitruv.tools/methodologisttemplate/model#//System","_id":"/","components":[{"name":"First Component"}]}}]'
$r = Invoke-Api -Method PUT -Uri "$base/v1/views/$viewId" -Body $commitBody
Assert-Status "Step 4 (commit)" $r 200
Write-Host "4. Commit: OK (HTTP 200)"

# Step 5: Start async propagation
$r = Invoke-Api -Method POST -Uri "$base/v1/views/$viewId/apply-update/async"
Assert-Status "Step 5 (async start)" $r 202
if ($r.Body -like "*No static resource*apply-update/async*") {
    Write-Host "`nHINT: The running server JAR does not include the async endpoint." -ForegroundColor Yellow
    Write-Host "      Rebuild and restart: mvn clean package, then java -jar target/VitruviusServer-0.0.1-SNAPSHOT.jar"
    exit 1
}
$taskId = (Parse-Json "Step 5" $r.Body).taskId
Write-Host "5. Async started: taskId=$taskId"

# Step 6: Poll (up to 60 seconds)
Write-Host "6. Polling task status..."
$finalStatus = $null
for ($i = 0; $i -lt 120; $i++) {
    $r = Invoke-Api -Uri "$base/v1/tasks/$taskId"

    if ($r.StatusCode -ne 200) {
        Write-Host "   attempt $i : HTTP $($r.StatusCode) - $($r.Body)" -ForegroundColor Yellow
        Start-Sleep -Milliseconds 500
        continue
    }

    $status = Parse-Json "Step 6 (poll)" $r.Body
    Write-Host "   attempt $i : state=$($status.state)"

    if ($status.state -eq "WAITING_USER_INTERACTION") {
        Write-Host "`nTask is waiting for user interaction. Submit an answer, then poll again:" -ForegroundColor Yellow
        Write-Host "  POST $base/v1/tasks/$taskId/interaction"
        Write-Host "  (Step 4 provider wiring is needed for full interaction support.)"
        exit 0
    }

    if ($status.state -eq "COMPLETED" -or $status.state -eq "FAILED") {
        $finalStatus = $status
        break
    }

    Start-Sleep -Milliseconds 500
}

if ($null -eq $finalStatus) {
    Write-Host "`nTIMEOUT: task did not finish within 60 seconds." -ForegroundColor Red
    Write-Host "Check server logs and keep the server running in a separate terminal."
    exit 1
}

Write-Host "`n=== FINAL STATUS ===" -ForegroundColor Green
Write-Host ($finalStatus | ConvertTo-Json -Depth 10)

if ($finalStatus.state -eq "FAILED") {
    Write-Host "`nPropagation failed: $($finalStatus.error)" -ForegroundColor Red
    exit 1
}

if ($finalStatus.result -and $finalStatus.result -like "*First Component*") {
    Write-Host "`nSUCCESS: result contains 'First Component'." -ForegroundColor Green
} else {
    Write-Host "`nCompleted, but result may not include the committed change. Check result above." -ForegroundColor Yellow
}
