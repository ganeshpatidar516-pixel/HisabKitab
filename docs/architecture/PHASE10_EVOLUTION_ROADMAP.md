# Phase-10 Evolution Roadmap (90 days)

Reference: Phase-10 audit (read-only). Sacred flows frozen unless bugfix.

## Tier A — Governance (complete in repo)

- [x] `docs/ONBOARDING.md`
- [x] ADR index + ADR-001..003
- [x] Dependabot for Gradle catalog
- [x] CI: `phase9_ops_canary` job
- [ ] **You:** Fill rollback owners in `RELEASE_GOVERNANCE.md`

## Tier B — Hygiene (complete for route layer)

- [x] Wire `AppRoutes` in `AppNavGraph`
- [x] Remove hot `allCustomers` StateFlow subscription
- [x] `NavScreen` + `AIChatScreen` + `AICommandRouter` use `AppRoutes`
- [x] `AppRoutesStabilityTest` unit test
- [x] `@Deprecated getAllCustomers()` on repository

## Tier C — Requires explicit approval

- Gradle module `:commandos`
- Firebase Performance re-enable
- Room schema split
