from fastapi import FastAPI
from fastapi.openapi.utils import get_openapi

from app.api.v1.api import api_router
from app.api.sync_api import router as sync_router
from app.core.database import init_db

app = FastAPI(
    title="HisabKitab Pro Ultra Backend",
    description="Ultra Professional AI-Powered Billing Backend",
    version="1.0.0"
)

@app.on_event("startup")
def startup_event():
    # Initialize Database
    init_db()

# API Router
app.include_router(api_router, prefix="/api/v1")
app.include_router(sync_router)


# Custom Swagger JWT Authorize Button
def custom_openapi():

    if app.openapi_schema:
        return app.openapi_schema

    openapi_schema = get_openapi(
        title="HisabKitab Pro Ultra Backend",
        version="1.0.0",
        description="Ultra Professional AI-Powered Billing Backend",
        routes=app.routes,
    )

    openapi_schema["components"]["securitySchemes"] = {
        "BearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
        }
    }

    openapi_schema["security"] = [
        {
            "BearerAuth": []
        }
    ]

    app.openapi_schema = openapi_schema
    return app.openapi_schema


app.openapi = custom_openapi
