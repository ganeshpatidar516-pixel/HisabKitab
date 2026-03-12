from fastapi import APIRouter
from app.services.invoice_service import generate_invoice

router = APIRouter()


@router.post("/invoice/create")
def create_invoice(data: dict):

    customer = data.get("customer")
    items = data.get("items")
    note = data.get("note", "")
    template = data.get("template", "1")
    apply_gst = data.get("apply_gst", False)

    result = generate_invoice(
        customer_name=customer,
        items=items,
        note=note,
        template=template,
        apply_gst=apply_gst
    )

    return result
