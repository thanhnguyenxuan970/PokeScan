from pydantic import BaseModel
from typing import Optional


class PriceResponse(BaseModel):
    card_sku: str
    market_price: Optional[float]
    price_source: str = "tcgplayer"
    is_completed_sale: bool = True   # G3: never listing price
    fetched_at: str
    error: Optional[str] = None
