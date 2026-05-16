# Railway / API uptime checks (Phase-9 P2 — backend ops)

Separate from the Android app. Run on the production host.

## Health endpoint

```text
GET https://hisabkitab-production-ceea.up.railway.app/api/v1/health
```

Expect HTTP 200 within 5s.

## Weekly manual checks

| Check | Tool |
|-------|------|
| Uptime | Railway dashboard → Metrics |
| 5xx rate | Railway logs filter `5xx` |
| p95 latency | Railway metrics or external ping (UptimeRobot, Better Stack) |

## Correlate with app signals

When Railway is degraded, watch Crashlytics for:

- `api_call_degraded`
- `sync_cycle_degraded`

App Analytics domain `api` / `sync` funnels in BigQuery (`hk_ops_funnel`).
