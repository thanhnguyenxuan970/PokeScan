import httpx
from datetime import datetime, timezone
from app.config import settings

TCGPLAYER_AUTH_URL = "https://api.tcgplayer.com/token"

_bearer_token: str = ""


async def get_bearer_token() -> str:
    global _bearer_token
    if _bearer_token:
        return _bearer_token
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            TCGPLAYER_AUTH_URL,
            data={
                "grant_type": "client_credentials",
                "client_id": settings.tcgplayer_public_key,
                "client_secret": settings.tcgplayer_private_key,
            },
        )
        resp.raise_for_status()
        _bearer_token = resp.json()["access_token"]
    return _bearer_token


async def fetch_completed_sale_price(card_sku: str) -> dict:
    """
    Returns marketPrice (completed sales). G3: NEVER use lowPrice (listing).
    card_sku format: "{setCode}-{cardNum}-{total}" e.g. "base1-025-102"
    """
    # Phase 2 stub: return fixed price to prove URLSession pipeline end-to-end.
    # get_bearer_token() not called yet — TCGPlayer SKU→product_id mapping is Phase 3.
    # TODO Phase 3: token = await get_bearer_token(); catalog search; real marketPrice fetch.
    return {
        "market_price": 4.99,
        "price_source": "tcgplayer",
        "is_completed_sale": True,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
    }
