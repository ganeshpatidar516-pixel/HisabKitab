from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from passlib.context import CryptContext
from datetime import datetime, timedelta
from jose import JWTError, jwt
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from app.core.database import get_connection
from app.core.response import success_response, error_response


router = APIRouter()

SECRET_KEY = "ultra_secret_key"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60

pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
security = HTTPBearer()


class UserCreate(BaseModel):
    username: str
    password: str


class UserLogin(BaseModel):
    username: str
    password: str


def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)

    to_encode.update({"exp": expire})

    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


def decode_access_token(token: str):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload

    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")


def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):

    token = credentials.credentials
    payload = decode_access_token(token)

    return payload


@router.post("/register")
def register(user: UserCreate):

    conn = get_connection()
    cursor = conn.cursor()

    try:

        # Check existing user
        cursor.execute("SELECT username FROM users WHERE username=?", (user.username,))
        existing_user = cursor.fetchone()

        if existing_user:
            return error_response(message="User already exists")

        hashed_password = pwd_context.hash(user.password)

        cursor.execute(
            "INSERT INTO users (username, hashed_password) VALUES (?, ?)",
            (user.username, hashed_password)
        )

        conn.commit()

        return success_response(message="User registered successfully")

    except Exception as e:

        return error_response(message="Registration failed", error=str(e))

    finally:
        conn.close()


@router.post("/login")
def login(user: UserLogin):

    conn = get_connection()
    cursor = conn.cursor()

    try:

        cursor.execute("SELECT * FROM users WHERE username=?", (user.username,))
        db_user = cursor.fetchone()

        if not db_user:
            raise HTTPException(status_code=401, detail="Invalid username")

        if not pwd_context.verify(user.password, db_user["hashed_password"]):
            raise HTTPException(status_code=401, detail="Invalid password")

        access_token = create_access_token({"sub": user.username})

        return success_response(
            message="Login successful",
            data={"access_token": access_token}
        )

    finally:
        conn.close()
