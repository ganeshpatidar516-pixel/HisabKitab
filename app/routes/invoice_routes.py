from fastapi import APIRouter
from database import get_db_connection
from app.services.invoice_service import generate_invoice

router = APIRouter()


# ===============================
# CREATE INVOICE
# ===============================

@router.post("/invoice/create")
def create_invoice(data: dict):

    customer = data.get("customer")
    items = data.get("items")
    note = data.get("note", "")
    template = data.get("template", "1")
    apply_gst = data.get("gst", False)

    invoice = generate_invoice(
        customer,
        items,
        note,
        template,
        apply_gst
    )

    with get_db_connection() as conn:

        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO invoices
            (username, invoice_id, customer_name, amount, gst, total, file_path)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                "default",
                invoice["invoice_id"],
                customer,
                0,
                0,
                0,
                invoice["file_path"]
            )
        )

    return invoice


# ===============================
# LIST INVOICES
# ===============================

@router.get("/invoice/list")
def list_invoices():

    with get_db_connection() as conn:

        cursor = conn.cursor()

        cursor.execute(
            "SELECT * FROM invoices ORDER BY id DESC"
        )

        rows = cursor.fetchall()

        return [dict(row) for row in rows]


# ===============================
# DOWNLOAD INVOICE
# ===============================

@router.get("/invoice/download/{invoice_id}")
def download_invoice(invoice_id: str):

    with get_db_connection() as conn:

        cursor = conn.cursor()

        cursor.execute(
            "SELECT file_path FROM invoices WHERE invoice_id=?",
            (invoice_id,)
        )

        row = cursor.fetchone()

        if not row:
            return {"error": "Invoice not found"}

        return {
            "file_path": row["file_path"]
        }
