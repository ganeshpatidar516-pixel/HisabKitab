from database import get_db_connection


def business_advisor():

    with get_db_connection() as conn:

        cursor = conn.cursor()

        # total sales
        cursor.execute("SELECT SUM(total) as sales FROM invoices")
        sales = cursor.fetchone()["sales"]

        if not sales:
            sales = 0

        # risky customer
        cursor.execute("""
        SELECT c.name, SUM(e.amount) as balance
        FROM entries e
        JOIN customers c ON e.customer_id=c.id
        GROUP BY c.name
        ORDER BY balance DESC
        LIMIT 1
        """)

        risky = cursor.fetchone()

        if risky:
            risky_text = f"सबसे ज्यादा उधार {risky['name']} (₹{risky['balance']})"
        else:
            risky_text = "कोई उधार नहीं"

        return {
            "sales": sales,
            "risk_customer": risky_text,
            "advice": "उधार कम रखें और नियमित भुगतान लें"
        }
