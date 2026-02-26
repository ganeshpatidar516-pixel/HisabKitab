import sqlite3
import datetime
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional

app = FastAPI(title="HisabKitab Ultimate AI")

# --- 1. डेटाबेस कनेक्शन (Security First) ---
def get_db_connection():
    try:
        conn = sqlite3.connect('hisabkitab_pro.db', timeout=10)
        conn.row_factory = sqlite3.Row
        return conn
    except Exception as e:
        print(f"❌ Security Alert: DB Connection Failed: {e}")
        return None

# --- 2. डेटाबेस टेबल सेटअप (Auto-Initialize) ---
def init_db():
    conn = get_db_connection()
    if conn:
        # ग्राहकों की टेबल
        conn.execute('''CREATE TABLE IF NOT EXISTS customers 
                        (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                         name TEXT UNIQUE, 
                         risk_score TEXT DEFAULT 'Low')''')
        # लेन-देन की टेबल
        conn.execute('''CREATE TABLE IF NOT EXISTS transactions 
                        (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                         cust_id INTEGER, 
                         item TEXT, 
                         amount REAL, 
                         timestamp DATETIME,
                         FOREIGN KEY(cust_id) REFERENCES customers(id))''')
        conn.commit()
        conn.close()

init_db()

# --- 3. डेटा मॉडल (Data Model) ---
class HisabEntry(BaseModel):
    customer_name: str
    item: str
    quantity: float
    price_per_unit: float

# --- 4. AI रिस्क इंजन (Step 9: Decision Logic) ---
def analyze_risk(total: float):
    if total > 5000:
        return "🔴 High Risk", "Strict"
    if total > 2000:
        return "🟡 Medium Risk", "Normal"
    return "🟢 Low Risk", "Gentle"

# --- 5. मुख्य API एंडपॉइंट (Main Process) ---
@app.post("/process/")
async def process_entry(data: HisabEntry):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection failed")
    
    try:
        cursor = conn.cursor()
        total_amount = data.quantity * data.price_per_unit
        risk_label, tone = analyze_risk(total_amount)
        
        # ग्राहक को चेक करना या जोड़ना
        cursor.execute('INSERT OR IGNORE INTO customers (name) VALUES (?)', (data.customer_name,))
        cursor.execute('SELECT id FROM customers WHERE name = ?', (data.customer_name,))
        result = cursor.fetchone()
        cust_id = result[0]
        
        # लेन-देन दर्ज करना
        cursor.execute('''INSERT INTO transactions (cust_id, item, amount, timestamp) 
                          VALUES (?, ?, ?, ?)''', 
                       (cust_id, data.item, total_amount, datetime.datetime.now()))
        
        conn.commit()

        # प्रोफेशनल बिल बनाना
        bill_msg = (f"📊 *OFFICIAL BILL*\n"
                    f"👤 ग्राहक: {data.customer_name}\n"
                    f"📦 सामान: {data.item}\n"
                    f"💰 कुल राशि: ₹{total_amount}\n"
                    f"⚠️ AI रिस्क: {risk_label}\n"
                    f"💡 सलाह: {tone} तरीके से बात करें।")

        return {
            "success": True,
            "message": "हिसाब ऑफलाइन सेव हो गया",
            "ai_analysis": {"risk": risk_label, "tone": tone},
            "bill": bill_msg,
            "whatsapp_link": f"https://wa.me/?text={bill_msg.replace(' ', '%20')}"
        }
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    finally:
        conn.close()
