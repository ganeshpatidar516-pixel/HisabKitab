import sqlite3
from contextlib import contextmanager

DATABASE_NAME = "hisabkitab_pro.db"


def init_db():
    conn = sqlite3.connect(DATABASE_NAME)
    cursor = conn.cursor()

    # ================= USERS =================
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE,
        password TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_users_username
    ON users(username)
    """)

    # ================= CUSTOMERS =================
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

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_customers_username
    ON customers(username)
    """)

    # ================= LEDGER ENTRIES =================
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS entries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        customer_id INTEGER,
        type TEXT,
        amount REAL,
        note TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(customer_id) REFERENCES customers(id)
    )
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_entries_customer
    ON entries(customer_id)
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_entries_username
    ON entries(username)
    """)

    # ================= REMINDERS =================
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS reminders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        customer_id INTEGER,
        message TEXT,
        remind_date TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_reminders_username
    ON reminders(username)
    """)

    # ================= AI LEARNING =================
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS ai_products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        product_name TEXT,
        usage_count INTEGER DEFAULT 1,
        last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_ai_products_username
    ON ai_products(username)
    """)

    # ================= BUSINESS ANALYTICS =================
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS business_insights (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        insight_type TEXT,
        value TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_insights_username
    ON business_insights(username)
    """)

    # ================= BUSINESS SETTINGS =================
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS business_settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        business_name TEXT,
        gst_number TEXT,
        phone TEXT,
        address TEXT,
        logo TEXT,
        default_template TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP
    )
    """)

    cursor.execute("""
    CREATE INDEX IF NOT EXISTS idx_business_settings_username
    ON business_settings(username)
    """)

    conn.commit()
    conn.close()


@contextmanager
def get_db_connection():

    conn = sqlite3.connect(
        DATABASE_NAME,
        check_same_thread=False,
        timeout=10
    )

    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA foreign_keys = ON;")

    conn.row_factory = sqlite3.Row

    try:
        yield conn
        conn.commit()

    except Exception:
        conn.rollback()
        raise

    finally:
        conn.close()
