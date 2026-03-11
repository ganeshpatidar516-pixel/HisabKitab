from ai.router import route_message
from ai.ai_execute import execute_action


def run_ai(message):

    # Step 1: route message
    routed = route_message(message)

    intent = routed["intent"]
    entities = routed["entities"]

    # Step 2: execute action
    result = execute_action(intent, entities)

    return {
        "intent": intent,
        "entities": entities,
        "result": result
    }


if __name__ == "__main__":

    while True:

        user_input = input("AI से पूछें: ")

        output = run_ai(user_input)

        print("\nAI Result:\n", output)
