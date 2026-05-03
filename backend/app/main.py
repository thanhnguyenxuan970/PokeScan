import logging
from fastapi import FastAPI, HTTPException
from datetime import datetime, timezone
from app.models import PriceResponse
from app.services.tcgplayer import fetch_completed_sale_price as tcg_fetch
from app.services.ebay import fetch_completed_sale_price as ebay_fetch
from app.services.aggregator import aggregate
from app.routers import auth as auth_router
from app.routers import collection as collection_router

logger = logging.getLogger(__name__)

app = FastAPI(title="PokeScan Backend", version="0.3.0")
app.include_router(auth_router.router)
app.include_router(collection_router.router)


@app.get("/health")
async def health():
    return {"status": "ok", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.get("/price/{card_sku}", response_model=PriceResponse)
async def get_price(card_sku: str, tier: str = "free") -> PriceResponse:
    """
    Returns completed-sale market price for card_sku.
    G3: marketPrice only — never listing price.
    tier=free: TCGPlayer only. tier=pro: weighted avg TCGPlayer + eBay.
    API keys stay server-side — never exposed to client.
    """
    try:
        tcg_result = await tcg_fetch(card_sku)
        tcg_price = tcg_result.get("market_price")
        staleness_flag = tcg_result.get("staleness_flag", False)

        ebay_price = None
        if tier == "pro":
            try:
                ebay_price = await ebay_fetch(card_sku)
            except Exception as e:
                logger.warning("eBay fetch failed for %s: %s", card_sku, e)

        agg = aggregate(tcg_price, ebay_price, tier)

        price_source = "aggregated" if len(agg["price_sources"]) > 1 else (
            agg["price_sources"][0] if agg["price_sources"] else "tcgplayer"
        )

        return PriceResponse(
            card_sku=card_sku,
            market_price=agg["market_price"],
            price_source=price_source,
            price_sources=agg["price_sources"],
            is_completed_sale=True,
            fetched_at=datetime.now(timezone.utc).isoformat(),
            staleness_flag=staleness_flag,
            error=tcg_result.get("error"),
        )
    except Exception as e:
        logger.error("price fetch failed for %s: %s", card_sku, e)
        raise HTTPException(status_code=502, detail="Price fetch failed")
