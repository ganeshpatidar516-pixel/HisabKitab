import re


def extract_amount(message: str):
    """
    Extract amount from message
    """

    # find numbers like 50, 200, 1000 etc
    match = re.search(r"\d+", message)

    if match:
        return int(match.group())

    return None
