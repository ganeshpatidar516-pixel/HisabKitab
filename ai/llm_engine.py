import json


def llm_fallback(message: str):

    """
    Basic free LLM fallback simulation
    """

    message = message.lower()

    if "udhar" in message or "diya" in message:
        return {
            "intent": "add_ledger"
        }

    if "balance" in message or "kitna" in message:
        return {
            "intent": "balance_check"
        }

    if "sales" in message:
        return {
            "intent": "sales_report"
        }

    return {
        "intent": "unknown"
    }
