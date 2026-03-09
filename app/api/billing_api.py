from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
from database import get_db_connection

router = APIRouter()


class Item(BaseModel):
    name: str
    qty: int
    price: float


class BillRequest(BaseModel):
    username: str
    customer_id: int
    items: List[Item]
    gst_enabled: bool = False
    gst_rate: float = 18
    discount: float = 0
    extra_charges: float = 0


@router.post("/bill/create")
def create_bill(data: BillRequest):

    subtotal = 0

    for item in data.items:
        subtotal += item.qty * item.price

    subtotal -= data.discount
    subtotal += data.extra_charges

    gst_amount = 0

    if data.gst_enabled:
        gst_amount = subtotal * data.gst_rate / 100

    total = subtotal + gst_amount

    # Save in ledger
    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT INTO entries (username, customer_id, type, amount, note)
        VALUES (?, ?, ?, ?, ?)
        """, (
            data.username,
            data.customer_id,
            "credit",
            total,
            "Bill generated"
        ))

    return {
        "subtotal": subtotal,
        "gst": gst_amount,
        "total": total,
        "status": "bill added to ledger"
    }
