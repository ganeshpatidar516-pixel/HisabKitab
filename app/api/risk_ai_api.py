from fastapi import APIRouter
from database import get_db_connection

router = APIRouter()


@router.get("/ai/risk-customers")
def risk_customers(username: str):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT c.name,
               SUM(CASE WHEN e.type='credit' THEN e.amount ELSE 0 END) -
               SUM(CASE WHEN e.type='debit' THEN e.amount ELSE 0 END) AS balance
        FROM customers c
        LEFT JOIN entries e ON c.id = e.customer_id
        WHERE c.username = ?
        GROUP BY c.id
        """, (username,))

        rows = cursor.fetchall()

    results = []

    for row in rows:
        balance = row["balance"] or 0

        if balance > 5000:
            risk = "HIGH"
        elif balance > 1000:
            risk = "MEDIUM"
        else:
            risk = "LOW"

        results.append({
            "customer": row["name"],
            "pending_amount": balance,
            "risk_level": risk
        })

    return {
        "customers_risk_analysis": results
    }
