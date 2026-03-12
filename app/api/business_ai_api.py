from fastapi import APIRouter
from database import get_db_connection
from app.ai.business_advisor_ai import business_advisor

router = APIRouter()


@router.get("/ai/business-insights")
def business_insights(username: str):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        # total outstanding
        cursor.execute("""
        SELECT SUM(amount) as total_credit
        FROM entries
        WHERE username=? AND type='credit'
        """, (username,))

        credit = cursor.fetchone()["total_credit"] or 0

        cursor.execute("""
        SELECT SUM(amount) as total_debit
        FROM entries
        WHERE username=? AND type='debit'
        """, (username,))

        debit = cursor.fetchone()["total_debit"] or 0

        outstanding = credit - debit

        # top customers
        cursor.execute("""
        SELECT c.name, SUM(e.amount) as total
        FROM entries e
        JOIN customers c ON e.customer_id = c.id
        WHERE e.username=? AND e.type='credit'
        GROUP BY e.customer_id
        ORDER BY total DESC
        LIMIT 5
        """, (username,))

        top_customers = cursor.fetchall()

    return {
        "total_credit": credit,
        "total_debit": debit,
        "outstanding_balance": outstanding,
        "top_customers": [
            {
                "name": row["name"],
                "amount": row["total"]
            } for row in top_customers
        ]
    }

@router.get("/ai/business/advisor")
def advisor():
    return business_advisor()
