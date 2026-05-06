from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.services import collection as col_svc
from app.database import get_db
from app.dependencies import get_current_user_id

router = APIRouter(prefix="/collection", tags=["collection"])

FREE_TIER_LIMIT = 50


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
async def list_collection(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user = await col_svc.get_or_create_user(db, user_id)
    cards = await col_svc.list_cards(db, user.id)
    return [
        CardOut(
            server_id=str(c.id),
            card_sku=c.card_sku,
            name=c.name,
            set_code=c.set_code,
            set_number=c.set_number,
            language=c.language or "english",
            market_price=float(c.market_price) if c.market_price is not None else None,
            price_source=c.price_source,
            scanned_at=c.scanned_at,
        )
        for c in cards
    ]


@router.post("", response_model=dict)
async def add_card(
    card: CardIn,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user = await col_svc.get_or_create_user(db, user_id)
    count = await col_svc.count_active_cards(db, user.id)
    if user.tier == "free" and count >= FREE_TIER_LIMIT:
        raise HTTPException(status_code=403, detail="Free tier limit: 50 cards")
    record = await col_svc.add_card(db, user.id, card.model_dump())
    return {"server_id": str(record.id)}


@router.delete("/{card_id}")
async def delete_card(
    card_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user = await col_svc.get_user(db, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="Card not found")
    deleted = await col_svc.soft_delete_card(db, user.id, card_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Card not found")
    return {"deleted": True}
