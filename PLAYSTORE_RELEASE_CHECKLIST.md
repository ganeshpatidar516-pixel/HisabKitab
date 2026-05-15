# HisabKitab Play Store Release Checklist

Use this checklist before every production submission.

## 0) Release governance (Phase-8)

- [ ] `RELEASE_GOVERNANCE.md` — rollback owner + contacts filled
- [ ] Gate log archived under `release-artifacts/gate-logs/` (see governance doc)
- [ ] After any API TLS cert change: `scripts/update-cert-pins.ps1` run and release smoke on device

## 1) Build and Quality Gates

- [ ] `:app:testDebugUnitTest` passes fully
- [ ] `:app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.domain.sync.*"` passes (sync codec/backoff)
- [ ] `scripts/commandos_quality_gate.ps1` passes
- [ ] Release build generated successfully (`assembleRelease` / `bundleRelease`)
- [ ] One-click release validation gate passes: `scripts/release_validation_gate.ps1`
- [ ] Manual sacred flow smoke test PASS:
  - [ ] Customer ledger add/update
  - [ ] Bill create + clear
  - [ ] Reminder trigger + history
  - [ ] Settings save + reopen consistency

## 2) Regression Suite (Release Testing System)

- [ ] Instrumented sacred flow test passes (`SacredFlowInstrumentedTest`)
- [ ] Compose sacred flow test passes (`SacredFlowComposeHiltTest`)
- [ ] Room migration test passes (`AppDatabaseMigrationTest`)
- [ ] No regression in customer ledger math or bill settlement behavior

## 3) Offline / Data Protection Validation

- [ ] Offline entry survives app restart and later syncs correctly
- [ ] Backup validation report succeeds before destructive restore
- [ ] Restore apply requires explicit confirmation phrase and is tested once
- [ ] No data loss seen in post-restore ledger balances

## 4) Security and Privacy

- [ ] No hardcoded secrets/tokens in source
- [ ] Database encryption passphrase rotation logic verified
- [ ] Privacy Policy URL is live and public
- [ ] Account/data deletion process documented and reachable by users

## 5) Play Policy Compliance

- [ ] Sensitive permissions are justified in store listing and in-app UX
- [ ] `POST_NOTIFICATIONS` runtime prompt is contextual
- [ ] `READ_CONTACTS` usage is clear and optional
- [ ] Notification listener usage has explicit user consent flow
- [ ] Data Safety form in Play Console matches real app behavior

## 6) Store Listing and Assets

- [ ] App title, short description, full description finalized
- [ ] Feature graphic, screenshots, icon updated
- [ ] Contact email and support details verified
- [ ] App category, content rating, ads declaration reviewed

## 7) Rollout Strategy

- [ ] Staged rollout enabled (start with 5% cohort)
- [ ] Crash and ANR monitoring alerts configured
- [ ] Rollback owner and emergency contacts documented in `RELEASE_GOVERNANCE.md`
- [ ] Crashlytics watched for `bill_pdf_not_ready`, `sync_cloud_mirror_failed`, `balance_cache_drift`
- [ ] Canary guard / feature toggle rollback path tested

## Go/No-Go

Ship only when all critical checks are green and no P0/P1 issue is open in sacred flows.
