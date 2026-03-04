from app.core.database import get_connection


def learn_product(username, product_name):

    try:

        conn = get_connection()
        cursor = conn.cursor()

        # Check if product already exists
        cursor.execute(
            "SELECT id, usage_count FROM ai_products WHERE username=? AND product_name=?",
            (username, product_name)
        )

        row = cursor.fetchone()

        if row:

            new_count = row["usage_count"] + 1

            cursor.execute(
                "UPDATE ai_products SET usage_count=? WHERE id=?",
                (new_count, row["id"])
            )

        else:

            cursor.execute(
                "INSERT INTO ai_products (username, product_name, usage_count) VALUES (?, ?, ?)",
                (username, product_name, 1)
            )

        conn.commit()
        conn.close()

    except Exception as e:

        print("AI Learning Error:", e)
