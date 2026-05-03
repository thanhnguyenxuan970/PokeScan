TCGPLAYER_WEIGHT = 0.6
EBAY_WEIGHT = 0.4


def aggregate(
    tcg_price: float | None,
    ebay_price: float | None,
    tier: str,
) -> dict:
    """
    G3: market_price = weighted avg of TCGPlayer (60%) + eBay (40%) completed sales.
    Free tier receives TCGPlayer only.
    """
    if tier == "free" or ebay_price is None:
        return {
            "market_price": tcg_price,
            "price_sources": ["tcgplayer"] if tcg_price is not None else [],
        }
    if tcg_price is None:
        return {
            "market_price": ebay_price,
            "price_sources": ["ebay"],
        }
    weighted = round(tcg_price * TCGPLAYER_WEIGHT + ebay_price * EBAY_WEIGHT, 2)
    return {
        "market_price": weighted,
        "price_sources": ["tcgplayer", "ebay"],
    }
