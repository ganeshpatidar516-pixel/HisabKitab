from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.services.ai_service import AIEntryParser
from app.core.database import get_connection
from app.core.response import success_response, error_response
from app.api.v1.endpoints.auth import get_current_user

router = APIRouter()


class AIRequest(BaseModel):
    text: str


# -------- AI PARSE --------
@router.post("/parse")
def parse_entry(request: AIRequest):
    try:
        result = AIEntryParser.parse(request.text)

        if not result:
            return error_response(message="Could not understand the entry")

        return success_response(
            message="Entry parsed successfully",
            data=result
        )

    except Exception as e:
        return error_response(
            message="AI parsing failed",
            error=str(e)
        )


# -------- AI AUTO ENTRY --------
@router.post("/auto-entry")
def ai_auto_entry(request: AIRequest, current_user: dict = Depends(get_current_user)):

    try:
        parsed = AIEntryParser.parse(request.text)

        if not parsed:
            return error_response(message="AI could not understand the entry")

        username = current_user["sub"]

        amount = parsed["amount"]
        customer_name = parsed["customer_name"]

        quantity = 1
        price_per_unit = amount
        total = amount

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            "SELECT id FROM users WHERE username=?",
            (username,)
        )

        user = cursor.fetchone()

        if not user:
            conn.close()
            return error_response(message="User not found")

        user_id = user["id"]

        cursor.execute(
            """
            INSERT INTO entries
            (user_id, customer_name, item, quantity, price_per_unit, total)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                user_id,
                customer_name,
                "AI Entry",
                quantity,
                price_per_unit,
                total
            )
        )

        conn.commit()
        conn.close()

        return success_response(
            message="AI entry created successfully",
            data=parsed
        )

    except Exception as e:
        return error_response(
            message="AI auto entry failed",
            error=str(e)
        )


# -------- BUSINESS REPORT --------
@router.get("/business-report")
def ai_business_report(current_user: dict = Depends(get_current_user)):

    try:
        username = current_user["sub"]

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            "SELECT id FROM users WHERE username=?",
            (username,)
        )

        user = cursor.fetchone()

        if not user:
            conn.close()
            return error_response(message="User not found")

        user_id = user["id"]

        cursor.execute(
            """
            SELECT customer_name, SUM(total) as total_amount
            FROM entries
            WHERE user_id=?
            GROUP BY customer_name
            ORDER BY total_amount DESC
            """,
            (user_id,)
        )

        rows = cursor.fetchall()
        conn.close()

        data = [dict(row) for row in rows]

        return success_response(
            message="AI business report generated",
            data=data,
            count=len(data)
        )

    except Exception as e:
        return error_response(
            message="Business AI failed",
            error=str(e)
        )


# -------- RECOVERY ADVICE --------
@router.get("/recovery-advice")
def ai_recovery_advice(current_user: dict = Depends(get_current_user)):

    try:
        username = current_user["sub"]

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            "SELECT id FROM users WHERE username=?",
            (username,)
        )

        user = cursor.fetchone()

        if not user:
            conn.close()
            return error_response(message="User not found")

        user_id = user["id"]

        cursor.execute(
            """
            SELECT customer_name, SUM(total) as total_amount
            FROM entries
            WHERE user_id=?
            GROUP BY customer_name
            ORDER BY total_amount DESC
            """,
            (user_id,)
        )

        rows = cursor.fetchall()
        conn.close()

        advice = []

        for row in rows:
            amount = row["total_amount"]

            if amount >= 1000:
                risk = "HIGH"
            elif amount >= 500:
                risk = "MEDIUM"
            else:
                risk = "LOW"

            advice.append({
                "customer_name": row["customer_name"],
                "pending_amount": amount,
                "risk_level": risk
            })

        return success_response(
            message="AI recovery advice generated",
            data=advice,
            count=len(advice)
        )

    except Exception as e:
        return error_response(
            message="Recovery AI failed",
            error=str(e)
        )
