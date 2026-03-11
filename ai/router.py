from ai.intent_parser import detect_intent
from ai.entity_extractor import extract_entities


def route_message(message: str):

    intent = detect_intent(message)
    entities = extract_entities(message)

    response = {
        "intent": intent,
        "entities": entities
    }

    return response
