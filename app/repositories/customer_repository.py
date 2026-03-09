from database import get_db_connection


def create_customer(username, name, phone=None, address=None):
    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT INTO customers (username, name, phone, address)
        VALUES (?, ?, ?, ?)
        """, (username, name, phone, address))

        return {"status": "success", "message": "Customer added"}


def get_customers(username):
    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT * FROM customers
        WHERE username = ?
        ORDER BY id DESC
        """, (username,))

        rows = cursor.fetchall()

        return [dict(row) for row in rows]


def delete_customer(customer_id):
    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        DELETE FROM customers
        WHERE id = ?
        """, (customer_id,))

        return {"status": "deleted"}
