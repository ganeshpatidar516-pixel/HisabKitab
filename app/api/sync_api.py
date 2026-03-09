from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
from app.services.sync_service import add_to_sync_queue, get_sync_queue, clear_sync_queue

router = APIRouter()


class SyncItem(BaseModel):
    type: str
    data: dict


@router.post("/sync")
def sync_data(items: List[SyncItem]):

    for item in items:
        add_to_sync_queue(item.dict())

    return {
        "status": "queued",
        "items_received": len(items)
    }


@router.get("/sync/queue")
def get_queue():

    queue = get_sync_queue()

    return {
        "pending_items": queue,
        "count": len(queue)
    }


@router.delete("/sync/queue")
def clear_queue():

    clear_sync_queue()

    return {
        "status": "queue cleared"
    }
