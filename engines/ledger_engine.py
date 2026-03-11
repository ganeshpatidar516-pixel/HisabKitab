# Simple in-memory ledger store

ledger_data = {}


def handle_ledger(intent, entities):

    customer = entities.get("customer")
    amount = entities.get("amount", 0)

    if not customer:
        return {"message": "ग्राहक का नाम नहीं मिला"}

    if intent == "add_ledger":

        ledger_data[customer] = ledger_data.get(customer, 0) + amount

        return {
            "message": f"{customer} के खाते में ₹{amount} जोड़ दिया गया",
            "balance": ledger_data[customer]
        }

    if intent == "balance_check":

        balance = ledger_data.get(customer, 0)

        return {
            "customer": customer,
            "balance": balance
        }

    return {"message": "ledger action समझ नहीं आया"}
