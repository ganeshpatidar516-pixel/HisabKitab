from fastapi import APIRouter
from pydantic import BaseModel
from app.core.database import get_connection
from app.core.response import success_response, error_response

router = APIRouter()


class Reminder(BaseModel):
    title: str
    description: str = ""
    time: str


@router.post("/")
def add_reminder(reminder: Reminder):
    try:
        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute(
            "INSERT INTO reminders (title, description, time) VALUES (?, ?, ?)",
            (reminder.title, reminder.description, reminder.time)
        )

        conn.commit()
        conn.close()

        return success_response(message="Reminder added successfully")

    except Exception as e:
        return error_response(message="Failed to add reminder", error=str(e))


@router.get("/")
def list_reminders():
    try:
        conn = get_connection()
        cursor = conn.cursor()

        cursor.execute("SELECT * FROM reminders ORDER BY id DESC")
        rows = cursor.fetchall()
        conn.close()

        data = [dict(row) for row in rows]

        return success_response(
            data=data,
            message="Reminders fetched successfully",
            count=len(data)
        )

    except Exception as e:
        return error_response(message="Failed to fetch reminders", error=str(e))
