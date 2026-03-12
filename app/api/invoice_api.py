from fastapi import APIRouter
from pydantic import BaseModel
from fastapi.responses import FileResponse
import os

from app.services.invoice_service import generate_invoice

router = APIRouter()


class InvoiceRequest(BaseModel):
    customer_name: str
    amount: float
    note: str = ""
    template: str = "1"      # template selection
    apply_gst: bool = False  # GST optional


@router.post("/invoice/create")
def create_invoice(data: InvoiceRequest):

    invoice = generate_invoice(
        customer_name=data.customer_name,
        amount=data.amount,
        note=data.note,
        template=data.template,
        apply_gst=data.apply_gst
    )

    return {
        "status": "success",
        "invoice_id": invoice["invoice_id"],
        "file_path": invoice["file_path"]
    }


@router.get("/invoice/download/{invoice_id}")
def download_invoice(invoice_id: str):

    file_path = f"invoices/{invoice_id}.pdf"

    if not os.path.exists(file_path):
        return {"error": "Invoice not found"}

    return FileResponse(
        path=file_path,
        filename=f"{invoice_id}.pdf",
        media_type="application/pdf"
    )	

