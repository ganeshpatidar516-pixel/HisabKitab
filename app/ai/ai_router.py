from app.ai.khata_ai import khata_ai
from app.ai.invoice_ai import parse_invoice_text


def ai_router(message: str):

    text = message.lower()

    # ===============================
    # KHATA QUESTIONS
    # ===============================

    if "हिसाब" in text or "balance" in text:

        return {
            "type": "khata",
            "result": khata_ai(message)
        }

    # ===============================
    # SALES QUESTIONS
    # ===============================

    if "बिक्री" in text or "sale" in text:

        return {
            "type": "analytics",
            "result": "Sales analytics module coming soon"
        }

    # ===============================
    # INVOICE COMMAND
    # ===============================

    parsed = parse_invoice_text(message)

    if "error" not in parsed:

        return {
            "type": "invoice",
            "result": parsed
        }

    # ===============================
    # DEFAULT
    # ===============================

    return {
        "type": "unknown",
        "result": "समझ नहीं आया"
    }
