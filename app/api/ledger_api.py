from fastapi import APIRouter
from pydantic import BaseModel
from database import get_db_connection

router = APIRouter()

# =========================
# DATA MODEL
# =========================

class LedgerEntry(BaseModel):
    customer_id: int
    type: str
    amount: float
    note: str = ""

# =========================
# ADD ENTRY
# =========================

@router.post("/ledger/add")
def add_entry(data: LedgerEntry):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO entries (customer_id, type, amount, note)
            VALUES (?, ?, ?, ?)
            """,
            (
                data.customer_id,
                data.type,
                data.amount,
                data.note
            )
        )

        conn.commit()

    return {
        "status": "success",
        "message": "Entry added"
    }

# =========================
# CUSTOMER LEDGER
# =========================

@router.get("/ledger/customer/{customer_id}")
def customer_ledger(customer_id: int):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT * FROM entries
            WHERE customer_id=?
            ORDER BY created_at DESC
            """,
            (customer_id,)
        )

        rows = cursor.fetchall()

    return [dict(row) for row in rows]

# =========================
# CUSTOMER BALANCE
# =========================

@router.get("/ledger/balance/{customer_id}")
def customer_balance(customer_id: int):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT
            COALESCE(SUM(CASE WHEN type='credit' THEN amount END),0) -
            COALESCE(SUM(CASE WHEN type='debit' THEN amount END),0)
            as balance
            FROM entries
            WHERE customer_id=?
            """,
            (customer_id,)
        )

        result = cursor.fetchone()

    return {
        "balance": result["balance"]
    }
