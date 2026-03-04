from fastapi import APIRouter, UploadFile, File, Depends
import pytesseract
from PIL import Image
import shutil
import re
import difflib

from app.core.database import get_connection
from app.api.v1.endpoints.auth import get_current_user
from app.core.response import success_response, error_response

router = APIRouter()

KNOWN_WORDS = [
    "milk",
    "rice",
    "sugar",
    "oil",
    "salt",
    "tea",
    "coffee",
    "bread",
    "total"
]

def correct_word(word: str):

    word_lower = word.lower()

    matches = difflib.get_close_matches(word_lower, KNOWN_WORDS, n=1, cutoff=0.6)

    if matches:
        return matches[0]

    return word_lower


@router.post("/read-bill")
async def read_bill(
    file: UploadFile = File(...),
    current_user: dict = Depends(get_current_user)
):

    try:

        username = current_user["sub"]

        file_location = f"temp_{file.filename}"

        with open(file_location, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        img = Image.open(file_location)

        text = pytesseract.image_to_string(img)

        lines = text.split("\n")

        items = []
        total = 0

        for line in lines:

            match = re.search(r"([A-Za-z]+)\s+(\d+)", line)

            if match:

                raw_name = match.group(1)
                amount = int(match.group(2))

                name = correct_word(raw_name)

                if name == "total":
                    total = amount
                else:
                    items.append({
                        "name": name,
                        "amount": amount
                    })

        # DATABASE ENTRY CREATE

        conn = get_connection()
        cursor = conn.cursor()

        for item in items:

            cursor.execute(
                """
                INSERT INTO entries
                (username, customer_name, item, quantity, price_per_unit, total)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (
                    username,
                    "OCR Customer",
                    item["name"],
                    1,
                    item["amount"],
                    item["amount"]
                )
            )

        conn.commit()
        conn.close()

        return success_response(
            message="OCR entry created successfully",
            data={
                "items": items,
                "total": total
            }
        )

    except Exception as e:

        return error_response(
            message="OCR processing failed",
            error=str(e)
        )
