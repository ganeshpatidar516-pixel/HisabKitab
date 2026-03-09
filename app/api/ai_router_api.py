from fastapi import APIRouter
from pydantic import BaseModel
import re

router = APIRouter()


class AICommand(BaseModel):
    username: str
    command: str


def detect_intent(command):

    command = command.lower()

    if "udhar" in command or "credit" in command:
        return "ledger_add"

    if "bill" in command or "invoice" in command:
        return "invoice"

    if "reminder" in command or "yaad" in command:
        return "reminder"

    if "analysis" in command or "business" in command:
        return "business_insights"

    if "poster" in command or "marketing" in command:
        return "marketing"

    if "risk" in command:
        return "risk_analysis"

    return "unknown"


@router.post("/ai/router")
def ai_router(data: AICommand):

    intent = detect_intent(data.command)

    return {
        "detected_intent": intent,
        "command": data.command
    }
