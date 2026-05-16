# ADR-001: UI must not import Room directly

**Status:** Accepted (Phase-10 Tier B)  
**Date:** 2026-05-16  
**Sacred impact:** None — additive rule for new code

## Context

Several screens and ViewModels import `AppDatabase` or DAOs directly. Schema changes then require wide UI edits and complicate testing.

## Decision

1. **New code** in `ui/` must use repository interfaces or thin use-cases — no new `AppDatabase` imports.
2. **Existing** direct DB access may be refactored incrementally; sacred screens (`CustomerLedgerScreen`, bill flows) change only when behavior-neutral.
3. Code review checklist: reject PRs that add `import com.ganesh.hisabkitabpro.data.local.AppDatabase` under `ui/`.

## Consequences

### Positive

- Clearer ownership; safer Room migrations.

### Negative

- Short-term duplication until legacy screens are migrated.

## Verification

- `rg "data.local.AppDatabase" app/src/main/java/com/ganesh/hisabkitabpro/ui` — count should not grow.
