# Super Command OS - Production Readiness Checklist

This checklist maps your final blueprint into executable go-live gates.
Mark each item as PASS before expanding rollout.

## Current Snapshot (Pre-filled)

- Phase 0: IN PROGRESS
- Phase 1: PASS
- Phase 2: PASS
- Phase 3: PASS
- Phase 4: IN PROGRESS
- Phase 5: IN PROGRESS
- Phase 6: IN PROGRESS
- Phase 7: IN PROGRESS (manual sacred smoke pending)
- Phase 8 governance: `RELEASE_GOVERNANCE.md` (rollback template + gate archive)

## Phase 0 - Governance Lock

- [ ] Sacred modules documented and frozen (`customer_ledger`, ledger math, bill save flow, reminder baseline, settings baseline).  **Status: IN PROGRESS**
- [x] "No direct DB write from parser/orchestrator" rule enforced.  **Status: PASS**
- [x] Feature flag default is OFF for new installs.  **Status: PASS**
- [ ] Rollback owner and emergency contacts defined.  **Status: PENDING**

Verify:

```powershell
rg "AppDatabase|Dao|SQLite" "app/src/main/java/com/ganesh/hisabkitabpro/commandos" -n
```

Expected: no direct DB access in command parser/orchestrator flow.

## Phase 1 - Command Contract Foundation

- [x] Deterministic parser intent coverage for v1 intents.  **Status: PASS**
- [x] Entity extraction validated for customer/amount/setting keys.  **Status: PASS**
- [x] Unknown command safe rejection path verified.  **Status: PASS**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.DeterministicIntentParserTest*"
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.CommandGoldenFixturesTest*"
```

## Phase 2 - Linguistic Core v1

- [x] Normalization handles Hinglish number words (example: `paanch sau -> 500`).  **Status: PASS**
- [x] Dialect mapping pack active for configured locale.  **Status: PASS**
- [x] Golden fixtures include typo/dialect/unknown cases.  **Status: PASS**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.InputNormalizerTest*"
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.DialectRegistryTest*"
```

## Phase 3 - Safety, Guard, Router

- [x] Confidence/risk guard prompts clarification when required.  **Status: PASS**
- [x] High-risk commands require explicit confirmation.  **Status: PASS**
- [x] Financial commands use double-confirm UX path.  **Status: PASS**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.PolicyGuardTest*"
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.CommandSimulationTest*"
```

## Phase 4 - Offline-first + Replay

- [x] Offline commands persist in journal.  **Status: PASS**
- [x] Idempotency key generation deterministic.  **Status: PASS**
- [x] Replay worker scheduled with network constraint and retry.  **Status: PASS**
- [x] Queue health metrics visible in settings.  **Status: PASS**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.PersistentOfflineJournalTest*"
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.ReplayAndConflictTest*"
```

## Phase 5 - Multi-step Transaction Consistency

- [x] Saga planner builds expected sequence.  **Status: PASS**
- [x] Hard atomic failures trigger manual review.  **Status: PASS**
- [x] Financial + notification eventual behavior validated.  **Status: PASS**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.SagaPlannerTest*"
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.SagaExecutorTest*"
```

## Phase 6 - Observability + Self-heal

- [x] Audit events written for success/reject/confirm-required/auto-disable.  **Status: PASS**
- [x] SLO counters available in settings.  **Status: PASS**
- [x] Runtime health report visible and refreshable.  **Status: PASS**
- [x] Canary auto-disable threshold configured and tested.  **Status: PASS**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.CommandSloAndCanaryTest*"
```

## Phase 7 - Release Gates (Mandatory)

- [x] Full commandos suite passes.  **Status: PASS**
- [x] `commandos_quality_gate.ps1` passes.  **Status: PASS**
- [x] CI runs sync unit tests on push (`domain.sync.*`).  **Status: PASS**
- [ ] Manual sacred-flow smoke test passes (customer ledger, bill create/clear, reminder trigger, settings save).  **Status: PENDING**
- [ ] `RELEASE_GOVERNANCE.md` rollback table filled.  **Status: PENDING**
- [ ] Canary rollout first cohort approved.  **Status: PENDING**

Verify:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "com.ganesh.hisabkitabpro.commandos.*"
powershell -ExecutionPolicy Bypass -File "./scripts/commandos_quality_gate.ps1"
```

## Go / No-Go Rule

Go live only if all below are true:

- [ ] No P0/P1 open issue in sacred flows.
- [ ] Command failure rate acceptable for rollout stage.
- [ ] Queue backlog stable and draining.
- [ ] Canary guard active and tested.
- [ ] Rollback path confirmed and rehearsed.

## Ordered Plan (Today -> Go-Live)

1. Run `commandos_quality_gate.ps1` locally and in CI.
2. Execute manual sacred-flow smoke test on real device:
   - customer ledger add/update
   - bill create/clear
   - reminder send flow
   - settings save/reopen validation
3. Record rollout owners + rollback contacts in `RELEASE_GOVERNANCE.md` and release note.
4. Enable feature for internal canary cohort only.
5. Monitor runtime health report + SLO + queue health for 24-48h.
6. If stable, increase cohort gradually with auto-disable guard ON.
7. Approve full rollout only after no P0/P1 and stable failure rate.
