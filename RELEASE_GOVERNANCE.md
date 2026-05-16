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

## Phase-9 live monitoring (P0 — configure before staged %)

Full steps: `docs/ops/PHASE9_LIVE_MONITORING_SETUP.md`

- [ ] Firebase Crashlytics alerts enabled (fatals + spike)
- [ ] Play Android vitals alerts enabled
- [ ] 48h daily ops review scheduled (first week of rollout)
- [ ] BigQuery export linked (weekly review)

## Phase-9 P2 — unified ops funnel (app)

- [ ] Run `scripts/phase9_ops_canary.ps1` before each staged % bump
- [ ] BigQuery dashboards use `hk_ops_funnel` (see `docs/ops/bigquery/hk_ops_funnel_queries.sql`)
- [ ] Settings → Cloud shows **Production health snapshot** on test device
- [ ] Railway uptime checks: `docs/ops/RAILWAY_UPTIME_CHECKS.md`

## Telemetry signals (Crashlytics)

When crash reporting is ON in Settings, each session includes `version_code`, `version_name`, `build_type`.

Non-fatals include:

| Signal | Meaning |
|--------|---------|
| `bill_pdf_not_ready` | Bill saved; PDF generation failed or share file missing |
| `invoice_save_issue` | Bill save failed or PDF not ready after save |
| `sync_cloud_mirror_failed` | FastAPI sync OK but Firestore re-mirror failed |
| `balance_cache_drift` | SQL balance ≠ `balanceCache` (detect-only unless auto-repair ON) |
| `balance_cache_repaired` | Opt-in or manual repair aligned cache to SQL sum |
| `sync_cycle_degraded` | Sync cycle had permanent failures or auth expired |
| `api_call_degraded` | API 5xx, network failure, or call ≥10s (path only, no PII) |

**Analytics funnel** (`hk_ops_funnel`, when Analytics ON): domains `session`, `ocr`, `sync`, `invoice`, `api` — see `docs/ops/PHASE9_P2_ENTERPRISE_OPS.md`.

## Scope lock

Ship only items in `RELEASE_SCOPE_LOCK.md`. Sacred flows: customer ledger, bill create/clear, reminder baseline, settings baseline.

## Git remote (recommended)

Configure `git remote` and push `main` before submission so rollback/hotfix branches are not single-machine only.
