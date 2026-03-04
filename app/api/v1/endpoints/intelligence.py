from fastapi import APIRouter, Depends
from app.core.database import get_connection
from app.api.v1.endpoints.auth import get_current_user
from app.core.response import success_response

router = APIRouter()

@router.get("/business-report")
def business_report(current_user: dict = Depends(get_current_user)):

    username = current_user["sub"]

    conn = get_connection()
    cursor = conn.cursor()

    cursor.execute(
        "SELECT customer_name, SUM(total) as total_amount FROM entries WHERE username=? GROUP BY customer_name",
        (username,)
    )

    rows = cursor.fetchall()
    conn.close()

    customers = [dict(row) for row in rows]

    high_risk = []
    top_customers = []

    for c in customers:

        if c["total_amount"] > 3000:
            high_risk.append(c)

        top_customers.append(c)

    top_customers = sorted(top_customers, key=lambda x: x["total_amount"], reverse=True)

    return success_response(
        message="AI business report generated",
        data={
            "high_risk_customers": high_risk,
            "top_customers": top_customers[:5]
        }
    )
