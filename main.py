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

import sqlite3

# डेटाबेस कनेक्शन फंक्शन (Crash-proof logic)
def get_db_connection():
    try:
        conn = sqlite3.connect('hisabkitab.db', check_same_thread=False)
        conn.row_factory = sqlite3.Row
        return conn
    except Exception as e:
        print(f"Database Error: {e}")
        return None

# टेबल बनाना (अगर नहीं है तो)
conn = get_db_connection()
if conn:
    conn.execute('''CREATE TABLE IF NOT EXISTS users 
                    (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                     username TEXT UNIQUE, 
                     password TEXT)''')
    conn.execute('''CREATE TABLE IF NOT EXISTS transactions 
                    (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                     item TEXT, 
                     amount REAL, 
                     user_id INTEGER)''')
    conn.commit()
    conn.close()

print("✅ डेटाबेस टेबल सुरक्षित रूप से तैयार हैं।")

import sqlite3

# डेटाबेस कनेक्शन फंक्शन (Crash-proof logic)
def get_db_connection():
    try:
        conn = sqlite3.connect('hisabkitab.db', check_same_thread=False)
        conn.row_factory = sqlite3.Row
        return conn
    except Exception as e:
        print(f"Database Error: {e}")
        return None

# टेबल बनाना (अगर नहीं है तो)
conn = get_db_connection()
if conn:
    conn.execute('''CREATE TABLE IF NOT EXISTS users 
                    (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                     username TEXT UNIQUE, 
                     password TEXT)''')
    conn.execute('''CREATE TABLE IF NOT EXISTS transactions 
                    (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                     item TEXT, 
                     amount REAL, 
                     user_id INTEGER)''')
    conn.commit()
    conn.close()

print("✅ डेटाबेस टेबल सुरक्षित रूप से तैयार हैं।")

from datetime import datetime, timedelta
from jose import jwt

# टोकन की सेटिंग्स (1 दिन के लिए मान्य)
ACCESS_TOKEN_EXPIRE_MINUTES = 1440 

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, "GANESH_ULTRA_SECURE_2026", algorithm="HS256")
    return encoded_jwt

@app.post("/login")
def login(username: str, password: str):
    conn = get_db_connection()
    user = conn.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()
    conn.close()
    
    if user and verify_password(password, user["password"]):
        token = create_access_token(data={"sub": username})
        return {"status": "success", "access_token": token, "token_type": "bearer"}
    else:
        return {"status": "error", "message": "गलत यूजरनेम या पासवर्ड"}

print("✅ लॉगिन और टोकन सिस्टम एक्टिवेट हो गया है।")

# हिसाब-किताब जोड़ने का सुरक्षित API
@app.post("/add_transaction")
def add_transaction(item: str, amount: float, category: str, token: str):
    try:
        # टोकन चेक करना (Security)
        payload = jwt.decode(token, "GANESH_ULTRA_SECURE_2026", algorithms=["HS256"])
        username = payload.get("sub")
        
        conn = get_db_connection()
        user = conn.execute("SELECT id FROM users WHERE username = ?", (username,)).fetchone()
        
        if user:
            conn.execute("INSERT INTO transactions (item, amount, category, user_id) VALUES (?, ?, ?, ?)", 
                         (item, amount, category, user["id"]))
            conn.commit()
            conn.close()
            return {"status": "success", "message": f"'{item}' का हिसाब सुरक्षित सेव हो गया है।"}
        else:
            return {"status": "error", "message": "यूजर नहीं मिला।"}
    except Exception as e:
        return {"status": "error", "message": f"Security Error: {str(e)}"}

# सारा हिसाब देखने का API
@app.get("/get_history")
def get_history(token: str):
    try:
        payload = jwt.decode(token, "GANESH_ULTRA_SECURE_2026", algorithms=["HS256"])
        username = payload.get("sub")
        
        conn = get_db_connection()
        user = conn.execute("SELECT id FROM users WHERE username = ?", (username,)).fetchone()
        rows = conn.execute("SELECT item, amount, category FROM transactions WHERE user_id = ?", (user["id"],)).fetchall()
        conn.close()
        
        return {"status": "success", "history": [dict(row) for row in rows]}
    except:
        return {"status": "error", "message": "अवैध टोकन या लॉगिन की जरूरत है।"}

print("✅ ट्रांजैक्शन और हिस्ट्री सिस्टम सक्रिय (Active) हो गया है।")
