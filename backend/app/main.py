import asyncio
import logging
from datetime import datetime, timezone
from typing import Optional

from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from starlette.requests import Request

from app.models import PriceResponse
from app.routers import auth as auth_router
from app.routers import collection as collection_router
from app.routers import detection as detection_router
from app.routers import grading as grading_router
from app.services.aggregator import aggregate
from app.services.auth import decode_server_token, warmup_google_auth
from app.services.ebay import fetch_completed_sale_price as ebay_fetch
from app.services.tcgplayer import fetch_completed_sale_price as tcg_fetch

logger = logging.getLogger(__name__)

limiter = Limiter(key_func=get_remote_address)
_optional_bearer = HTTPBearer(auto_error=False)

app = FastAPI(title="PokeScan Backend", version="0.4.0")
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://pokescan.app"],
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)

app.include_router(auth_router.router)
app.include_router(collection_router.router)
app.include_router(grading_router.router)
app.include_router(detection_router.router)


@app.on_event("startup")
async def startup_event():
    asyncio.create_task(warmup_google_auth())


@app.get("/health")
async def health():
    return {"status": "ok", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.get("/price/{card_sku}", response_model=PriceResponse)
@limiter.limit("60/minute")
async def get_price(
    request: Request,  # noqa: ARG001 — required by slowapi limiter
    card_sku: str,
    tier: str = "free",
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(_optional_bearer),
) -> PriceResponse:
    """
    Returns completed-sale market price for card_sku.
    G3: marketPrice only — never listing price.
    EN cards (tier=free): TCGPlayer only. EN (tier=pro): weighted avg TCGPlayer + eBay.
    JP cards: eBay completed sales only (TCGPlayer has no JP catalog).
    API keys stay server-side — never exposed to client.
    tier=pro is only honoured when the request carries a valid server JWT.
    """
    if tier == "pro":
        try:
            decode_server_token(credentials.credentials if credentials else "")
        except Exception:
            tier = "free"
    sku_lower = card_sku.lower()
    is_japanese = sku_lower.endswith("-jp") or "-jp-" in sku_lower
    try:
        tcg_price = None
        staleness_flag = False
        tcg_error = None
        if not is_japanese:
            tcg_result = await tcg_fetch(card_sku)
            tcg_price = tcg_result.get("market_price")
            staleness_flag = tcg_result.get("staleness_flag", False)
            tcg_error = tcg_result.get("error")

        ebay_price = None
        if tier == "pro" or is_japanese:
            try:
                ebay_price = await ebay_fetch(card_sku)
            except Exception as e:
                logger.warning("eBay fetch failed for %s: %s", card_sku, e)

        # JP cards have no TCGPlayer data; tier gate is irrelevant — always use eBay path
        agg = aggregate(tcg_price, ebay_price, "pro" if is_japanese else tier)

        price_source = "aggregated" if len(agg["price_sources"]) > 1 else (
            agg["price_sources"][0] if agg["price_sources"] else "unknown"
        )

        return PriceResponse(
            card_sku=card_sku,
            market_price=agg["market_price"],
            price_source=price_source,
            price_sources=agg["price_sources"],
            is_completed_sale=True,
            fetched_at=datetime.now(timezone.utc).isoformat(),
            staleness_flag=staleness_flag,
            error=tcg_error,
        )
    except Exception as e:
        logger.error("price fetch failed for %s: %s", card_sku, e)
        raise HTTPException(status_code=502, detail="Price fetch failed")
