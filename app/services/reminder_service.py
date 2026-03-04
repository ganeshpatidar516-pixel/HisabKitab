class ReminderService:

    @staticmethod
    def format_whatsapp_message(customer_name: str, amount: float, tone: str = "Normal") -> str:
        """
        Generate WhatsApp reminder message based on tone
        """

        templates = {

            "Gentle": f"नमस्ते {customer_name} जी, उम्मीद है सब ठीक होगा। आपके ₹{amount} का हिसाब बाकी है। कृपया समय मिलने पर भुगतान कर दें। 🙏",

            "Normal": f"हेल्लो {customer_name}, आपके खाते में ₹{amount} का बकाया है। कृपया जल्द भुगतान करें।",

            "Strict": f"चेतावनी: {customer_name}, आपका ₹{amount} का भुगतान पेंडिंग है। कृपया आज ही क्लियर करें! ⚠️"

        }

        return templates.get(tone, templates["Normal"])
