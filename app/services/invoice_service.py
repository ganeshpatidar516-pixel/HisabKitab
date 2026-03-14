import os
import time
import qrcode

from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4

from database import get_db_connection


# =========================
# PATH SETUP (RENDER SAFE)
# =========================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
INVOICE_DIR = os.path.join(BASE_DIR, "../../invoices")
INVOICE_DIR = os.path.abspath(INVOICE_DIR)

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
# LOAD BUSINESS SETTINGS
# =========================

def load_business_settings(username):

    with get_db_connection() as conn:

        cur = conn.cursor()

        cur.execute(
            "SELECT * FROM business_settings WHERE username=?",
            (username,)
        )

        row = cur.fetchone()

        if not row:
            return {
                "business_name": "HisabKitab Pro",
                "gst_number": "",
                "phone": "",
                "address": "",
                "logo": ""
            }

        return dict(row)


# =========================
# SAVE INVOICE DB
# =========================

def save_invoice_db(username, invoice_id, customer, amount, gst, total):

    with get_db_connection() as conn:

        cur = conn.cursor()

        cur.execute("""
        CREATE TABLE IF NOT EXISTS invoices(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT,
            invoice_id TEXT,
            customer TEXT,
            amount REAL,
            gst REAL,
            total REAL,
            created_at TEXT
        )
        """)

        cur.execute(
            """
            INSERT INTO invoices(username, invoice_id, customer, amount, gst, total, created_at)
            VALUES(?,?,?,?,?,?,?)
            """,
            (
                username,
                invoice_id,
                customer,
                amount,
                gst,
                total,
                time.strftime("%Y-%m-%d %H:%M:%S")
            )
        )

        conn.commit()


# =========================
# HEADER
# =========================

def draw_header(c, settings):

    name = settings.get("business_name", "")
    phone = settings.get("phone", "")
    address = settings.get("address", "")
    gst = settings.get("gst_number", "")

    c.setFont("Helvetica-Bold", 18)
    c.drawString(50, 820, name)

    c.setFont("Helvetica", 10)

    if address:
        c.drawString(50, 800, address)

    if phone:
        c.drawString(50, 785, f"Phone: {phone}")

    if gst:
        c.drawString(50, 770, f"GST: {gst}")


# =========================
# FOOTER
# =========================

def draw_footer(c):

    c.setFont("Helvetica", 9)

    c.drawString(200, 80, "Thank you for your business")
    c.drawString(190, 65, "Powered by HisabKitab Pro")


# =========================
# GENERATE INVOICE
# =========================

def generate_invoice(username, customer_name, items, apply_gst=True):

    if not items:
        raise Exception("Invoice items missing")

    settings = load_business_settings(username)

    invoice_id = f"INV-{int(time.time())}"

    pdf_path = os.path.join(INVOICE_DIR, f"{invoice_id}.pdf")

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


    # =========================
    # QR
    # =========================

    qr_data = f"upi://pay?pa=test@upi&pn=HisabKitab&am={total}"

    qr = qrcode.make(qr_data)

    qr_path = os.path.join(INVOICE_DIR, f"{invoice_id}_qr.png")

    qr.save(qr_path)


    # =========================
    # PDF
    # =========================

    c = canvas.Canvas(pdf_path, pagesize=A4)

    draw_header(c, settings)

    c.setFont("Helvetica-Bold", 16)
    c.drawString(240, 820, "TAX INVOICE")

    c.setFont("Helvetica", 11)

    c.drawString(50, 740, f"Invoice : {invoice_id}")
    c.drawString(50, 720, f"Customer : {customer_name}")

    y = 680

    c.setFont("Helvetica-Bold", 11)

    c.drawString(50, y, "Item")
    c.drawString(250, y, "Qty")
    c.drawString(320, y, "Price")
    c.drawString(400, y, "Total")

    y -= 30

    c.setFont("Helvetica", 10)

    for item in item_objects:

        item_total = item.qty * item.price

        c.drawString(50, y, item.name)
        c.drawString(250, y, str(item.qty))
        c.drawString(320, y, str(item.price))
        c.drawString(400, y, str(item_total))

        y -= 20

    c.line(50, y, 500, y)

    c.drawString(320, y - 20, f"Subtotal : ₹{amount}")

    if gst:
        c.drawString(320, y - 40, f"GST : ₹{gst}")
    else:
        c.drawString(320, y - 40, "GST : Not Applied")

    c.drawString(320, y - 60, f"Total : ₹{total}")

    c.drawImage(qr_path, 450, 740, width=100, height=100)

    draw_footer(c)

    c.save()

    save_invoice_db(username, invoice_id, customer_name, amount, gst, total)

    return {
        "invoice_id": invoice_id,
        "file_path": f"invoices/{invoice_id}.pdf"
    }
