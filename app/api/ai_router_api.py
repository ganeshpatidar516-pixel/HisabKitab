from fastapi import APIRouter
from pydantic import BaseModel

from ai.router import route_message

router = APIRouter()


class AICommand(BaseModel):
    username: str
    command: str
    message: str


@router.post("/ai/router")
def ai_router(data: AICommand):

    # Dummy customer list (later DB से आएगा)
    customers = ["Ramesh", "Suresh", "Mahesh"]

    routed = route_message(data.message, customers)

    intent = routed["intent"]
    entities = routed["entities"]

    return {
        "intent": intent,
        "entities": entities,
        "original_message": data.message
    }
