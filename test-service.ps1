# PowerShell test script for PDFBox Analysis Service (Windows)

param(
    [string]$PdfFile = ""
)

$BaseUrl = "http://localhost:8080"

Write-Host "`nPDFBox Analysis Service - Test Script" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/pdf/health" -Method Get
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Health check passed" -ForegroundColor Green
        Write-Host "Response: $($response.Content)"
    }
} catch {
    Write-Host "✗ Health check failed" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 2: PDF Analysis
if ([string]::IsNullOrEmpty($PdfFile)) {
    Write-Host "Test 2: PDF Analysis" -ForegroundColor Yellow
    Write-Host "No PDF file provided. Usage: .\test-service.ps1 -PdfFile <path-to-pdf>" -ForegroundColor Red
    Write-Host "Skipping PDF analysis test" -ForegroundColor Yellow
} else {
    if (-not (Test-Path $PdfFile)) {
        Write-Host "Error: File '$PdfFile' not found" -ForegroundColor Red
        exit 1
    }

    Write-Host "Test 2: Analyzing PDF: $PdfFile" -ForegroundColor Yellow

    try {
        $form = @{
            file = Get-Item -Path $PdfFile
        }

        $response = Invoke-WebRequest -Uri "$BaseUrl/api/pdf/analyze" -Method Post -Form $form

        if ($response.StatusCode -eq 200) {
            Write-Host "✓ PDF analysis successful" -ForegroundColor Green
            Write-Host ""
            Write-Host "Analysis Results:"
            $json = $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
            Write-Host $json
        }
    } catch {
        Write-Host "✗ PDF analysis failed" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "All tests completed!" -ForegroundColor Green
