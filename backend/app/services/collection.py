from datetime import datetime, timezone
from typing import Any
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from app.models_db import User, CollectionCard


async def get_or_create_user(db: AsyncSession, apple_user_id: str) -> User:
    result = await db.execute(select(User).where(User.apple_user_id == apple_user_id))
    user = result.scalar_one_or_none()
    if user is None:
        user = User(apple_user_id=apple_user_id)
        db.add(user)
        await db.flush()
    return user


async def count_active_cards(db: AsyncSession, user_id: Any) -> int:
    result = await db.execute(
        select(func.count()).select_from(CollectionCard).where(
            CollectionCard.user_id == user_id,
            CollectionCard.deleted_at.is_(None),
        )
    )
    return result.scalar_one()


async def list_cards(db: AsyncSession, user_id: Any) -> list[CollectionCard]:
    result = await db.execute(
        select(CollectionCard).where(
            CollectionCard.user_id == user_id,
            CollectionCard.deleted_at.is_(None),
        ).order_by(CollectionCard.scanned_at.desc())
    )
    return list(result.scalars())


async def add_card(db: AsyncSession, user_id: Any, card_data: dict) -> CollectionCard:
    card = CollectionCard(
        user_id=user_id,
        card_sku=card_data["card_sku"],
        name=card_data["name"],
        set_code=card_data.get("set_code"),
        set_number=card_data.get("set_number"),
        language=card_data.get("language", "english"),
        market_price=card_data.get("market_price"),
        price_source=card_data.get("price_source"),
        scanned_at=card_data["scanned_at"],
    )
    db.add(card)
    await db.flush()
    return card


async def soft_delete_card(db: AsyncSession, user_id: Any, card_id: str) -> bool:
    result = await db.execute(
        select(CollectionCard).where(
            CollectionCard.id == card_id,
            CollectionCard.user_id == user_id,
            CollectionCard.deleted_at.is_(None),
        )
    )
    card = result.scalar_one_or_none()
    if card is None:
        return False
    card.deleted_at = datetime.now(timezone.utc)
    return True
