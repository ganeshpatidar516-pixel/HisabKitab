from fastapi import APIRouter
from app.ai.ai_router import ai_router

router = APIRouter()


@router.post("/ai/chat")
def ai_chat(data: dict):

    message = data.get("message")

    result = ai_router(message)

    return result
