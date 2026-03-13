import os
import time
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4
import qrcode

# =========================
# BUSINESS SETTINGS
# =========================

BUSINESS_NAME = "HisabKitab Pro"
BUSINESS_ADDRESS = ""
BUSINESS_PHONE = ""
BUSINESS_GST = ""
BUSINESS_LOGO = ""

UPI_ID = "demo@upi"

# =========================
# PATH SETUP
# =========================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
INVOICE_DIR = os.path.join(BASE_DIR, "invoices")

os.makedirs(INVOICE_DIR, exist_ok=True)


# =========================
# ITEM CLASS
# =========================

class ItemObj:
    def __init__(self, name, qty, price):
        self.name = name
        self.qty = qty
        self.price = price


# =========================
# HEADER FUNCTION
# =========================

def draw_business_header(c):

    c.setFont("Helvetica-Bold", 18)
    c.drawString(50, 820, BUSINESS_NAME)

    c.setFont("Helvetica", 10)

    y = 800

    if BUSINESS_ADDRESS:
        c.drawString(50, y, BUSINESS_ADDRESS)
        y -= 15

    if BUSINESS_PHONE:
        c.drawString(50, y, f"Phone: {BUSINESS_PHONE}")
        y -= 15

    if BUSINESS_GST:
        c.drawString(50, y, f"GST: {BUSINESS_GST}")


# =========================
# DRAW TABLE HEADER
# =========================

def draw_table_header(c):

    c.setFont("Helvetica-Bold", 11)

    c.drawString(50, 680, "Item")
    c.drawString(250, 680, "Qty")
    c.drawString(320, 680, "Price")
    c.drawString(400, 680, "Total")

    c.line(50, 675, 500, 675)


# =========================
# TEMPLATE 1
# =========================

def draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr):

    draw_business_header(c)

    c.setFont("Helvetica-Bold", 14)
    c.drawString(250, 820, "INVOICE")

    c.setFont("Helvetica", 11)
    c.drawString(50, 750, f"Invoice : {invoice_id}")
    c.drawString(50, 730, f"Customer : {customer}")
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

    c.drawImage(qr, 450, 740, width=100, height=100)


# =========================
# TEMPLATE 2
# =========================

def draw_template_2(c, customer, items, amount, gst, total, note, invoice_id, qr):

    draw_business_header(c)

    c.setFont("Helvetica-Bold", 18)
    c.drawString(220, 820, "INVOICE")

    c.setFont("Helvetica", 11)
    c.drawString(50, 750, f"Invoice : {invoice_id}")
    c.drawString(50, 730, f"Customer : {customer}")

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

    c.drawString(350, y-20, f"Subtotal : ₹{amount}")

    if gst == 0:
        c.drawString(350, y-40, "GST : Not Applied")
    else:
        c.drawString(350, y-40, f"GST : ₹{gst}")

    c.drawString(350, y-60, f"Total : ₹{total}")

    c.drawImage(qr, 450, 740, width=100, height=100)


# =========================
# TEMPLATE 3
# =========================

def draw_template_3(c, customer, items, amount, gst, total, note, invoice_id, qr):

    draw_business_header(c)

    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, 820, "HisabKitab Invoice")

    c.setFont("Helvetica", 11)
    c.drawString(50, 770, f"Invoice ID : {invoice_id}")
    c.drawString(50, 750, f"Customer : {customer}")

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

    c.drawString(50, y-20, f"Subtotal : ₹{amount}")

    if gst == 0:
        c.drawString(50, y-40, "GST : Not Applied")
    else:
        c.drawString(50, y-40, f"GST : ₹{gst}")

    c.drawString(50, y-60, f"Total : ₹{total}")

    c.drawImage(qr, 400, 740, width=120, height=120)


# =========================
# MAIN INVOICE FUNCTION
# =========================

def generate_invoice(customer_name, items, note="", template="1", apply_gst=False):

    invoice_id = f"INV-{int(time.time())}"

    pdf_path = os.path.join(INVOICE_DIR, f"{invoice_id}.pdf")
    qr_path = os.path.join(INVOICE_DIR, f"{invoice_id}_qr.png")

    item_objects = []

    for i in items:
        item_objects.append(ItemObj(i["name"], i["qty"], i["price"]))

    amount = 0

    for item in item_objects:
        amount += item.qty * item.price

    gst = 0

    if apply_gst:
        gst = amount * 0.18

    total = amount + gst

    qr_data = f"upi://pay?pa={UPI_ID}&pn=HisabKitab&am={total}"

    qr = qrcode.make(qr_data)
    qr.save(qr_path)

    c = canvas.Canvas(pdf_path, pagesize=A4)

    template_map = {
        "1": draw_template_1,
        "2": draw_template_2,
        "3": draw_template_3
    }

    template_func = template_map.get(template, draw_template_1)

    template_func(
        c,
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
