from fastapi import APIRouter
from pydantic import BaseModel
from database import get_db_connection

router = APIRouter()


class LedgerEntry(BaseModel):
    username: str
    customer_id: int
    type: str
    amount: float
    note: str = ""


@router.post("/ledger/add")
def add_entry(data: LedgerEntry):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT INTO entries (username, customer_id, type, amount, note)
        VALUES (?, ?, ?, ?, ?)
        """, (
            data.username,
            data.customer_id,
            data.type,
            data.amount,
            data.note
        ))

    return {"success": True}


@router.get("/ledger/customer/{customer_id}")
def customer_ledger(customer_id: int):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT * FROM entries
        WHERE customer_id=?
        ORDER BY created_at DESC
        """, (customer_id,))

        rows = cursor.fetchall()

    return [dict(row) for row in rows]


@router.get("/ledger/balance/{customer_id}")
def customer_balance(customer_id: int):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT
        SUM(CASE WHEN type='credit' THEN amount ELSE 0 END) -
        SUM(CASE WHEN type='debit' THEN amount ELSE 0 END)
        as balance
        FROM entries
        WHERE customer_id=?
        """, (customer_id,))

        result = cursor.fetchone()

    return {"balance": result["balance"] or 0}
