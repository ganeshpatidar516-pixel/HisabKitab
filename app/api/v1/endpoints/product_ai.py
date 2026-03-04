from fastapi import APIRouter, Depends, Query
from app.core.database import get_connection
from app.api.v1.endpoints.auth import get_current_user
from app.core.response import success_response, error_response

router = APIRouter()


@router.get("/suggest")
def suggest_product(
    q: str = Query(...),
    current_user: dict = Depends(get_current_user)
):

    try:

        username = current_user["sub"]

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            """
            SELECT product_name, usage_count
            FROM ai_products
            WHERE username=? AND product_name LIKE ?
            ORDER BY usage_count DESC
            LIMIT 5
            """,
            (username, f"{q}%")
        )

        rows = cursor.fetchall()

        conn.close()

        suggestions = [row["product_name"] for row in rows]

        return success_response(
            message="Suggestions fetched",
            data=suggestions
        )

    except Exception as e:

        return error_response(
            message="Suggestion failed",
            error=str(e)
        )
