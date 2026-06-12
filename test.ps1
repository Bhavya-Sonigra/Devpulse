param([string]$Action)

$Backend = "http://localhost:8080"

if ($Action -eq "generate") {
    Write-Host "Generating test events..." -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$Backend/api/test/generate-sample-events" -Method POST -ContentType "application/json" -ErrorAction Stop
        Write-Host "SUCCESS: " -ForegroundColor Green -NoNewline
        Write-Host $response.Content
    } catch {
        Write-Host "ERROR: Cannot connect to backend. Make sure it's running." -ForegroundColor Red
        exit 1
    }
}

elseif ($Action -eq "status") {
    Write-Host "Checking event status..." -ForegroundColor Cyan
    try {
        $response = Invoke-WebRequest -Uri "$Backend/api/test/events-count" -Method Get -ContentType "application/json" -ErrorAction Stop
        $data = $response.Content | ConvertFrom-Json
        Write-Host "Total: $($data.total) | Processed: $($data.processed) | Unprocessed: $($data.unprocessed)" -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Failed to get status" -ForegroundColor Red
        exit 1
    }
}

elseif ($Action -eq "clean") {
    Write-Host "WARNING: This will delete ALL events!" -ForegroundColor Red
    $confirm = Read-Host "Type 'yes' to confirm"
    if ($confirm -ne "yes") { return }
    
    try {
        $response = Invoke-WebRequest -Uri "$Backend/api/test/clean-events" -Method POST -ContentType "application/json" -ErrorAction Stop
        Write-Host "CLEANED: " -ForegroundColor Green -NoNewline
        Write-Host $response.Content
    } catch {
        Write-Host "ERROR: Failed to clean" -ForegroundColor Red
        exit 1
    }
}

else {
    Write-Host "Usage:" -ForegroundColor Cyan
    Write-Host "  .\test.ps1 generate          - Generate 15 test events"
    Write-Host "  .\test.ps1 status            - Check event count"
    Write-Host "  .\test.ps1 clean             - Delete all events"
    Write-Host "  .\test.ps1 compute TOKEN     - Compute metrics (needs JWT token)"
}
