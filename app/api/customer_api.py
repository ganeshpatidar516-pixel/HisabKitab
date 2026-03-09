from fastapi import APIRouter
from app.repositories.customer_repository import (
    create_customer,
    get_customers,
    delete_customer
)

router = APIRouter()


@router.post("/customer/add")
def add_customer(username: str, name: str, phone: str = None, address: str = None):
    return create_customer(username, name, phone, address)


@router.get("/customer/list")
def customer_list(username: str):
    return get_customers(username)


@router.delete("/customer/delete/{customer_id}")
def remove_customer(customer_id: int):
    return delete_customer(customer_id)
