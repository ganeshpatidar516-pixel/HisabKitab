from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import sqlite3
import datetime
from typing import Optional

app = FastAPI(title="HisabKitab Ultra Pro Max")

# --- 1. अभेद्य सुरक्षा: Database Shield ---
def get_db_connection():
    try:
        conn = sqlite3.connect('hisabkitab_pro.db', timeout=10)
        conn.row_factory = sqlite3.Row
        return conn
    except Exception as e:
        print(f"🛡️ Security Alert: DB Connection Failed: {e}")
        return None

def init_db():
    conn = get_db_connection()
    if conn:
        # Customers Table
        conn.execute('''CREATE TABLE IF NOT EXISTS customers 
            (id INTEGER PRIMARY KEY AUTOINCREMENT, 
             name TEXT UNIQUE, 
             risk_score TEXT DEFAULT 'Low')''')
        # Transactions Table
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

# --- 2. व्यवस्थित ढांचा: Data Model ---
class HisabEntry(BaseModel):
    customer_name: str
    item: str
    quantity: float
    price_per_unit: float

# --- 3. सुपर कमांडर AI: Risk & Tone Brain ---
def analyze_risk(total: float):
    if total > 5000: return "🔴 High Risk", "Strict"
    if total > 2000: return "🟡 Medium Risk", "Normal"
    return "🟢 Low Risk", "Gentle"

# --- 4. क्रांतिकारी शेयर इंजन (Bill + Reminder) ---
def generate_bill_payload(name, item, total, risk, tone):
    now = datetime.datetime.now().strftime("%d-%m-%Y %H:%M")
    
    # Official Bill
    bill = (
        f"🚩 *OFFICIAL INVOICE: HISAB-KITAB* 🚩\n"
        f"--------------------------\n"
        f"👤 ग्राहक: *{name}*\n"
        f"📦 सामान: {item}\n"
        f"💰 कुल राशि: *₹{total}*\n"
        f"📅 समय: {now}\n"
        f"--------------------------\n"
    )

    # Smart Reminders (Manual/Auto)
    messages = {
        "Gentle": f"नमस्ते {name} जी, आपके ₹{total} का हिसाब दर्ज है। समय मिले तो देख लीजिएगा। 🙏",
        "Normal": f"हेल्लो {name}, आपका ₹{total} का बिल बकाया है। कृपया समय पर भुगतान करें।",
        "Strict": f"⚠️ *अति आवश्यक:* {name}, आपका ₹{total} का भुगतान पेंडिंग है। कृपया इसे तुरंत क्लियर करें।"
    }
    
    reminder = messages.get(tone)
    full_text = f"{bill}\n*रिमाइंडर:*\n{reminder}"
    
    return full_text

# --- 5. मुख्य API Route ---
@app.get("/")
def home():
    return {"status": "Online", "engine": "Ultra Pro Max"}

@app.post("/add_hisaab/")
async def add_hisaab(entry: HisabEntry):
    conn = get_db_connection()
    try:
        # Smart Math
        total = round(entry.quantity * entry.price_per_unit, 2)
        risk, tone = analyze_risk(total)

        cursor = conn.cursor()
        # Atomic Transaction
        cursor.execute('INSERT OR IGNORE INTO customers (name, risk_score) VALUES (?, ?)', (entry.customer_name, risk))
        cursor.execute('SELECT id FROM customers WHERE name = ?', (entry.customer_name,))
        cust_id = cursor.fetchone()[0]
        
        cursor.execute('INSERT INTO transactions (cust_id, item, amount, timestamp) VALUES (?, ?, ?, ?)', 
                       (cust_id, entry.item, total, datetime.datetime.now()))
        conn.commit()

        # Generate Shareable Content
        final_bill = generate_bill_payload(entry.customer_name, entry.item, total, risk, tone)
        
        return {
            "success": True,
            "bill_preview": final_bill,
            "whatsapp_link": f"https://wa.me/?text={final_bill.replace(' ', '%20').replace('\\n', '%0A')}",
            "ai_report": f"Customer: {risk}, Recommended Tone: {tone}"
        }
    except Exception as e:
        if conn: conn.rollback()
        raise HTTPException(status_code=500, detail=f"🛡️ Crash Shield: {str(e)}")
    finally:
        if conn: conn.close()
