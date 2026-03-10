from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import sqlite3
import re
from app.api.ocr_api import router as ocr_router
from database import init_db
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

# ==============================
# APP INIT
# ==============================

app = FastAPI(title="HisabKitab Pro Ultra Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DB_NAME = "hisabkitab_pro.db"

# ==============================
# DATABASE INIT
# ==============================

init_db()

# ==============================
# ROUTERS
# ==============================
app.include_router(billing_router)
app.include_router(customer_router)
app.include_router(ledger_router)
app.include_router(voice_router)
app.include_router(invoice_router)
app.include_router(ocr_router)
app.include_router(ocr_bill_router)
app.include_router(business_ai_router)
app.include_router(risk_ai_router)
app.include_router(ai_router)
app.include_router(ai_action_router)
app.include_router(ai_voice_router)
app.include_router(ai_learning_router)

# ==============================
# MODELS
# ==============================

class HisabEntry(BaseModel):
    customer_name: str
    item: str
    quantity: float
    price_per_unit: float


class Reminder(BaseModel):
    title: str
    description: str = ""
    time: str


# ==============================
# HEALTH CHECK
# ==============================

@app.get("/health")
def health():
    return {"status": "Backend running perfectly"}


# ==============================
# ENTRIES CRUD
# ==============================

@app.post("/entries")
def create_entry(data: HisabEntry):

    total = data.quantity * data.price_per_unit

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
    INSERT INTO entries (username, customer_id, type, amount, note)
    VALUES (?, ?, ?, ?, ?)
    """, (
        "default",
        1,
        "credit",
        total,
        data.item
    ))

    conn.commit()
    conn.close()

    return {"success": True, "total": total}


@app.get("/entries")
def get_entries():

    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM entries ORDER BY id DESC")
    rows = cursor.fetchall()

    conn.close()

    return [dict(row) for row in rows]


@app.delete("/entries/{entry_id}")
def delete_entry(entry_id: int):

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("DELETE FROM entries WHERE id=?", (entry_id,))
    conn.commit()

    if cursor.rowcount == 0:
        conn.close()
        raise HTTPException(status_code=404, detail="Entry not found")

    conn.close()

    return {"success": True}


# ==============================
# REMINDER SYSTEM
# ==============================

@app.post("/reminders")
def add_reminder(reminder: Reminder):

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute(
        "INSERT INTO reminders (username, message, remind_date) VALUES (?, ?, ?)",
        ("default", reminder.title, reminder.time)
    )

    conn.commit()
    conn.close()

    return {"success": True}


@app.get("/reminders")
def list_reminders():

    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM reminders ORDER BY id DESC")
    rows = cursor.fetchall()

    conn.close()

    return [dict(row) for row in rows]


# ==============================
# AI TEXT ENTRY
# ==============================

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


@app.post("/ai-entry")
def ai_entry(text: str):

    parsed = parse_text(text)

    if not parsed:
        raise HTTPException(status_code=400, detail="AI could not understand text")

    total = parsed["quantity"] * parsed["price_per_unit"]

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
    INSERT INTO entries (username, customer_id, type, amount, note)
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
import os
import uvicorn

port = int(os.environ.get("PORT", 8000))

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=port)
