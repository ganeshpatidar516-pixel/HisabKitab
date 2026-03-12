def detect_intent(message: str):

    msg = message.lower()

    # Balance check
    if "बकाया" in msg or "balance" in msg or "kitna" in msg:
        return "balance_check"

    # Add ledger entry
    if (
        "उधार" in msg
        or "udhar" in msg
        or "de diya" in msg
        or "diya" in msg
        or "de do" in msg
        or "liye" in msg
        or "liya" in msg
    ):
        return "add_ledger"

    # Sales report
    if "बिक्री" in msg or "sales" in msg:
        return "sales_report"

    # Daily report
    if "रिपोर्ट" in msg or "report" in msg:
        return "daily_report"

    # Reminder
    if "reminder" in msg or "याद दिलाओ" in msg or "yaad dilao" in msg:
        return "send_reminder"

    # Marketing
    if "poster" in msg or "banner" in msg or "video" in msg:
        return "marketing_request"

    # Knowledge questions
    if "gst" in msg or "tax" in msg or "क्या है" in msg or "kya hai" in msg:
        return "knowledge_question"

    return "unknown"
