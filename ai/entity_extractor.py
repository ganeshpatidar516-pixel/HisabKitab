import re

def extract_entities(message: str):

    entities = {}

    # Detect amount
    amount_match = re.search(r'\d+', message)
    if amount_match:
        entities["amount"] = int(amount_match.group())

    # Split words
    words = message.split()

    # Hindi stop words
    stop_words = ["को", "दे", "दो", "का", "है", "में", "से"]

    for word in words:

        # Skip numbers
        if word.isdigit():
            continue

        # Skip stop words
        if word in stop_words:
            continue

        # First meaningful word = customer name
        entities["customer"] = word
        break

    return entities
