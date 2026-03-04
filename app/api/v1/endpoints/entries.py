from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.database import get_connection
from app.core.response import success_response, error_response
from app.api.v1.endpoints.auth import get_current_user

# AI Learning
from app.aiapi.learning_engine import learn_product


router = APIRouter()


class HisabEntry(BaseModel):
    customer_name: str
    item: str
    quantity: float
    price_per_unit: float


@router.post("/")
def create_entry(
    data: HisabEntry,
    current_user: dict = Depends(get_current_user)
):
    try:

        username = current_user["sub"]

        total = data.quantity * data.price_per_unit

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO entries
            (username, customer_name, item, quantity, price_per_unit, total)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                username,
                data.customer_name,
                data.item,
                data.quantity,
                data.price_per_unit,
                total
            )
        )

        conn.commit()
        conn.close()

        # AI Learning System
        learn_product(username, data.item)

        return success_response(
            message="Entry created successfully",
            data={"total": total}
        )

    except Exception as e:

        return error_response(
            message="Failed to create entry",
            error=str(e)
        )


@router.get("/")
def get_entries(current_user: dict = Depends(get_current_user)):

    try:

        username = current_user["sub"]

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            "SELECT * FROM entries WHERE username=? ORDER BY id DESC",
            (username,)
        )

        rows = cursor.fetchall()

        conn.close()

        data = [dict(row) for row in rows]

        return success_response(
            message="Entries fetched successfully",
            data=data,
            count=len(data)
        )

    except Exception as e:

        return error_response(
            message="Failed to fetch entries",
            error=str(e)
        )


@router.delete("/{entry_id}")
def delete_entry(entry_id: int, current_user: dict = Depends(get_current_user)):

    try:

        username = current_user["sub"]

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            "DELETE FROM entries WHERE id=? AND username=?",
            (entry_id, username)
        )

        conn.commit()

        if cursor.rowcount == 0:
            conn.close()
            return error_response(message="Entry not found")

        conn.close()

        return success_response(message="Entry deleted successfully")

    except Exception as e:

        return error_response(
            message="Failed to delete entry",
            error=str(e)
        )
