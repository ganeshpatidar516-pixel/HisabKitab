from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from datetime import datetime
import os
import qrcode

INVOICE_DIR = "invoices"

BUSINESS_NAME = "HisabKitab Store"
GST_NUMBER = ""
UPI_ID = "ganesh@upi"


class Item:
    def __init__(self, name, qty, price):
        self.name = name
        self.qty = qty
        self.price = price


def generate_invoice(customer_name, items, note="", template="1", apply_gst=False):

    if not os.path.exists(INVOICE_DIR):
        os.makedirs(INVOICE_DIR)

    invoice_id = f"INV-{int(datetime.now().timestamp())}"
    file_path = f"{INVOICE_DIR}/{invoice_id}.pdf"

    item_objects = []
    for i in items:
        item_objects.append(Item(i["name"], i["qty"], i["price"]))

    amount = 0
    for item in item_objects:
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
        "3": draw_template_3
    }

    template_function = template_map.get(template, draw_template_1)

    template_function(
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
        "file_path": file_path
    }


# ================= TEMPLATE 1 =================

def draw_template_1(c, customer, items, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 20)
    c.drawString(50, 800, BUSINESS_NAME)

    c.setFont("Helvetica", 11)
    c.drawString(50, 770, f"Invoice : {invoice_id}")
    c.drawString(50, 750, f"Customer : {customer}")
    c.drawString(50, 730, f"Note : {note}")

    y = 690

    for item in items:

        item_total = item.qty * item.price

        c.drawString(50, y, item.name)
        c.drawString(250, y, str(item.qty))
        c.drawString(300, y, f"₹{item.price}")
        c.drawString(380, y, f"₹{item_total}")

        y -= 20

    c.drawString(320, y-20, f"Subtotal : ₹{amount}")

    if gst == 0:
        c.drawString(320, y-40, "GST : Not Applied")
    else:
        c.drawString(320, y-40, f"GST : ₹{gst}")

    c.drawString(320, y-60, f"Total : ₹{total}")

    c.drawImage(qr, 450, 750, width=100, height=100)


# ================= TEMPLATE 2 =================

def draw_template_2(c, customer, items, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 18)
    c.drawString(220, 800, "INVOICE")

    c.setFont("Helvetica", 11)
    c.drawString(50, 760, f"Invoice : {invoice_id}")
    c.drawString(50, 740, f"Customer : {customer}")

    y = 700

    for item in items:

        item_total = item.qty * item.price

        c.drawString(50, y, item.name)
        c.drawString(300, y, str(item.qty))
        c.drawString(350, y, f"₹{item.price}")
        c.drawString(450, y, f"₹{item_total}")

        y -= 20

    c.drawString(350, y-20, f"Subtotal : ₹{amount}")

    if gst == 0:
        c.drawString(350, y-40, "GST : Not Applied")
    else:
        c.drawString(350, y-40, f"GST : ₹{gst}")

    c.drawString(350, y-60, f"Total : ₹{total}")

    c.drawImage(qr, 450, 750, width=100, height=100)


# ================= TEMPLATE 3 =================

def draw_template_3(c, customer, items, amount, gst, total, note, invoice_id, qr):

    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, 800, "HisabKitab Invoice")

    c.setFont("Helvetica", 11)
    c.drawString(50, 770, f"Invoice ID : {invoice_id}")
    c.drawString(50, 750, f"Customer : {customer}")

    y = 700

    for item in items:

        item_total = item.qty * item.price
        c.drawString(50, y, f"{item.name} x {item.qty} = ₹{item_total}")

        y -= 20

    c.drawString(50, y-20, f"Subtotal : ₹{amount}")

    if gst == 0:
        c.drawString(50, y-40, "GST : Not Applied")
    else:
        c.drawString(50, y-40, f"GST : ₹{gst}")

    c.drawString(50, y-60, f"Total : ₹{total}")

    c.drawImage(qr, 400, 750, width=120, height=120)
