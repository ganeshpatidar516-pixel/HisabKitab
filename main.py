from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import sqlite3
import os
import uvicorn
import re

# =============================
# APP INIT
# =============================

app = FastAPI(title="HisabKitab Pro Ultra Backend")

# =============================
# STATIC FILES (INVOICES)
# =============================

app.mount("/invoices", StaticFiles(directory="invoices"), name="invoices")

# =============================
# DATABASE INIT
# =============================

from database import init_db

# =============================
# ROUTERS
# =============================

from app.api.customer_api import router as customer_router
from app.api.ledger_api import router as ledger_router
from app.api.invoice_api import router as invoice_router
from app.api.billing_api import router as billing_router
from app.api.voice_api import router as voice_router
from app.api.ocr_bill_api import router as ocr_bill_router
from app.api.business_ai_api import router as business_ai_router
from app.api.risk_ai_api import router as risk_ai_router
from app.api.ai_router_api import router as ai_router
from app.api.ai_action_api import router as ai_action_router
from app.api.ai_voice_control_api import router as ai_voice_router
from app.api.ai_learning_api import router as ai_learning_router

from app.routes import business_settings
from app.routes import invoice_api
from app.routes.invoice_routes import router as invoice_routes
from app.routes.ai_invoice_routes import router as ai_invoice_router
from app.routes.ai_khata_routes import router as ai_khata_router

# =============================
# INCLUDE ROUTERS
# =============================

app.include_router(customer_router)
app.include_router(ledger_router)
app.include_router(invoice_router)
app.include_router(billing_router)
app.include_router(voice_router)
app.include_router(ocr_bill_router)
app.include_router(business_ai_router)
app.include_router(risk_ai_router)
app.include_router(ai_router)
app.include_router(ai_action_router)
app.include_router(ai_voice_router)
app.include_router(ai_learning_router)

app.include_router(invoice_routes)
app.include_router(ai_invoice_router)
app.include_router(ai_khata_router)

# =============================
# CORS
# =============================

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =============================
# STARTUP EVENT
# =============================

@app.on_event("startup")
def startup():
    init_db()

# =============================
# ROOT TEST
# =============================

@app.get("/")
def root():
    return {"message": "HisabKitab Pro Backend Running 🚀"}

# =============================
# MAIN
# =============================

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
