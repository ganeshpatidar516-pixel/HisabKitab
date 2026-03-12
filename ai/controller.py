from ai.router import route_message

def ai_controller(message: str, customers: list):

    routed = route_message(message, customers)

    intent = routed["intent"]
    entities = routed["entities"]

    return {
        "intent": intent,
        "entities": entities
    }
