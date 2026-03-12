from fastapi import APIRouter
from app.ai.invoice_ai import parse_invoice_text
from app.services.invoice_service import generate_invoice

router = APIRouter()


@router.post("/ai/invoice")
def ai_invoice(data: dict):

    text = data.get("text")

    parsed = parse_invoice_text(text)

    if "error" in parsed:
        return parsed

    invoice = generate_invoice(
        parsed["customer"],
        parsed["items"],
        "",
        "1",
        False
    )

    return {
        "parsed": parsed,
        "invoice": invoice
    }
