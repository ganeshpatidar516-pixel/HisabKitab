from engines.ledger_engine import handle_ledger
from engines.analytics_engine import handle_analytics
from engines.knowledge_engine import handle_knowledge
from engines.marketing_engine import handle_marketing
from engines.automation_engine import run_automation


def execute_action(intent, entities):

    if intent in ["balance_check", "add_ledger"]:
        return handle_ledger(intent, entities)

    if intent == "sales_report":
        return handle_analytics(intent, entities)

    if intent == "knowledge_question":
        return handle_knowledge(intent, entities)

    if intent == "marketing_request":
        return handle_marketing(intent, entities)

    if intent in ["send_reminder", "daily_report"]:
        return run_automation(intent, entities)

    return {"message": "समझ नहीं आया"}
