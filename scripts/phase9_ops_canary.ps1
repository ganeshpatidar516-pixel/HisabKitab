# Phase-9 P2 — pre-rollout ops canary (compile + unit tests + checklist)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root "gradlew.bat"))) {
    throw "Run from repo root via scripts\phase9_ops_canary.ps1"
}
Set-Location $root

Write-Host "== Phase-9 ops canary ==" -ForegroundColor Cyan

Write-Host "1/4 Ops telemetry unit tests" -ForegroundColor Cyan
& .\gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.core.firebase.OpsTelemetryHubTest" --no-daemon
if ($LASTEXITCODE -ne 0) { throw "OpsTelemetryHubTest failed" }

Write-Host "2/4 Sync domain tests" -ForegroundColor Cyan
& .\gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.domain.sync.*" --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Sync unit tests failed" }

Write-Host "3/4 Release compile (no signing)" -ForegroundColor Cyan
& .\gradlew.bat :app:compileReleaseKotlin --no-daemon
if ($LASTEXITCODE -ne 0) { throw "compileReleaseKotlin failed" }

Write-Host "4/4 Post-upload ops checklist (manual)" -ForegroundColor Cyan
Write-Host "  - docs/ops/PHASE9_LIVE_MONITORING_SETUP.md (Crashlytics + Play vitals alerts)"
Write-Host "  - docs/ops/PHASE9_P2_ENTERPRISE_OPS.md (hk_ops_funnel in BigQuery)"
Write-Host "  - RELEASE_GOVERNANCE.md rollback owners filled"
Write-Host "  - Play pre-launch report after internal track upload"

Write-Host ""
Write-Host "Ops canary PASS (automated steps). Complete manual checklist before staged %." -ForegroundColor Green
