from fastapi import APIRouter

from app.api.v1.endpoints import health
from app.api.v1.endpoints import entries
from app.api.v1.endpoints import reminders
from app.api.v1.endpoints import ai
from app.api.v1.endpoints import auth
from app.api.v1.endpoints import intelligence
from app.api.v1.endpoints import chat_ai
from app.api.v1.endpoints import voice_ai
from app.api.v1.endpoints import product_ai

from app.api.v1 import ocr


api_router = APIRouter()


# Health API
api_router.include_router(health.router, prefix="/health", tags=["Health"])

# Auth API
api_router.include_router(auth.router, prefix="/auth", tags=["Auth"])

# Entries API
api_router.include_router(entries.router, prefix="/entries", tags=["Entries"])

# Reminders API
api_router.include_router(reminders.router, prefix="/reminders", tags=["Reminders"])

# Basic AI
api_router.include_router(ai.router, prefix="/ai", tags=["AI"])

# OCR API
api_router.include_router(ocr.router, prefix="/ocr", tags=["OCR"])

# Business Intelligence
api_router.include_router(intelligence.router, prefix="/intelligence", tags=["AI Intelligence"])

# AI Chat
api_router.include_router(chat_ai.router, prefix="/chat-ai", tags=["AI Chat"])

# Voice AI
api_router.include_router(voice_ai.router, prefix="/voice-ai", tags=["Voice AI"])

# Product Suggestion AI
api_router.include_router(product_ai.router, prefix="/products", tags=["AI Products"])
