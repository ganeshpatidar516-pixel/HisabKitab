from fastapi import APIRouter
from datetime import datetime

router = APIRouter()

@router.get("/")
def health_check():
    return {
        "status": "ok",
        "service": "HisabKitab Pro Backend",
        "version": "v1",
        "timestamp": datetime.utcnow()
    }
