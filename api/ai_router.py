from fastapi import APIRouter
from pydantic import BaseModel
from ai.controller import ai_controller

router = APIRouter()

class AIRequest(BaseModel):
    message: str

@router.post("/ai/command")
def ai_command(req: AIRequest):
    result = ai_controller(req.message)
    return result
