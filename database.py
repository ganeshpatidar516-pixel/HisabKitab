import sqlite3

DB_NAME = "hisabkitab.db"

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


def save_entry(customer_name, item, quantity, price_per_unit, total):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    cursor.execute("""
    INSERT INTO entries (customer_name, item, quantity, price_per_unit, total)
    VALUES (?, ?, ?, ?, ?)
    """, (customer_name, item, quantity, price_per_unit, total))

    conn.commit()
    conn.close()
