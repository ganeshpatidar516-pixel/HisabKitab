# Phase-9 Live Monitoring Setup (P0)

Complete these in Firebase / Play Console **before** staged rollout above 5%.

## 1) Firebase Crashlytics alerts

1. Open [Firebase Console](https://console.firebase.google.com) → project **hisabkitab-pro-5d4ee** → **Crashlytics**.
2. Enable email alerts for:
   - New fatal issue
   - Regressed issue
   - Velocity alert (spike vs baseline)
3. Filter non-fatals by custom keys:
   - `bill_pdf_not_ready`
   - `sync_cycle_degraded`
   - `invoice_save_issue`
   - `api_call_degraded`

## 2) Play Console vitals

1. Play Console → **HisabKitab Pro** → **Monitor and improve** → **Android vitals**.
2. Subscribe to alerts: crash rate, ANR rate, wakeups (if shown).
3. Compare **internal testing** vs **production** tracks separately.

## 3) 48-hour rollout watch (mandatory)

| Check | Frequency |
|-------|-----------|
| Crash-free users % | Daily |
| Top 3 Crashlytics issues | Daily |
| Non-fatal: `sync_cycle_degraded` | Daily |
| Non-fatal: `bill_pdf_not_ready` | Daily |
| Play vitals regression | Daily |

**Halt rollout** if: new fatal in ledger/bill path, crash rate >2× baseline, or sync auth storm.

## 4) Firebase Analytics → BigQuery (P1)

1. Firebase Console → **Project settings** → **Integrations** → link **BigQuery**.
2. Enable daily export for Analytics.
3. Weekly query ideas:
   - `hk_ocr_funnel` event counts by `phase`
   - Crash correlation by `version_name` custom key (Crashlytics)

## 5) Session keys on every crash (app-side, Phase-9 P1)

When Crashlytics is enabled, each session includes:

- `version_code`
- `version_name`
- `build_type` (`release` / `debug`)

## 6) API degraded calls (app-side, Phase-9 P1)

Non-fatal `api_call_degraded` when:

- HTTP 5xx, or
- Network I/O failure, or
- Duration ≥ 10 seconds

No URLs with query tokens; path only (e.g. `/api/v1/transactions`).
