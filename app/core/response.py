from typing import Any, Optional


def success_response(
    data: Any = None,
    message: str = "Request successful",
    count: Optional[int] = None
):
    response = {
        "success": True,
        "message": message,
        "data": data,
        "error": None
    }

    if count is not None:
        response["count"] = count

    return response


def error_response(
    message: str = "Something went wrong",
    error: Any = None
):
    return {
        "success": False,
        "message": message,
        "data": None,
        "error": error
    }
