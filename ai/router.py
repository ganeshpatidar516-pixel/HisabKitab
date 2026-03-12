from ai.intent_parser import detect_intent
from ai.entity_extractor import extract_entities
from ai.fuzzy_match import find_best_customer_match
from ai.amount_extractor import extract_amount
from ai.llm_engine import llm_fallback


def route_message(message, customers):

    intent = detect_intent(message)

    # If intent unknown → use LLM fallback
    if intent == "unknown":
        llm_result = llm_fallback(message)
        intent = llm_result["intent"]

    entities = extract_entities(message)

    # Extract amount if missing
    if not entities.get("amount"):
        amount = extract_amount(message)
        if amount:
            entities["amount"] = amount

    # Fuzzy customer match
    if entities.get("customer"):
        best_match = find_best_customer_match(
            entities["customer"], customers
        )
        if best_match:
            entities["customer"] = best_match

    return {
        "intent": intent,
        "entities": entities
    }
