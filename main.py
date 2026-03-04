from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import sqlite3
import re

# ==============================
# APP INITIALIZATION
# ==============================

app = FastAPI(title="HisabKitab Pro Ultimate Backend")

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

def init_db():
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS entries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customer_name TEXT NOT NULL,
        item TEXT NOT NULL,
        quantity REAL NOT NULL,
        price_per_unit REAL NOT NULL,
        total REAL NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS reminders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        description TEXT,
        time TEXT NOT NULL
    )
    """)

    conn.commit()
    conn.close()

init_db()

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
    INSERT INTO entries (customer_name, item, quantity, price_per_unit, total)
    VALUES (?, ?, ?, ?, ?)
    """, (
        data.customer_name,
        data.item,
        data.quantity,
        data.price_per_unit,
        total
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
        raise HTTPException(status_code=400, detail="AI could not parse text")

    total = parsed["quantity"] * parsed["price_per_unit"]

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
    INSERT INTO entries (customer_name, item, quantity, price_per_unit, total)
    VALUES (?, ?, ?, ?, ?)
    """, (
        parsed["customer_name"],
        parsed["item"],
        parsed["quantity"],
        parsed["price_per_unit"],
        total
    ))

    conn.commit()
    conn.close()

    return {"success": True, "parsed": parsed, "total": total}

# ==============================
# REMINDER SYSTEM
# ==============================

@app.post("/reminders")
def add_reminder(reminder: Reminder):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute(
        "INSERT INTO reminders (title, description, time) VALUES (?, ?, ?)",
        (reminder.title, reminder.description, reminder.time)
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
