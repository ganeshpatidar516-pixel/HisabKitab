# Phase-10 Evolution Roadmap (90 days)

Reference: Phase-10 audit (read-only). Sacred flows frozen unless bugfix.

## Tier A — Governance (complete in repo)

- [x] `docs/ONBOARDING.md`
- [x] ADR index + ADR-001..003
- [x] Dependabot for Gradle catalog
- [x] CI: `phase9_ops_canary` job
- [ ] **You:** Fill rollback owners in `RELEASE_GOVERNANCE.md`

## Tier B — Hygiene (in progress)

- [x] Wire `AppRoutes` in `AppNavGraph`
- [x] Remove hot `allCustomers` StateFlow subscription
- [ ] Wire remaining `AppRoutes` in `MainActivity` / tests
- [ ] Deprecate `getAllCustomers()` at repository level (when callers = 0)

## Tier C — Requires explicit approval

- Gradle module `:commandos`
- Firebase Performance re-enable
- Room schema split
