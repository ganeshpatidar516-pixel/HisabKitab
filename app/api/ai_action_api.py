from fastapi import APIRouter
from pydantic import BaseModel
import re
from database import get_db_connection

router = APIRouter()


class AIAction(BaseModel):
    username: str
    command: str


def detect_amount(text):
    match = re.search(r"\d+", text)
    if match:
        return int(match.group())
    return None


def detect_customer(text):
    match = re.search(r"[a-zA-Z]+", text)
    if match:
        return match.group().lower()
    return None


@router.post("/ai/execute")
def ai_execute(data: AIAction):

    command = data.command.lower()

    amount = detect_amount(command)
    customer_name = detect_customer(command)

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT id FROM customers
        WHERE name=? AND username=?
        """, (customer_name, data.username))

        customer = cursor.fetchone()

        if not customer:
            return {"error": "customer not found"}

        customer_id = customer["id"]

        if "udhar" in command or "credit" in command:

            cursor.execute("""
            INSERT INTO entries (username, customer_id, type, amount, note)
            VALUES (?, ?, ?, ?, ?)
            """, (
                data.username,
                customer_id,
                "credit",
                amount,
                "AI auto action"
            ))

            return {
                "status": "ledger updated",
                "customer": customer_name,
                "amount": amount
            }

    return {"message": "command processed"}
