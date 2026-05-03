from pydantic import BaseModel
from typing import Optional


class PriceResponse(BaseModel):
    card_sku: str
    market_price: Optional[float]
    price_source: str = "tcgplayer"
    price_sources: list[str] = []
    is_completed_sale: bool = True   # G3: never listing price
    fetched_at: str
    staleness_flag: bool = False     # True when marketPrice data is >24h old
    error: Optional[str] = None
