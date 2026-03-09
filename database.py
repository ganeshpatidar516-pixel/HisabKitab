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

    cursor.execute("CREATE INDEX IF NOT EXISTS idx_customer_username ON customers(username)")

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

    cursor.execute("CREATE INDEX IF NOT EXISTS idx_entries_customer ON entries(customer_id)")

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
