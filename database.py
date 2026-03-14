import sqlite3
import os

# =========================
# DATABASE PATH
# =========================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "hisabkitab_pro.db")


# =========================
# DB CONNECTION
# =========================

def get_db_connection():
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


# =========================
# DATABASE INIT
# =========================

def init_database():

    conn = get_db_connection()
    cursor = conn.cursor()

    # =========================
    # BUSINESS SETTINGS TABLE
    # =========================

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS business_settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        business_name TEXT,
        gst_number TEXT,
        phone TEXT,
        address TEXT,
        logo TEXT
    )
    """)

    # =========================
    # CUSTOMERS TABLE
    # =========================

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS customers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        name TEXT,
        phone TEXT,
        address TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # =========================
    # ENTRIES TABLE
    # =========================

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS entries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        customer_id INTEGER,
        type TEXT,
        amount REAL,
        note TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # =========================
    # INVOICES TABLE
    # =========================

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS invoices (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        invoice_id TEXT,
        customer TEXT,
        amount REAL,
        gst REAL,
        total REAL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    # =========================
    # INVOICE ITEMS TABLE
    # =========================

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS invoice_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        invoice_id TEXT,
        item_name TEXT,
        qty INTEGER,
        price REAL
    )
    """)

    conn.commit()
    conn.close()


# =========================
# AUTO INIT DATABASE
# =========================

init_database()
