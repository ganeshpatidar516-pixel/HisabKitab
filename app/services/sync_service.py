import json
import os

SYNC_QUEUE_FILE = "sync_queue.json"


def add_to_sync_queue(data):
    queue = []

    if os.path.exists(SYNC_QUEUE_FILE):
        try:
            with open(SYNC_QUEUE_FILE, "r") as f:
                queue = json.load(f)
        except Exception:
            queue = []

    queue.append(data)

    with open(SYNC_QUEUE_FILE, "w") as f:
        json.dump(queue, f, indent=2)


def get_sync_queue():
    if not os.path.exists(SYNC_QUEUE_FILE):
        return []

    try:
        with open(SYNC_QUEUE_FILE, "r") as f:
            return json.load(f)
    except Exception:
        return []


def clear_sync_queue():
    if os.path.exists(SYNC_QUEUE_FILE):
        os.remove(SYNC_QUEUE_FILE)
