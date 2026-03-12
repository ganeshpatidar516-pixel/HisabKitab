from fastapi import APIRouter
from app.ai.khata_ai import khata_ai

router = APIRouter()


@router.post("/ai/khata")
def ai_khata(data: dict):

    question = data.get("question")

    result = khata_ai(question)

    return result
