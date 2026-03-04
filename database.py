import sqlite3
from contextlib import contextmanager

DATABASE_NAME = "hisabkitab_pro.db"


def init_db():

    conn = sqlite3.connect(DATABASE_NAME)
    cursor = conn.cursor()

    # AI Learning Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS ai_products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT,
        product_name TEXT,
        usage_count INTEGER DEFAULT 1
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
