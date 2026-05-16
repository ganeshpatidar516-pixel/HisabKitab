# ADR-002: Single route source via AppRoutes

**Status:** Accepted (Phase-10 Tier B)  
**Date:** 2026-05-16  
**Sacred impact:** None — route strings unchanged

## Context

`AppNavGraph` used string literals while `AppRoutes` existed but was unused in the graph, causing drift risk for deep links and tests.

## Decision

All `composable(...)` and `navigate(...)` calls in `AppNavGraph` use `AppRoutes` constants or builder functions (`customerLedger(id)`, `scanBill(...)`, etc.).

Route **strings** remain identical to pre-change values.

## Verification

- Instrumented sacred tests still pass.
- `rg 'composable\("' AppNavGraph.kt` — only parameterized patterns if any remain temporarily.
