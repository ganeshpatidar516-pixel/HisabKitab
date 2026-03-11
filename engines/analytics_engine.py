from engines.ledger_engine import ledger_data


def handle_analytics(intent, entities):

    if intent == "sales_report":

        total_customers = len(ledger_data)
        total_balance = sum(ledger_data.values())

        return {
            "total_customers": total_customers,
            "total_balance": total_balance,
            "message": "यह वर्तमान उधार स्थिति है"
        }

    return {"message": "analytics समझ नहीं आया"}
