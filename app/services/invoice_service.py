from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from datetime import datetime
import os
import qrcode

INVOICE_DIR = "invoices"

BUSINESS_NAME = "HisabKitab Store"
GST_NUMBER = "22AAAAA0000A1Z5"
UPI_ID = "ganesh@upi"


def generate_invoice(customer_name: str, amount: float, note: str = ""):

    if not os.path.exists(INVOICE_DIR):
        os.makedirs(INVOICE_DIR)

    invoice_id = f"INV-{int(datetime.now().timestamp())}"
    file_path = f"{INVOICE_DIR}/{invoice_id}.pdf"

    # QR code
    qr_data = f"upi://pay?pa={UPI_ID}&pn=HisabKitab&am={amount}"
    qr = qrcode.make(qr_data)

    qr_path = f"{INVOICE_DIR}/{invoice_id}_qr.png"
    qr.save(qr_path)

    c = canvas.Canvas(file_path, pagesize=A4)

    # Header
    c.setFont("Helvetica-Bold", 18)
    c.drawString(50, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 11)
    c.drawString(50, 780, f"GSTIN: {GST_NUMBER}")

    # Invoice info
    c.drawString(50, 740, f"Invoice No: {invoice_id}")
    c.drawString(50, 720, f"Date: {datetime.now().strftime('%Y-%m-%d')}")
    c.drawString(50, 700, f"Customer: {customer_name}")

    # Item section
    c.drawString(50, 660, f"Note: {note}")

    # Total
    c.setFont("Helvetica-Bold", 14)
    c.drawString(50, 620, f"Total Amount: ₹{amount}")

    # QR
    c.drawImage(qr_path, 400, 650, width=120, height=120)

    c.save()

    return {
        "invoice_id": invoice_id,
        "file_path": file_path
    }
