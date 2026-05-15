# HisabKitab backend

FastAPI backend lives in **`MainApp/`** (separate git repository with its own `origin`).

Security changes for legacy JWT auth: see `MainApp/docs/LEGACY_AUTH_SECURITY.md` and `MainApp/app/api/v1/endpoints/auth.py`.

The Android app (`/app` at repo root) uses **Firebase ID tokens**, not legacy `/auth/login` JWT.
