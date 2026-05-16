# Phase-9 P2 — Enterprise Ops Maturity

App-side deliverables (no sacred-flow logic changes).

## Unified funnel schema

| Field | Value |
|-------|--------|
| Analytics event | `hk_ops_funnel` |
| `domain` | `session`, `ocr`, `sync`, `invoice`, `api` |
| `phase` | Short outcome code (e.g. `bill_save_ok`, `cycle_degraded`) |
| Other params | Sanitized strings only — no names, phones, amounts |

**Code:** `OpsTelemetryHub.kt` — all OCR events route here (replaces legacy `hk_ocr_funnel`).

Crashlytics non-fatals remain in `ProductionOpsTelemetry` (degraded paths only).

## In-app ops snapshot

**Settings → Cloud & backup** shows build identity, last sync phase, and last funnel signal (device-local).

## Deferred (explicit)

| Item | Reason |
|------|--------|
| Firebase Performance SDK | Removed — placeholder `google-services.json` caused startup crashes; re-enable only with verified production Firebase project |
| OpenTelemetry / Sentry | Separate program; not in mobile hot path |
| Railway uptime automation | Backend ops — see `RAILWAY_UPTIME_CHECKS.md` |

## Pre-staged rollout canary

```powershell
powershell -ExecutionPolicy Bypass -File ".\scripts\phase9_ops_canary.ps1"
```

## BigQuery

Sample queries: `docs/ops/bigquery/hk_ops_funnel_queries.sql`
