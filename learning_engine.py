from app.core.database import get_connection


def learn_product(username, product):

    conn = get_connection()
    cursor = conn.cursor()

    cursor.execute(
        "SELECT * FROM ai_products WHERE username=? AND product_name=?",
        (username, product)
    )

    row = cursor.fetchone()

    if row:

        cursor.execute(
            "UPDATE ai_products SET usage_count = usage_count + 1 WHERE id=?",
            (row["id"],)
        )

    else:

        cursor.execute(
            "INSERT INTO ai_products (username, product_name, usage_count) VALUES (?, ?, 1)",
            (username, product)
        )

    conn.commit()
    conn.close()
