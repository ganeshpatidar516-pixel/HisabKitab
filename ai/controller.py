from ai.router import route_message
from ai.ai_execute import execute_action


def ai_controller(message: str):

    # Step 1 — understand message
    routed = route_message(message)

    intent = routed["intent"]
    entities = routed["entities"]

    # Step 2 — run action
    result = execute_action(intent, entities)

    return {
        "command": message,
        "intent": intent,
        "entities": entities,
        "result": result
    }
