import os
import time
import qrcode

from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4

from database import get_db_connection


# =========================
# PATH SETUP
# =========================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
INVOICE_DIR = os.path.join(BASE_DIR, "invoices")

os.makedirs(INVOICE_DIR, exist_ok=True)


# =========================
# LOAD BUSINESS SETTINGS
# =========================

def load_business_settings(username):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            "SELECT * FROM business_settings WHERE username=?",
            (username,)
        )

        row = cursor.fetchone()

        if not row:

            return {
                "business_name": "HisabKitab Pro",
                "gst_number": "",
                "phone": "",
                "address": "",
                "logo": "",
                "default_template": "1"
            }

        return dict(row)


# =========================
# ITEM CLASS
# =========================

class ItemObj:

    def __init__(self, name, qty, price):

        self.name = name
        self.qty = qty
        self.price = price


# =========================
# BUSINESS HEADER
# =========================

def draw_business_header(c, settings):

    name = settings.get("business_name", "")
    phone = settings.get("phone", "")
    address = settings.get("address", "")
    gst = settings.get("gst_number", "")

    c.setFont("Helvetica-Bold", 18)

    if name:
        c.drawString(50, 820, name)

    c.setFont("Helvetica", 10)

    y = 800

    if address:
        c.drawString(50, y, address)
        y -= 15

    if phone:
        c.drawString(50, y, f"Phone: {phone}")
        y -= 15

    if gst:
        c.drawString(50, y, f"GST: {gst}")


# =========================
# TABLE HEADER
# =========================

def draw_table_header(c):

    c.setFont("Helvetica-Bold", 11)

    c.drawString(50, 680, "Item")
    c.drawString(250, 680, "Qty")
    c.drawString(320, 680, "Price")
    c.drawString(400, 680, "Total")

    c.line(50, 675, 500, 675)


# =========================
# FOOTER
# =========================

def draw_footer(c):

    c.setFont("Helvetica", 9)

    c.drawString(200, 80, "Thank you for your business")
    c.drawString(190, 65, "Powered by HisabKitab Pro")


# =========================
# TEMPLATE 1
# =========================

def draw_template_1(c, settings, customer, items, amount, gst, total, note, invoice_id, qr):

    draw_business_header(c, settings)

    c.setFont("Helvetica-Bold", 16)
    c.drawString(240, 820, "TAX INVOICE")

    c.setFont("Helvetica", 11)

    c.drawString(50, 750, f"Invoice : {invoice_id}")
    c.drawString(50, 730, f"Customer : {customer}")

    if note:
        c.drawString(50, 710, f"Note : {note}")

    draw_table_header(c)

    y = 650

    for item in items:

        item_total = item.qty * item.price

        c.drawString(50, y, item.name)
        c.drawString(250, y, str(item.qty))
        c.drawString(320, y, str(item.price))
        c.drawString(400, y, str(item_total))

        y -= 20

    c.line(50, y, 500, y)

    c.drawString(320, y-20, f"Subtotal : ₹{amount}")

    if gst == 0:
        c.drawString(320, y-40, "GST : Not Applied")
    else:
        c.drawString(320, y-40, f"GST : ₹{gst}")

    c.drawString(320, y-60, f"Total : ₹{total}")

    c.setFont("Helvetica-Bold", 10)
    c.drawString(450, 860, "Scan & Pay")

    c.drawImage(qr, 450, 740, width=100, height=100)

    draw_footer(c)


# =========================
# MAIN INVOICE FUNCTION
# =========================

def generate_invoice(username, customer_name, items, note="", template="1", apply_gst=False):

    settings = load_business_settings(username)

    invoice_id = f"INV-{int(time.time())}"

    pdf_path = os.path.join(INVOICE_DIR, f"{invoice_id}.pdf")
    qr_path = os.path.join(INVOICE_DIR, f"{invoice_id}_qr.png")

    item_objects = []

    for i in items:

        item_objects.append(
            ItemObj(i["name"], i["qty"], i["price"])
        )

    amount = 0

    for item in item_objects:

        amount += item.qty * item.price

    gst = 0

    if apply_gst:

        gst = amount * 0.18

    total = amount + gst

    qr_data = f"upi://pay?pa=test@upi&pn=HisabKitab&am={total}"

    qr = qrcode.make(qr_data)

    qr.save(qr_path)

    c = canvas.Canvas(pdf_path, pagesize=A4)

    draw_template_1(
        c,
        settings,
        customer_name,
        item_objects,
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
        "file_path": f"invoices/{invoice_id}.pdf"
    }
