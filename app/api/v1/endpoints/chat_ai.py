from fastapi import APIRouter, Depends
from pydantic import BaseModel
from app.core.database import get_connection
from app.api.v1.endpoints.auth import get_current_user
from app.core.response import success_response

router = APIRouter()

class Question(BaseModel):
    text: str


@router.post("/ask")
def ask_ai(question: Question, current_user: dict = Depends(get_current_user)):

    username = current_user["sub"]
    text = question.text.lower()

    conn = get_connection()
    cursor = conn.cursor()

    # Find customer name
    cursor.execute(
        "SELECT customer_name, SUM(total) as total_amount FROM entries WHERE username=? GROUP BY customer_name",
        (username,)
    )

    rows = cursor.fetchall()

    conn.close()

    customers = [dict(row) for row in rows]

    for c in customers:

        name = c["customer_name"].lower()

        if name in text:

            return success_response(
                message="AI answer",
                data={
                    "customer": c["customer_name"],
                    "total_pending": c["total_amount"]
                }
            )

    # If user asks top debtor
    if "sabse" in text or "highest" in text:

        if customers:

            top = sorted(customers, key=lambda x: x["total_amount"], reverse=True)[0]

            return success_response(
                message="AI answer",
                data={
                    "top_customer": top["customer_name"],
                    "amount": top["total_amount"]
                }
            )

    return success_response(
        message="AI could not understand the question"
    )
