from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from datetime import datetime
import os
import qrcode

INVOICE_DIR = "invoices"

BUSINESS_NAME = "HisabKitab Store"
GST_NUMBER = "22AAAAA0000A1Z5"
UPI_ID = "ganesh@upi"


def generate_invoice(customer_name, items, note="", template="1", apply_gst=False):

    if not os.path.exists(INVOICE_DIR):
        os.makedirs(INVOICE_DIR)

    invoice_id = f"INV-{int(datetime.now().timestamp())}"
    file_path = f"{INVOICE_DIR}/{invoice_id}.pdf"

    # -------------------------
    # Calculate subtotal from items
    # -------------------------
    amount = 0

    for item in items:
        amount += item.qty * item.price

    gst = 0
    total = amount

    if apply_gst:
        gst = amount * 0.18
        total = amount + gst

    # -------------------------
    # QR code
    # -------------------------
    qr_data = f"upi://pay?pa={UPI_ID}&pn=HisabKitab&am={total}"

    qr = qrcode.make(qr_data)

    qr_path = f"{INVOICE_DIR}/{invoice_id}_qr.png"
    qr.save(qr_path)

    c = canvas.Canvas(file_path, pagesize=A4)

    # -------------------------
    # Template Router
    # -------------------------
    template_map = {
        "1": draw_template_1,
        "2": draw_template_2,
        "3": draw_template_3,
        "4": draw_template_4,
        "5": draw_template_5,
        "6": draw_template_6,
        "7": draw_template_7,
        "8": draw_template_8,
        "9": draw_template_9,
        "10": draw_template_10
    }

    template_function = template_map.get(template, draw_template_1)

    template_function(
        c,
        customer_name,
        amount,
        gst,
        total,
        note,
        invoice_id,
        qr_path
    )

    c.save()

    return {
        "invoice_id": invoice_id,
        "file_path": file_path
    }


# TEMPLATE 1
def draw_template_1(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 18)
    c.drawString(50, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 11)
    c.drawString(50, 780, f"GSTIN: {GST_NUMBER}")

    c.drawString(50, 740, f"Invoice No: {invoice_id}")
    c.drawString(50, 720, f"Customer: {customer}")
    c.drawString(50, 700, f"Note: {note}")

    c.setFont("Helvetica-Bold", 14)

    c.drawString(50, 650, f"Amount: ₹{amount}")
    c.drawString(50, 630, f"GST: ₹{gst}")
    c.drawString(50, 610, f"Total: ₹{total}")

    c.drawImage(qr, 400, 650, width=120, height=120)


# TEMPLATE 2
def draw_template_2(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFillColor(HexColor("#2E86C1"))
    c.rect(0, 760, 600, 80, fill=1)

    c.setFillColor(HexColor("#FFFFFF"))
    c.setFont("Helvetica-Bold", 20)
    c.drawString(50, 790, BUSINESS_NAME)

    c.setFillColor(HexColor("#000000"))

    c.setFont("Helvetica", 11)

    c.drawString(50, 720, f"Invoice: {invoice_id}")
    c.drawString(50, 700, f"Customer: {customer}")

    c.drawString(50, 660, f"Amount: ₹{amount}")
    c.drawString(50, 640, f"GST: ₹{gst}")
    c.drawString(50, 620, f"Total: ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 3
def draw_template_3(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Courier-Bold", 18)
    c.drawString(200, 800, "INVOICE")

    c.setFont("Courier", 12)

    c.drawString(50, 760, f"Business: {BUSINESS_NAME}")
    c.drawString(50, 740, f"Customer: {customer}")

    c.drawString(50, 700, f"Amount: ₹{amount}")
    c.drawString(50, 680, f"GST: ₹{gst}")
    c.drawString(50, 660, f"Total: ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 4
def draw_template_4(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 22)
    c.drawCentredString(300, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 12)

    c.drawString(100, 740, f"Invoice ID: {invoice_id}")
    c.drawString(100, 720, f"Customer: {customer}")

    c.drawString(100, 680, f"Amount: ₹{amount}")
    c.drawString(100, 660, f"GST: ₹{gst}")
    c.drawString(100, 640, f"Total: ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 5
def draw_template_5(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFillColor(HexColor("#000000"))
    c.rect(0, 760, 600, 80, fill=1)

    c.setFillColor(HexColor("#FFFFFF"))

    c.setFont("Helvetica-Bold", 22)
    c.drawString(50, 790, BUSINESS_NAME)

    c.setFillColor(HexColor("#000000"))

    c.drawString(50, 720, f"Invoice: {invoice_id}")
    c.drawString(50, 700, f"Customer: {customer}")

    c.drawString(50, 660, f"Amount ₹{amount}")
    c.drawString(50, 640, f"GST ₹{gst}")
    c.drawString(50, 620, f"Total ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 6
def draw_template_6(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 20)
    c.drawCentredString(300, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 12)

    c.drawString(50, 740, f"Bill No : {invoice_id}")
    c.drawString(50, 720, f"Customer : {customer}")

    c.drawString(50, 680, f"Subtotal ₹{amount}")
    c.drawString(50, 660, f"GST ₹{gst}")
    c.drawString(50, 640, f"Grand Total ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 7
def draw_template_7(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 18)
    c.drawString(50, 800, "INVOICE")

    c.setFont("Helvetica", 12)

    c.drawString(50, 760, f"Customer : {customer}")
    c.drawString(50, 740, f"Invoice ID : {invoice_id}")

    c.drawString(50, 700, f"Total ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 8
def draw_template_8(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 18)
    c.drawString(50, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 11)

    c.drawString(50, 780, f"GSTIN : {GST_NUMBER}")

    c.drawString(50, 740, f"Customer : {customer}")

    c.drawString(50, 700, f"Amount ₹{amount}")
    c.drawString(50, 680, f"GST ₹{gst}")
    c.drawString(50, 660, f"Total ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 9
def draw_template_9(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFillColor(HexColor("#004AAD"))
    c.rect(0, 760, 600, 80, fill=1)

    c.setFillColor(HexColor("#FFFFFF"))

    c.setFont("Helvetica-Bold", 20)
    c.drawString(50, 790, BUSINESS_NAME)

    c.setFillColor(HexColor("#000000"))

    c.drawString(50, 720, f"Invoice {invoice_id}")
    c.drawString(50, 700, f"Customer {customer}")

    c.drawString(50, 660, f"Total ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)


# TEMPLATE 10
def draw_template_10(c, customer, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 24)
    c.drawCentredString(300, 800, "PREMIUM INVOICE")

    c.setFont("Helvetica", 12)

    c.drawCentredString(300, 770, BUSINESS_NAME)

    c.drawString(100, 720, f"Customer : {customer}")
    c.drawString(100, 700, f"Invoice ID : {invoice_id}")

    c.drawString(100, 660, f"Amount ₹{amount}")
    c.drawString(100, 640, f"GST ₹{gst}")
    c.drawString(100, 620, f"Final ₹{total}")

    c.drawImage(qr, 420, 640, width=100, height=100)
