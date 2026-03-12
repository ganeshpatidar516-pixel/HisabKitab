from fastapi import APIRouter
from pydantic import BaseModel

from ai.router import route_message
from ai.ai_execute import execute_action

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

    # AI action execute
    result = execute_action(intent, entities)

    return {
        "intent": intent,
        "entities": entities,
        "result": result
    }
