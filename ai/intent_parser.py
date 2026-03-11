def detect_intent(message: str):

    msg = message.lower()

    # Balance check
    if "बकाया" in msg or "balance" in msg:
        return "balance_check"

    # Add ledger entry
    if "उधार" in msg or "दे दो" in msg:
        return "add_ledger"

    # Sales report
    if "बिक्री" in msg or "sales" in msg:
        return "sales_report"

    # Daily report
    if "रिपोर्ट" in msg or "report" in msg:
        return "daily_report"

    # Reminder
    if "reminder" in msg or "याद दिलाओ" in msg:
        return "send_reminder"

    # Marketing
    if "poster" in msg or "banner" in msg or "video" in msg:
        return "marketing_request"

    # Knowledge
    if "gst" in msg or "tax" in msg or "क्या है" in msg:
        return "knowledge_question"

    return "unknown"
