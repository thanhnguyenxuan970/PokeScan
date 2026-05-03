import httpx
from datetime import datetime, timezone, timedelta
from app.config import settings

EBAY_FINDING_URL = "https://svcs.ebay.com/services/search/FindingService/v1"


def _build_search_query(card_sku: str) -> str:
    parts = card_sku.split("-")
    if len(parts) >= 3:
        card_num = parts[1].lstrip("0") or "0"
        total = parts[2]
        return f"{card_num}/{total} pokemon card"
    return f"{card_sku} pokemon card"


async def fetch_completed_sale_price(card_sku: str) -> float | None:
    """
    Returns median price from eBay 30-day completed sales.
    Trims top/bottom 10% outliers before computing median.
    Returns None if no results found.
    """
    thirty_days_ago = (datetime.now(timezone.utc) - timedelta(days=30)).strftime(
        "%Y-%m-%dT%H:%M:%S.000Z"
    )
    query = _build_search_query(card_sku)

    params = {
        "OPERATION-NAME": "findCompletedItems",
        "SERVICE-VERSION": "1.0.0",
        "SECURITY-APPNAME": settings.ebay_app_id,
        "RESPONSE-DATA-FORMAT": "JSON",
        "keywords": query,
        "itemFilter(0).name": "SoldItemsOnly",
        "itemFilter(0).value": "true",
        "itemFilter(1).name": "EndTimeFrom",
        "itemFilter(1).value": thirty_days_ago,
        "paginationInput.entriesPerPage": "20",
    }

    async with httpx.AsyncClient() as client:
        resp = await client.get(
            EBAY_FINDING_URL,
            params=params,
        )
        resp.raise_for_status()

    data = resp.json()
    try:
        items = (
            data["findCompletedItemsResponse"][0]
            ["searchResult"][0]
            .get("item", [])
        )
    except (KeyError, IndexError):
        return None

    prices = []
    for item in items:
        try:
            price = float(
                item["sellingStatus"][0]["currentPrice"][0]["__value__"]
            )
            prices.append(price)
        except (KeyError, IndexError, ValueError):
            continue

    if not prices:
        return None

    prices.sort()
    trim = max(1, len(prices) // 10)
    trimmed = prices[trim:-trim] if len(prices) > 2 * trim else prices
    return round(sum(trimmed) / len(trimmed), 2)
