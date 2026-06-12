#!/usr/bin/env pwsh
<#
.SYNOPSIS
    DevPulse Testing Helper Script
    
.DESCRIPTION
    Automates the testing workflow for DevPulse development and debugging.
    
.PARAMETER Action
    The action to perform: generate, compute, status, clean, trigger-report
    
.PARAMETER Token
    JWT token for authenticated requests (optional for some actions)
    
.EXAMPLE
    .\test-devpulse.ps1 -Action generate
    .\test-devpulse.ps1 -Action compute -Token "eyJ..."
    .\test-devpulse.ps1 -Action status
#>

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("generate", "compute", "status", "clean", "trigger-report")]
    [string]$Action,
    
    [Parameter(Mandatory=$false)]
    [string]$Token,
    
    [Parameter(Mandatory=$false)]
    [string]$Backend = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Write-ColorOutput {
    param([string]$Text, [string]$ForegroundColor = "White")
    Write-Host $Text -ForegroundColor $ForegroundColor
}

function Test-BackendHealth {
    Write-ColorOutput "Testing backend connectivity..." -ForegroundColor "Cyan"
    try {
        $response = Invoke-WebRequest -Uri "$Backend/actuator/health" -Method Get
        if ($response.StatusCode -eq 200) {
            Write-ColorOutput "✓ Backend is running at $Backend" -ForegroundColor "Green"
            return $true
        }
    } 
    catch {
        Write-ColorOutput "✗ Cannot connect to backend at $Backend" -ForegroundColor "Red"
        Write-ColorOutput "  Make sure the backend is running: .\mvnw.cmd spring-boot:run" -ForegroundColor "Yellow"
        return $false
    }
}

function Generate-TestEvents {
    Write-ColorOutput "`n=== Generating Test Events ===" -ForegroundColor "Cyan"
    
    try {
        $response = Invoke-WebRequest -Uri "$Backend/api/test/generate-sample-events" `
            -Method POST `
            -ContentType "application/json"
        
        $data = $response.Content | ConvertFrom-Json
        Write-ColorOutput "✓ Generated test events:" -ForegroundColor "Green"
        Write-ColorOutput "  Message: $($data.message)" -ForegroundColor "Green"
        Write-ColorOutput "  Push Events: $($data.pushEvents)" -ForegroundColor "Green"
        Write-ColorOutput "  PR Events: $($data.prEvents)" -ForegroundColor "Green"
        Write-ColorOutput "  Next: Run 'compute' action with your JWT token" -ForegroundColor "Yellow"
        
        return $true
    } catch {
        Write-ColorOutput "✗ Failed to generate test events: $_" -ForegroundColor "Red"
        return $false
    }
}

function Get-EventStatus {
    Write-ColorOutput "`n=== Event Status ===" -ForegroundColor "Cyan"
    
    try {
        $response = Invoke-WebRequest -Uri "$Backend/api/test/events-count" `
            -Method Get `
            -ContentType "application/json"
        
        $data = $response.Content | ConvertFrom-Json
        Write-ColorOutput "Event Count:" -ForegroundColor "Green"
        Write-ColorOutput "  Total: $($data.total)" -ForegroundColor "White"
        Write-ColorOutput "  Processed: $($data.processed)" -ForegroundColor "White"
        Write-ColorOutput "  Unprocessed: $($data.unprocessed)" -ForegroundColor "White"
        
        if ($data.unprocessed -gt 0) {
            Write-ColorOutput "`n  Run 'compute' action to process these events" -ForegroundColor "Yellow"
        }
        
        return $true
    } 
    catch {
        Write-ColorOutput "✗ Failed to get event status: $_" -ForegroundColor "Red"
        return $false
    }
}

function Compute-Metrics {
    Write-ColorOutput "`n=== Computing Metrics ===" -ForegroundColor "Cyan"
    
    if ([string]::IsNullOrEmpty($Token)) {
        Write-ColorOutput "✗ Token is required for compute action" -ForegroundColor "Red"
        Write-ColorOutput "  Usage: .\test-devpulse.ps1 -Action compute -Token 'your_jwt_token'" -ForegroundColor "Yellow"
        Write-ColorOutput "`n  To get your token:" -ForegroundColor "Cyan"
        Write-ColorOutput "  1. Login at http://localhost:3000/login" -ForegroundColor "White"
        Write-ColorOutput "  2. Open DevTools (F12) → Network tab" -ForegroundColor "White"
        Write-ColorOutput "  3. Refresh page, find any API call" -ForegroundColor "White"
        Write-ColorOutput "  4. Copy Authorization header value (after 'Bearer ')" -ForegroundColor "White"
        return $false
    }
    
    try {
        Write-ColorOutput "Sending request to trigger analysis..." -ForegroundColor "White"
        $headers = @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }
        
        $response = Invoke-WebRequest -Uri "$Backend/api/analysis/trigger" `
            -Method POST `
            -Headers $headers
        
        if ($response.StatusCode -eq 200) {
            Write-ColorOutput "✓ Metrics computed successfully!" -ForegroundColor "Green"
            Write-ColorOutput "  Message: $($response.Content)" -ForegroundColor "Green"
            Write-ColorOutput "`n  Refresh your dashboard at http://localhost:3000/dashboard" -ForegroundColor "Yellow"
            return $true
        }
    } 
    catch {
        $errorResponse = $_.Exception.Response
        if ($null -ne $errorResponse) {
            $reader = New-Object System.IO.StreamReader($errorResponse.GetResponseStream())
            $error = $reader.ReadToEnd()
            Write-ColorOutput "✗ Failed to compute metrics: $error" -ForegroundColor "Red"
        } else {
            Write-ColorOutput "✗ Failed to compute metrics: $_" -ForegroundColor "Red"
        }
        return $false
    }
}

function Trigger-Report {
    Write-ColorOutput "`n=== Triggering Report ===" -ForegroundColor "Cyan"
    
    if ([string]::IsNullOrEmpty($Token)) {
        Write-ColorOutput "✗ Token is required for trigger-report action" -ForegroundColor "Red"
        return $false
    }
    
    try {
        Write-ColorOutput "Sending manual report trigger..." -ForegroundColor "White"
        $headers = @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }
        
        $response = Invoke-WebRequest -Uri "$Backend/api/report/trigger" `
            -Method POST `
            -Headers $headers
        
        if ($response.StatusCode -eq 200) {
            Write-ColorOutput "✓ Report triggered successfully!" -ForegroundColor "Green"
            Write-ColorOutput "  Message: $($response.Content)" -ForegroundColor "Green"
            Write-ColorOutput "`n  💡 Check 'Recent Slack Reports' section in dashboard" -ForegroundColor "Yellow"
            return $true
        }
    } catch {
        Write-ColorOutput "✗ Failed to trigger report: $_" -ForegroundColor "Red"
        return $false
    }
}

function Clean-AllData {
    Write-ColorOutput "`n=== Cleaning All Events ===" -ForegroundColor "Cyan"
    Write-ColorOutput "WARNING: This will delete ALL events and metrics!" -ForegroundColor "Red"
    
    $confirmation = Read-Host "Type 'yes' to confirm"
    
    if ($confirmation -ne "yes") {
        Write-ColorOutput "✗ Operation cancelled" -ForegroundColor "Yellow"
        return $false
    }
    
    try {
        $response = Invoke-WebRequest -Uri "$Backend/api/test/clean-events" `
            -Method POST `
            -ContentType "application/json"
        
        $data = $response.Content | ConvertFrom-Json
        Write-ColorOutput "✓ Cleaned data:" -ForegroundColor "Green"
        Write-ColorOutput "  Message: $($data.message)" -ForegroundColor "Green"
        Write-ColorOutput "`n  Next: Run 'generate' action to create new test data" -ForegroundColor "Yellow"
        
        return $true
    } 
    catch {
        Write-ColorOutput "✗ Failed to clean data: $_" -ForegroundColor "Red"
        return $false
    }
}

# Main execution
Write-ColorOutput "`n=== DevPulse Testing Helper Script ==="  -ForegroundColor "Cyan"

# Check backend
if (-not (Test-BackendHealth)) {
    exit 1
}

# Execute action
switch ($Action) {
    "generate" {
        Generate-TestEvents | Out-Null
        Get-EventStatus | Out-Null
    }
    "status" {
        Get-EventStatus | Out-Null
    }
    "compute" {
        Compute-Metrics | Out-Null
    }
    "trigger-report" {
        Trigger-Report | Out-Null
    }
    "clean" {
        Clean-AllData | Out-Null
    }
}
