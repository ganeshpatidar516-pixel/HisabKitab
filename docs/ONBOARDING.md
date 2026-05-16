# HisabKitab Pro — Developer Onboarding

Single entry point for building, testing, and shipping the Android app (`:app`).

## Prerequisites

- Android Studio (Ladybug+) with JDK 21
- `local.properties` with `sdk.dir=...`
- Optional: `keystore.properties` + `release/hisabkitab-upload.jks` for signed release builds

## First build

```powershell
cd <repo-root>
.\gradlew.bat :app:assembleDebug
```

## Sacred flows (do not break)

Customer ledger, bill create/clear, sync enqueue-after-local, OCR customer-scoped save, auth.

See `RELEASE_SCOPE_LOCK.md` and workspace rule `preserve-working-systems`.

## Release train (order)

1. `scripts\phase9_ops_canary.ps1` — fast automated canary
2. `scripts\release_validation_gate.ps1` — full gate (device tests if USB connected)
3. `MANUAL_SACRED_SMOKE_TEST.md` — on **release** build
4. `RELEASE_GOVERNANCE.md` — rollback owners filled, staged % rules
5. `SUBMISSION_DAY_CHECKLIST_HI.md` — Play submission

## Key docs

| Topic | Path |
|-------|------|
| Release governance | `RELEASE_GOVERNANCE.md` |
| Scope lock | `RELEASE_SCOPE_LOCK.md` |
| Ops monitoring | `docs/ops/PHASE9_LIVE_MONITORING_SETUP.md` |
| Ops funnel / BigQuery | `docs/ops/PHASE9_P2_ENTERPRISE_OPS.md` |
| Architecture decisions | `docs/architecture/README.md` |
| Long-term evolution | `docs/architecture/PHASE10_EVOLUTION_ROADMAP.md` |
| Layer boundaries | `docs/architecture/ADR-001-ui-data-boundary.md` |

## Canonical backend

Production API (default): `https://hisabkitab-production-ceea.up.railway.app/`

Override in `local.properties`: `api.base.url=...` then rebuild.

## CI

GitHub Actions: `.github/workflows/android-build.yml` — debug build + unit tests on every push.

## Module map

| Module | Role |
|--------|------|
| `:app` | Full product (UI, data, domain, sync, OCR) |
| `:core:common` | Shared `AppResult` types only |
