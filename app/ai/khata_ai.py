from database import get_db_connection


def khata_ai(question: str):

    q = question.lower()

    # ===============================
    # TOTAL SALES
    # ===============================

    if "कुल बिक्री" in q or "total sale" in q:

        with get_db_connection() as conn:

            cursor = conn.cursor()

            cursor.execute(
                "SELECT SUM(total) as total_sales FROM invoices"
            )

            row = cursor.fetchone()

            return {
                "answer": f"कुल बिक्री ₹{row['total_sales']}"
            }

    # ===============================
    # CUSTOMER BALANCE
    # ===============================

    if "हिसाब" in q:

        words = q.split()

        if len(words) > 0:

            customer = words[0]

            with get_db_connection() as conn:

                cursor = conn.cursor()

                cursor.execute(
                    """
                    SELECT SUM(amount) as total
                    FROM entries
                    WHERE customer_id IN (
                        SELECT id FROM customers
                        WHERE name LIKE ?
                    )
                    """,
                    (f"%{customer}%",)
                )

                row = cursor.fetchone()

                return {
                    "answer": f"{customer} का कुल हिसाब ₹{row['total']}"
                }

    # ===============================
    # DEFAULT RESPONSE
    # ===============================

    return {
        "answer": "समझ नहीं आया, कृपया दोबारा पूछें"
    }
