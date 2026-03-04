import re


class AIEntryParser:

    @staticmethod
    def detect_amount(text: str):

        match = re.search(r"\d+", text)

        if match:
            return int(match.group())

        return None


    @staticmethod
    def detect_type(text: str):

        if "उधार" in text or "दे दिया" in text or "दिया" in text:
            return "udhar"

        if "वापस" in text or "मिल गया" in text or "दे दिया वापस" in text:
            return "jama"

        return "unknown"


    @staticmethod
    def detect_name(text: str):

        words = text.split()

        if len(words) == 0:
            return None

        return words[0]


    @staticmethod
    def parse(text: str):

        try:

            text = text.strip()

            amount = AIEntryParser.detect_amount(text)

            if not amount:
                return None

            entry_type = AIEntryParser.detect_type(text)

            name = AIEntryParser.detect_name(text)

            if not name:
                return None

            return {
                "customer_name": name,
                "amount": amount,
                "type": entry_type
            }

        except Exception:
            return None
