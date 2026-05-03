import httpx
from datetime import datetime, timezone, timedelta
from cachetools import TTLCache
from app.config import settings

TCGPLAYER_AUTH_URL = "https://api.tcgplayer.com/token"
TCGPLAYER_CATALOG_URL = "https://api.tcgplayer.com/catalog/products"
TCGPLAYER_PRICING_URL = "https://api.tcgplayer.com/pricing/product/{product_id}"

# TCGPlayer tokens expire after 1h; 59-min TTL prevents silent mid-request expiry.
_token_cache: TTLCache = TTLCache(maxsize=1, ttl=3540)


async def get_bearer_token() -> str:
    cached = _token_cache.get("token")
    if cached:
        return cached
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
        token = resp.json()["access_token"]
    _token_cache["token"] = token
    return token


def _parse_sku(card_sku: str) -> tuple[str, str, str]:
    """Parse 'base1-025-102' → (setCode, cardNum, total)."""
    parts = card_sku.split("-")
    if len(parts) < 3:
        raise ValueError(f"Invalid card_sku format: {card_sku}")
    set_code = parts[0]
    card_num = parts[1].lstrip("0") or "0"
    total = parts[2]
    return set_code, card_num, total


async def _get_product_id(card_num: str, total: str, token: str) -> int | None:
    query = f"{card_num}/{total}"
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            TCGPLAYER_CATALOG_URL,
            params={"productName": query, "categoryId": 3, "productTypeName": "Cards"},
            headers={"Authorization": f"Bearer {token}"},
        )
        resp.raise_for_status()
    results = resp.json().get("results", [])
    for product in results:
        for ext in product.get("extendedData", []):
            if ext.get("name") == "Number" and ext.get("value") == query:
                return product["productId"]
    return None


async def fetch_completed_sale_price(card_sku: str) -> dict:
    """
    Returns marketPrice from completed sales. G3: NEVER use lowPrice (listing price).
    card_sku format: "{setCode}-{cardNum}-{total}" e.g. "base1-025-102"
    """
    if settings.pokescan_use_mock:
        return {
            "market_price": 4.99,
            "price_source": "tcgplayer_mock",
            "price_sources": ["tcgplayer_mock"],
            "is_completed_sale": True,
            "fetched_at": datetime.now(timezone.utc).isoformat(),
            "staleness_flag": False,
        }

    token = await get_bearer_token()
    _set_code, card_num, total = _parse_sku(card_sku)
    product_id = await _get_product_id(card_num, total, token)

    if product_id is None:
        return {
            "market_price": None,
            "price_source": "tcgplayer",
            "price_sources": ["tcgplayer"],
            "is_completed_sale": True,
            "fetched_at": datetime.now(timezone.utc).isoformat(),
            "staleness_flag": False,
            "error": f"product not found for {card_sku}",
        }

    async with httpx.AsyncClient() as client:
        resp = await client.get(
            TCGPLAYER_PRICING_URL.format(product_id=product_id),
            headers={"Authorization": f"Bearer {token}"},
        )
        resp.raise_for_status()

    results = resp.json().get("results", [])
    market_price = None
    latest_sale_date = None

    for entry in results:
        if entry.get("subTypeName") == "Normal":
            market_price = entry.get("marketPrice")
            latest_sale_date = entry.get("latestSaleDate")
            break

    staleness_flag = False
    if latest_sale_date:
        try:
            sale_dt = datetime.fromisoformat(latest_sale_date.replace("Z", "+00:00"))
            staleness_flag = (datetime.now(timezone.utc) - sale_dt) > timedelta(hours=24)
        except (ValueError, AttributeError):
            pass

    return {
        "market_price": market_price,
        "price_source": "tcgplayer",
        "price_sources": ["tcgplayer"],
        "is_completed_sale": True,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "staleness_flag": staleness_flag,
    }
