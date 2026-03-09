from fastapi import APIRouter, UploadFile, File
import pytesseract
from PIL import Image
import io
import re
from database import get_db_connection

router = APIRouter()


def extract_amount(text: str):
    """
    Detect numbers from OCR text
    """
    numbers = re.findall(r"\d+", text)

    if not numbers:
        return None

    # biggest number usually total
    numbers = [int(n) for n in numbers]

    return max(numbers)


@router.post("/ocr/bill-ledger")
async def scan_bill_and_add_ledger(
    username: str,
    customer_id: int,
    file: UploadFile = File(...)
):

    contents = await file.read()

    image = Image.open(io.BytesIO(contents))
    image = image.convert("L")

    text = pytesseract.image_to_string(image)

    amount = extract_amount(text)

    if not amount:
        return {
            "error": "amount not detected",
            "ocr_text": text
        }

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
        INSERT INTO entries (username, customer_id, type, amount, note)
        VALUES (?, ?, ?, ?, ?)
        """, (
            username,
            customer_id,
            "credit",
            amount,
            "OCR bill entry"
        ))

    return {
        "amount_detected": amount,
        "status": "ledger updated",
        "ocr_text": text
    }
