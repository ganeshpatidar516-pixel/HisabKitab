from fastapi import APIRouter
from database import get_db_connection

router = APIRouter()


@router.get("/ai/learning/update")
def update_learning(username: str):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        # customer credit/debit behaviour
        cursor.execute("""
        SELECT c.name,
               SUM(CASE WHEN e.type='credit' THEN e.amount ELSE 0 END) AS credit_total,
               SUM(CASE WHEN e.type='debit' THEN e.amount ELSE 0 END) AS debit_total
        FROM customers c
        LEFT JOIN entries e ON c.id = e.customer_id
        WHERE c.username=?
        GROUP BY c.id
        """, (username,))

        rows = cursor.fetchall()

    learning_data = []

    for row in rows:

        credit = row["credit_total"] or 0
        debit = row["debit_total"] or 0

        balance = credit - debit

        if balance > 5000:
            behaviour = "high_risk"
        elif balance > 1000:
            behaviour = "medium_risk"
        else:
            behaviour = "good_customer"

        learning_data.append({
            "customer": row["name"],
            "credit": credit,
            "debit": debit,
            "balance": balance,
            "ai_tag": behaviour
        })

    return {
        "learning_analysis": learning_data
    }
