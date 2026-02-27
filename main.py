import sqlite3
from fastapi import FastAPI
from pydantic import BaseModel

# ---------------- APP ----------------
app = FastAPI(title="HisabKitab Ultimate AI")

DB_NAME = "hisabkitab_pro.db"

# ---------------- DATABASE INIT ----------------
def init_db():
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS entries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customer_name TEXT,
        item TEXT,
        quantity REAL,
        price_per_unit REAL,
        total REAL
    )
    """)

    conn.commit()
    conn.close()


# app start होते ही DB बन जाए
init_db()

# ---------------- AI RISK SCORING ----------------
def calculate_risk(total_amount: float, quantity: float):
    if total_amount >= 5000 or quantity >= 50:
        return "🔴 High Risk"
    elif total_amount >= 2000:
        return "🟡 Medium Risk"
    else:
        return "🟢 Low Risk"


# ---------------- MODEL ----------------
class HisabEntry(BaseModel):
    customer_name: str
    item: str
    quantity: float
    price_per_unit: float


# ---------------- SAVE FUNCTION ----------------
def save_entry(data: HisabEntry, total_amount: float):
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
        total_amount
    ))

    conn.commit()
    conn.close()


# ---------------- ROOT ----------------
@app.get("/")
def root():
    return {"message": "HisabKitab AI Running 🚀"}


# ---------------- CREATE / PROCESS ----------------
@app.post("/process/")
def process_entry(data: HisabEntry):
    total_amount = data.quantity * data.price_per_unit
    risk_level = calculate_risk(total_amount, data.quantity)

    save_entry(data, total_amount)

    bill_text = (
        f"📊 OFFICIAL BILL\n"
        f"👤 ग्राहक: {data.customer_name}\n"
        f"📦 सामान: {data.item}\n"
        f"💰 कुल राशि: ₹{total_amount}\n"
        f"⚠️ Risk Level: {risk_level}"
    )

    return {
        "success": True,
        "total": total_amount,
        "risk": risk_level,
        "bill": bill_text
    }


# ---------------- READ ALL ----------------
@app.get("/entries/")
def get_all_entries():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM entries ORDER BY id DESC")
    rows = cursor.fetchall()
    conn.close()

    return [dict(row) for row in rows]


# ---------------- UPDATE ENTRY ----------------
@app.put("/entries/{entry_id}")
def update_entry(entry_id: int, data: HisabEntry):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    total_amount = data.quantity * data.price_per_unit

    cursor.execute("""
    UPDATE entries
    SET customer_name=?, item=?, quantity=?, price_per_unit=?, total=?
    WHERE id=?
    """, (
        data.customer_name,
        data.item,
        data.quantity,
        data.price_per_unit,
        total_amount,
        entry_id
    ))

    conn.commit()

    if cursor.rowcount == 0:
        conn.close()
        return {"success": False, "message": "Entry not found"}

    conn.close()

    return {
        "success": True,
        "message": "Entry updated successfully",
        "total": total_amount
    }


# ---------------- DELETE ENTRY ----------------
@app.delete("/entries/{entry_id}")
def delete_entry(entry_id: int):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("DELETE FROM entries WHERE id=?", (entry_id,))
    conn.commit()

    if cursor.rowcount == 0:
        conn.close()
        return {"success": False, "message": "Entry not found"}

    conn.close()

    return {
        "success": True,
        "message": "Entry deleted successfully"
    }
