def run_automation(intent, entities):

    customer = entities.get("customer")

    # Payment reminder automation
    if intent == "send_reminder":

        if not customer:
            return {
                "status": "error",
                "message": "किस ग्राहक को reminder भेजना है?"
            }

        return {
            "status": "success",
            "automation": "reminder",
            "customer": customer,
            "message": f"{customer} को भुगतान reminder भेजने की प्रक्रिया शुरू की जा सकती है।"
        }

    # Daily business report automation
    if intent == "daily_report":

        return {
            "status": "success",
            "automation": "report",
            "message": "दैनिक व्यापार रिपोर्ट तैयार की जा सकती है।"
        }

    # Default fallback
    return {
        "status": "none",
        "message": "कोई automation action नहीं मिला"
    }
