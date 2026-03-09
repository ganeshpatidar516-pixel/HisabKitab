from fastapi import APIRouter, UploadFile, File
import pytesseract
from PIL import Image
import io

router = APIRouter()


@router.post("/ocr/scan")
async def scan_bill(file: UploadFile = File(...)):

    contents = await file.read()

    image = Image.open(io.BytesIO(contents))

    text = pytesseract.image_to_string(image)

    return {
        "extracted_text": text
    }
