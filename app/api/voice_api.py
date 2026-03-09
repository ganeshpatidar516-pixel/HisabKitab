from fastapi import APIRouter
from pydantic import BaseModel
import re
from database import get_db_connection

router = APIRouter()


class VoiceText(BaseModel):
    username: str
    text: str


def parse_voice_command(text: str):
    """
    Example:
    'ram ko 500 udhar'
    """

    name_match = re.search(r"[a-zA-Z]+", text)
    amount_match = re.search(r"\d+", text)

    if not name_match or not amount_match:
        return None

    customer_name = name_match.group()
    amount = float(amount_match.group())

    return customer_name, amount


@router.post("/voice-entry")
def voice_entry(data: VoiceText):

    parsed = parse_voice_command(data.text)

    if not parsed:
        return {"error": "command not understood"}

    customer_name, amount = parsed

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

        cursor.execute("""
        INSERT INTO entries (username, customer_id, type, amount, note)
        VALUES (?, ?, ?, ?, ?)
        """, (
            data.username,
            customer_id,
            "credit",
            amount,
            "voice entry"
        ))

    return {
        "customer": customer_name,
        "amount": amount,
        "status": "ledger updated via voice"
    }
