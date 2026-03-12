from fastapi import APIRouter
from database import get_db_connection

router = APIRouter()

@router.get("/business/settings/{username}")
def get_business_settings(username: str):

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            "SELECT * FROM business_settings WHERE username=?",
            (username,)
        )

        row = cursor.fetchone()

        if not row:
            return {
                "business_name": "",
                "gst_number": "",
                "phone": "",
                "address": "",
                "logo": "",
                "default_template": "1"
            }

        return dict(row)


@router.post("/business/settings")
def save_business_settings(data: dict):

    username = data.get("username")
    business_name = data.get("business_name", "")
    gst_number = data.get("gst_number", "")
    phone = data.get("phone", "")
    address = data.get("address", "")
    logo = data.get("logo", "")
    default_template = data.get("default_template", "1")

    with get_db_connection() as conn:
        cursor = conn.cursor()

        cursor.execute(
            "SELECT id FROM business_settings WHERE username=?",
            (username,)
        )

        row = cursor.fetchone()

        if row:

            cursor.execute("""
            UPDATE business_settings
            SET business_name=?,
                gst_number=?,
                phone=?,
                address=?,
                logo=?,
                default_template=?,
                updated_at=CURRENT_TIMESTAMP
            WHERE username=?
            """,
            (
                business_name,
                gst_number,
                phone,
                address,
                logo,
                default_template,
                username
            ))

        else:

            cursor.execute("""
            INSERT INTO business_settings
            (username,business_name,gst_number,phone,address,logo,default_template)
            VALUES (?,?,?,?,?,?,?)
            """,
            (
                username,
                business_name,
                gst_number,
                phone,
                address,
                logo,
                default_template
            ))

        return {"status": "saved"}
