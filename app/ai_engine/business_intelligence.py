def calculate_customer_risk(transactions):

    total_credit = 0
    total_paid = 0
    late_payments = 0

    for t in transactions:

        if t["type"] == "credit":
            total_credit += t["amount"]

        if t["type"] == "payment":
            total_paid += t["amount"]

        if t.get("late", False):
            late_payments += 1

    pending = total_credit - total_paid

    risk_score = 0

    if pending > 0:
        risk_score += 30

    if late_payments > 3:
        risk_score += 40

    if pending > 10000:
        risk_score += 30

    if risk_score < 30:
        level = "LOW"

    elif risk_score < 60:
        level = "MEDIUM"

    else:
        level = "HIGH"

    return {
        "risk_score": risk_score,
        "risk_level": level,
        "pending_amount": pending
    }


def recovery_advice(risk_level):

    if risk_level == "LOW":
        return "Normal reminder"

    if risk_level == "MEDIUM":
        return "Send reminder and limit credit"

    if risk_level == "HIGH":
        return "Stop credit and request payment immediately"
