from rapidfuzz import process


def find_best_customer_match(name, customer_list):
    """
    Find closest matching customer name
    """

    if not customer_list:
        return None

    match = process.extractOne(name, customer_list)

    if match:
        best_match = match[0]
        score = match[1]

        # minimum similarity threshold
        if score > 70:
            return best_match

    return None
