import re
from typing import Dict, Any


class AIService:

    def __init__(self):
        pass


    def parse_transaction_command(self, text: str) -> Dict[str, Any]:
        """
        Parse natural language transaction command
        Example: 'राम को 500 उधार लिख दो'
        """

        result = {
            "customer": None,
            "amount": None,
            "type": None
        }

        # Amount detection
        amount_match = re.search(r"\d+", text)
        if amount_match:
            result["amount"] = int(amount_match.group())

        # Transaction type detection
        if "उधार" in text or "देना" in text:
            result["type"] = "DEBIT"

        if "वापस" in text or "लेना" in text:
            result["type"] = "CREDIT"

        # Customer name detection (simple approach)
        words = text.split()

        if len(words) > 0:
            result["customer"] = words[0]

        return result
