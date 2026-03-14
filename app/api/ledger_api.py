from fastapi import APIRouter, Depends
from pydantic import BaseModel
from database import get_db_connection
from app.auth.jwt_handler import get_current_user

router = APIRouter()

# =============================
# DATA MODEL
# =============================

class LedgerEntry(BaseModel):
    customer_id: int
    type: str
    amount: float
    note: str = ""


# =============================
# ADD ENTRY
# =============================

@router.post("/ledger/add")
def add_entry(data: LedgerEntry, user: dict = Depends(get_current_user)):

    username = user["username"]

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT INTO entries (username, customer_id, type, amount, note)
        VALUES (?, ?, ?, ?, ?)
        """, (
            username,
            data.customer_id,
            data.type,
            data.amount,
            data.note
        ))

        conn.commit()

    return {
        "status": "success",
        "message": "Entry added"
    }


# =============================
# CUSTOMER LEDGER
# =============================

@router.get("/ledger/customer/{customer_id}")
def customer_ledger(customer_id: int, user: dict = Depends(get_current_user)):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT * FROM entries
        WHERE customer_id=?
        ORDER BY created_at DESC
        """, (customer_id,))

        rows = cursor.fetchall()

    return [dict(row) for row in rows]


# =============================
# CUSTOMER BALANCE
# =============================

@router.get("/ledger/balance/{customer_id}")
def customer_balance(customer_id: int, user: dict = Depends(get_current_user)):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT
        COALESCE(SUM(CASE WHEN type='credit' THEN amount ELSE 0 END),0) -
        COALESCE(SUM(CASE WHEN type='debit' THEN amount ELSE 0 END),0)
        as balance
        FROM entries
        WHERE customer_id=?
        """, (customer_id,))

        result = cursor.fetchone()

    return {
        "balance": result["balance"]
    }
