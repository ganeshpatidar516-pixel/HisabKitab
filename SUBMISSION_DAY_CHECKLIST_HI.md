# HisabKitab - Submission Day Checklist (Hindi, One Page)

यह checklist release वाले दिन step-by-step follow करें।  
Goal: sacred working flows सुरक्षित रहें और Play Store submission बिना risk जाए।

## 1) Start से पहले (5 मिनट)

- [ ] `RELEASE_SCOPE_LOCK.md` देखकर confirm करें: सिर्फ locked scope ही जा रहा है
- [ ] कोई नया risky feature add नहीं हुआ
- [ ] `RELEASE_GOVERNANCE.md` में rollback owner + emergency contact भरा है
- [ ] API cert rotate हुआ हो तो `scripts/update-cert-pins.ps1` चला कर release smoke किया

## 2) One-Click Gate चलाएं

Command चलाएं:

`powershell -ExecutionPolicy Bypass -File ".\scripts\release_validation_gate.ps1"`

Gate log archive (recommended): `RELEASE_GOVERNANCE.md` § Archive gate output

Pass criteria:
- [ ] Unit tests PASS
- [ ] Quality gate PASS
- [ ] Release APK/AAB build PASS
- [ ] Device test stage PASS (या no-device skip message साफ दिखा)

## 3) Real Device Sacred Smoke (Mandatory)

Reference: `MANUAL_SACRED_SMOKE_TEST.md`

- [ ] Customer ledger add/update + reopen PASS
- [ ] Bill create + clear PASS
- [ ] Reminder baseline PASS
- [ ] Settings save/reopen PASS
- [ ] Super Command default OFF isolation PASS

## 4) Play Console Preflight

Reference: `PLAYSTORE_PREFLIGHT_PACK.md`, `PLAYSTORE_RELEASE_CHECKLIST.md`, `PLAY_STORE_DEPLOYMENT_BLUEPRINT.md`

- [ ] AAB सही चुना गया
- [ ] Privacy Policy URL live
- [ ] Data Safety form सही भरा
- [ ] Permissions declaration सही (Camera/Notifications/Contacts आदि)
- [ ] Contact email/support details updated

## 5) Rollout Safety

- [ ] Staged rollout से शुरू (small %)
- [ ] 24-48h monitoring plan ready
- [ ] Crash/ANR watch enabled
- [ ] rollback decision rule clear

## 6) Final Go/No-Go

### GO तभी:
- [ ] ऊपर सभी mandatory steps PASS
- [ ] Sacred flows में कोई P0/P1 issue नहीं

### NO-GO अगर:
- [ ] किसी भी sacred flow में mismatch/crash
- [ ] Quality gate fail
- [ ] Policy/data safety mismatch

---

## 30-Second Team Message (Copy)

"आज submission केवल locked stable scope पर है। One-click gate PASS है, sacred smoke PASS है, और staged rollout से release जाएगा। किसी भी P0/P1 पर तुरंत rollback rule लागू होगा।"
