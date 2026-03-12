import re


def parse_invoice_text(text: str):

    text = text.strip()

    words = text.split()

    if len(words) < 4:
        return {"error": "Invalid invoice text"}

    customer = words[0]

    qty = None
    price = None
    item = None

    for w in words:

        if w.isdigit():

            if not qty:
                qty = int(w)

            else:
                price = int(w)

    item_words = words[2:-1]

    item = " ".join(item_words)

    return {
        "customer": customer,
        "items": [
            {
                "name": item,
                "qty": qty,
                "price": price
            }
        ]
    }
