import logging
from fastapi import FastAPI, HTTPException
from datetime import datetime, timezone
from app.models import PriceResponse
from app.services.tcgplayer import fetch_completed_sale_price

logger = logging.getLogger(__name__)

app = FastAPI(title="PokeScan Backend", version="0.2.0")


@app.get("/health")
async def health():
    return {"status": "ok", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.get("/price/{card_sku}", response_model=PriceResponse)
async def get_price(card_sku: str) -> PriceResponse:
    """
    Returns completed-sale market price for card_sku.
    G3: marketPrice only — never listing price.
    API keys stay server-side — never exposed to client.
    """
    try:
        result = await fetch_completed_sale_price(card_sku)
        return PriceResponse(card_sku=card_sku, **result)
    except Exception as e:
        logger.error("price fetch failed for %s: %s", card_sku, e)
        raise HTTPException(status_code=502, detail="Price fetch failed")
