import json
import os

SYNC_QUEUE_FILE = "sync_queue.json"


def add_to_sync_queue(data):

    queue = []

    if os.path.exists(SYNC_QUEUE_FILE):

        with open(SYNC_QUEUE_FILE, "r") as f:
            queue = json.load(f)

    queue.append(data)

    with open(SYNC_QUEUE_FILE, "w") as f:
        json.dump(queue, f)


def get_sync_queue():

    if not os.path.exists(SYNC_QUEUE_FILE):
        return []

    with open(SYNC_QUEUE_FILE, "r") as f:
        return json.load(f)


def clear_sync_queue():

    if os.path.exists(SYNC_QUEUE_FILE):
        os.remove(SYNC_QUEUE_FILE)/
