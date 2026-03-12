from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from datetime import datetime
import os
import qrcode

from database import get_db_connection

INVOICE_DIR = "invoices"

BUSINESS_NAME = "HisabKitab Store"
GST_NUMBER = "22AAAAA0000A1Z5"
UPI_ID = "ganesh@upi"


def get_business_settings(username):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            "SELECT * FROM business_settings WHERE username=?",
            (username,)
        )

        row = cursor.fetchone()

        if not row:
            return {
                "business_name": "My Store",
                "gst_number": "",
                "phone": "",
                "address": "",
                "logo": "",
                "default_template": "1"
            }

        return dict(row)


def generate_invoice(customer_name, items, note="", template="1", apply_gst=False):

    if not os.path.exists(INVOICE_DIR):
        os.makedirs(INVOICE_DIR)

    invoice_id = f"INV-{int(datetime.now().timestamp())}"
    file_path = f"{INVOICE_DIR}/{invoice_id}.pdf"

    amount = 0

    for item in items:
        amount += item.qty * item.price

    gst = 0
    total = amount

    if apply_gst:
        gst = amount * 0.18
        total = amount + gst

    qr_data = f"upi://pay?pa={UPI_ID}&pn=HisabKitab&am={total}"
    qr = qrcode.make(qr_data)

    qr_path = f"{INVOICE_DIR}/{invoice_id}_qr.png"
    qr.save(qr_path)

    c = canvas.Canvas(file_path, pagesize=A4)

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
        items,
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


def draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 22)
    c.drawString(50, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 11)
    c.drawString(50, 780, f"GSTIN : {GST_NUMBER}")

    c.drawString(50, 740, f"Invoice : {invoice_id}")
    c.drawString(50, 720, f"Customer : {customer}")
    c.drawString(50, 700, f"Note : {note}")

    y = 660

    c.setFont("Helvetica-Bold", 12)

    c.drawString(50, y, "Item")
    c.drawString(250, y, "Qty")
    c.drawString(320, y, "Price")
    c.drawString(420, y, "Total")

    y -= 20

    c.setFont("Helvetica", 11)

    for item in items:

        item_total = item.qty * item.price

        c.drawString(50, y, item.name)
        c.drawString(250, y, str(item.qty))
        c.drawString(320, y, f"₹{item.price}")
        c.drawString(420, y, f"₹{item_total}")

        y -= 20

    y -= 10

    c.setFont("Helvetica-Bold", 12)

    c.drawString(320, y, f"Subtotal : ₹{amount}")

    y -= 20

    if gst == 0:
        c.drawString(320, y, "GST : Not Applied")
    else:
        c.drawString(320, y, f"GST : ₹{gst}")

    y -= 20

    c.setFont("Helvetica-Bold", 14)
    c.drawString(320, y, f"Total : ₹{total}")

    c.drawImage(qr, 400, 740, width=100, height=100)


def draw_template_2(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_3(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_4(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_5(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_6(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_7(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_8(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_9(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)


def draw_template_10(c, customer, items, amount, gst, total, note, invoice_id, qr):
    draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr)
