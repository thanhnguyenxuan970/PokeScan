from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from app.services.auth import decode_server_token
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

router = APIRouter(prefix="/collection", tags=["collection"])
_bearer = HTTPBearer()

FREE_TIER_LIMIT = 50


async def get_current_user_id(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
) -> str:
    try:
        return decode_server_token(credentials.credentials)
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or expired token")


class CardIn(BaseModel):
    card_sku: str
    name: str
    set_code: Optional[str] = None
    set_number: Optional[str] = None
    language: str = "english"
    market_price: Optional[float] = None
    price_source: Optional[str] = None
    scanned_at: datetime


class CardOut(BaseModel):
    server_id: str
    card_sku: str
    name: str
    set_code: Optional[str]
    set_number: Optional[str]
    language: str
    market_price: Optional[float]
    price_source: Optional[str]
    scanned_at: datetime


@router.get("", response_model=list[CardOut])
async def list_collection(user_id: str = Depends(get_current_user_id)):
    # Placeholder until DB session is wired via dependency injection in Phase 3 setup.
    # Full implementation requires AsyncSession from SQLAlchemy engine (alembic + DATABASE_URL).
    raise HTTPException(status_code=503, detail="Database not configured")


@router.post("", response_model=dict)
async def add_card(card: CardIn, user_id: str = Depends(get_current_user_id)):
    raise HTTPException(status_code=503, detail="Database not configured")


@router.delete("/{card_id}")
async def delete_card(card_id: str, user_id: str = Depends(get_current_user_id)):
    raise HTTPException(status_code=503, detail="Database not configured")
