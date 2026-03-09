from fastapi import APIRouter, UploadFile, File
import speech_recognition as sr
import tempfile
import shutil
import requests

router = APIRouter()


@router.post("/ai/voice-command")
async def voice_command(file: UploadFile = File(...)):

    # save audio temporarily
    with tempfile.NamedTemporaryFile(delete=False) as tmp:
        shutil.copyfileobj(file.file, tmp)
        temp_audio = tmp.name

    recognizer = sr.Recognizer()

    with sr.AudioFile(temp_audio) as source:
        audio = recognizer.record(source)

    try:
        text = recognizer.recognize_google(audio)
    except:
        return {"error": "voice not understood"}

    # call AI command engine
    response = requests.post(
        "http://127.0.0.1:8000/ai/execute",
        json={
            "username": "test",
            "command": text
        }
    )

    return {
        "voice_text": text,
        "ai_result": response.json()
    }
