# HisabKitab Pro — Release Governance (Phase-8)

Fill this once per release cycle. Do not ship production without completed **Rollback** and **Gate** sections.

## Rollback owner (required)

| Role | Name | Contact |
|------|------|---------|
| **Rollback owner** | _fill before ship_ | _phone / WhatsApp_ |
| **Backup owner** | _fill before ship_ | _phone / WhatsApp_ |
| **Play Console owner** | _fill before ship_ | _email_ |

## Incident decision rule

1. **P0** (ledger wrong balance, data loss, crash loop): halt rollout → Play **halt rollout** → rollback owner decides previous AAB vs hotfix.
2. **P1** (sync stuck, PDF share broken for many users): staged pause at current % → hotfix within 24h.
3. **P2** (telemetry-only, isolated UI): document; fix in next train.

## Mandatory gates (in order)

1. `powershell -ExecutionPolicy Bypass -File ".\scripts\release_validation_gate.ps1"`
2. `MANUAL_SACRED_SMOKE_TEST.md` on **release** build (not debug-only).
3. `SUBMISSION_DAY_CHECKLIST_HI.md` — all mandatory boxes checked.
4. Play **Internal testing** track first; staged % only after 24–48h clean Crashlytics.

### Archive gate output (recommended)

```powershell
$ts = Get-Date -Format "yyyyMMdd_HHmm"
$dir = "release-artifacts\gate-logs\$ts"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
powershell -ExecutionPolicy Bypass -File ".\scripts\release_validation_gate.ps1" *>&1 |
  Tee-Object -FilePath "$dir\release_validation_gate.log"
```

Keep the log with the AAB you upload.

## API base URL override (dev/staging)

Optional in `local.properties` (not committed):

```properties
api.base.url=https://your-staging-host.example/
```

Rebuild after change. If the host differs from production, update certificate pins via `scripts/update-cert-pins.ps1` before release builds.

## Certificate pinning (release)

Release builds enable TLS pinning (`CERT_PINNING_ENABLED=true`). After **Railway / API cert rotation**:

```powershell
powershell -ExecutionPolicy Bypass -File ".\scripts\update-cert-pins.ps1"
```

Then run `assembleRelease` on a device and verify login + sync once before Play upload.

## Telemetry signals (Crashlytics)

When crash reporting is ON in Settings, non-fatals include:

| Signal | Meaning |
|--------|---------|
| `bill_pdf_not_ready` | Bill saved; PDF generation failed or share file missing |
| `sync_cloud_mirror_failed` | FastAPI sync OK but Firestore re-mirror failed |
| `balance_cache_drift` | SQL balance ≠ `balanceCache` (detect-only unless auto-repair ON) |
| `balance_cache_repaired` | Opt-in or manual repair aligned cache to SQL sum |
| `sync_cycle_degraded` | Sync cycle had permanent failures or auth expired |

## Scope lock

Ship only items in `RELEASE_SCOPE_LOCK.md`. Sacred flows: customer ledger, bill create/clear, reminder baseline, settings baseline.

## Git remote (recommended)

Configure `git remote` and push `main` before submission so rollback/hotfix branches are not single-machine only.
