from ai.router import route_message
from ai.ai_execute import execute_action


def ai_controller(message: str, customers: list):

    # Step 1 — Understand message
    routed = route_message(message, customers)

    intent = routed["intent"]
    entities = routed["entities"]

    # Step 2 — Execute action
    result = execute_action(intent, entities)

    return {
        "command": message,
        "intent": intent,
        "entities": entities,
        "result": result
    }
