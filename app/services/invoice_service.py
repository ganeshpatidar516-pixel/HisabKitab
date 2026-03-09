from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from datetime import datetime
import os
import qrcode

INVOICE_DIR = "invoices"

BUSINESS_NAME = "HisabKitab Store"
GST_NUMBER = "22AAAAA0000A1Z5"
UPI_ID = "ganesh@upi"


def generate_invoice(customer_name: str, items: list, gst_enabled: bool = True,
                     gst_rate: float = 18, discount: float = 0,
                     extra_charges: float = 0, payment_status: str = "UNPAID"):

    if not os.path.exists(INVOICE_DIR):
        os.makedirs(INVOICE_DIR)

    invoice_id = f"INV-{int(datetime.now().timestamp())}"
    file_path = f"{INVOICE_DIR}/{invoice_id}.pdf"

    subtotal = 0

    for item in items:
        subtotal += item["qty"] * item["price"]

    subtotal -= discount
    subtotal += extra_charges

    gst_amount = 0
    cgst = 0
    sgst = 0

    if gst_enabled:
        gst_amount = subtotal * gst_rate / 100
        cgst = gst_amount / 2
        sgst = gst_amount / 2

    total = subtotal + gst_amount

    # QR code
    qr_data = f"upi://pay?pa={UPI_ID}&pn=HisabKitab&am={total}&cu=INR"
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
    c.drawString(50, 750, f"Invoice No: {invoice_id}")
    c.drawString(50, 730, f"Date: {datetime.now().strftime('%Y-%m-%d')}")
    c.drawString(50, 710, f"Customer: {customer_name}")
    c.drawString(400, 710, f"Status: {payment_status}")

    # Table Header
    c.drawString(50, 670, "Item")
    c.drawString(250, 670, "Qty")
    c.drawString(300, 670, "Price")
    c.drawString(400, 670, "Total")

    y = 650

    for item in items:
        total_item = item["qty"] * item["price"]

        c.drawString(50, y, item["name"])
        c.drawString(250, y, str(item["qty"]))
        c.drawString(300, y, f"₹{item['price']}")
        c.drawString(400, y, f"₹{total_item}")

        y -= 20

    # Summary
    c.drawString(50, y - 20, f"Subtotal: ₹{subtotal}")

    if gst_enabled:
        c.drawString(50, y - 40, f"CGST: ₹{cgst:.2f}")
        c.drawString(50, y - 60, f"SGST: ₹{sgst:.2f}")

    c.drawString(50, y - 80, f"Extra Charges: ₹{extra_charges}")
    c.drawString(50, y - 100, f"Discount: ₹{discount}")

    c.setFont("Helvetica-Bold", 12)
    c.drawString(50, y - 130, f"Total Amount: ₹{total:.2f}")

    # QR payment
    c.drawImage(qr_path, 420, 550, width=120, height=120)

    c.save()

    return {
        "invoice_id": invoice_id,
        "file_path": file_path
    }
