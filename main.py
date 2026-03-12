from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import sqlite3
import os
import uvicorn
import re

# DATABASE INIT
from database import init_db

# ROUTERS
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
from app.routes.invoice_routes import router as invoice_router
from app.routes.ai_invoice_routes import router as ai_invoice_router

# NEW AI COMMAND ROUTER
from api.ai_router import router as ai_command_router


app = FastAPI(title="HisabKitab Pro Ultra Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DB_NAME = "hisabkitab_pro.db"

# DATABASE INIT
init_db()


# =============================
# ROUTERS
# =============================

app.include_router(billing_router)
app.include_router(customer_router)
app.include_router(ledger_router)
app.include_router(voice_router)
app.include_router(invoice_router)
app.include_router(ocr_bill_router)
app.include_router(business_ai_router)
app.include_router(risk_ai_router)
app.include_router(ai_router)
app.include_router(ai_action_router)
app.include_router(ai_voice_router)
app.include_router(ai_learning_router)
app.include_router(business_settings.router)
app.include_router(invoice_api.router)
app.include_router(invoice_router)
app.include_router(ai_invoice_router)

# NEW AI COMMAND ROUTER
app.include_router(ai_command_router)


# =============================
# MODELS
# =============================

class HisabEntry(BaseModel):
    customer_name: str
    item: str
    quantity: float
    price_per_unit: float


class Reminder(BaseModel):
    title: str
    description: str = ""
    time: str


# =============================
# HEALTH CHECK
# =============================

@app.get("/health")
def health():
    return {"status": "Backend running perfectly"}


# =============================
# SIMPLE AI TEXT PARSER
# =============================

def parse_text(text: str):

    name_match = re.search(r"([A-Za-z]+)", text)
    qty_match = re.search(r"(\d+)", text)
    price_match = re.search(r"₹?(\d+)", text)

    if not (name_match and qty_match and price_match):
        return None

    return {
        "customer_name": name_match.group(1),
        "item": "AI_Item",
        "quantity": float(qty_match.group(1)),
        "price_per_unit": float(price_match.group(1))
    }


# =============================
# AI ENTRY
# =============================

@app.post("/ai-entry")
def ai_entry(text: str):

    parsed = parse_text(text)

    if not parsed:
        raise HTTPException(status_code=400, detail="AI could not understand")

    total = parsed["quantity"] * parsed["price_per_unit"]

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
        INSERT INTO entries (username, customer_id, type, amount, item)
        VALUES (?, ?, ?, ?, ?)
    """, (
        "default",
        1,
        "credit",
        total,
        parsed["item"]
    ))

    conn.commit()
    conn.close()

    return {
        "success": True,
        "parsed": parsed,
        "total": total
    }


# =============================
# SERVER START
# =============================

port = int(os.environ.get("PORT", 8000))

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=port)
