from fastapi import APIRouter, UploadFile, File, Depends
import speech_recognition as sr
from pydub import AudioSegment
import re

from app.core.database import get_connection
from app.api.v1.endpoints.auth import get_current_user
from app.core.response import success_response, error_response

router = APIRouter()


@router.post("/voice-entry")
async def voice_entry(
    file: UploadFile = File(...),
    current_user: dict = Depends(get_current_user)
):

    try:

        username = current_user["sub"]

        file_location = f"temp_{file.filename}"

        with open(file_location, "wb") as f:
            f.write(await file.read())

        recognizer = sr.Recognizer()

        audio = AudioSegment.from_file(file_location)
        audio.export("temp.wav", format="wav")

        with sr.AudioFile("temp.wav") as source:
            audio_data = recognizer.record(source)

        text = recognizer.recognize_google(audio_data).lower()

        # Example voice command: ram 2 kilo sugar 80
        match = re.search(r"(\w+)\s+(\d+)\s+\w+\s+(\w+)\s+(\d+)", text)

        if not match:
            return error_response(message="Could not understand voice command")

        customer = match.group(1)
        quantity = float(match.group(2))
        item = match.group(3)
        price = float(match.group(4))

        total = quantity * price

        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO entries
            (username, customer_name, item, quantity, price_per_unit, total)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                username,
                customer,
                item,
                quantity,
                price,
                total
            )
        )

        conn.commit()
        conn.close()

        return success_response(
            message="Voice entry created",
            data={
                "voice_text": text,
                "customer": customer,
                "item": item,
                "quantity": quantity,
                "price": price,
                "total": total
            }
        )

    except Exception as e:

        return error_response(
            message="Voice processing failed",
            error=str(e)
        )
