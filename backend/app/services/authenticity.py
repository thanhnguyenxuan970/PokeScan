import re

SET_NUMBER_PATTERN = re.compile(r"^\d+/\d+$")

RECOMMENDATIONS = {
    "low": "Card appears authentic. No significant concerns detected.",
    "medium": "Some risk factors detected. Request additional photos or verify set details.",
    "high": "High risk of counterfeit. Request professional authentication before purchase.",
}


def _has_suspicious_chars(text: str) -> bool:
    """Returns True if text contains chars outside ASCII printable + Japanese CJK/kana blocks."""
    for c in text:
        cp = ord(c)
        ascii_ok = 0x20 <= cp <= 0x7E
        hiragana_katakana = 0x3040 <= cp <= 0x30FF
        cjk = 0x4E00 <= cp <= 0x9FFF
        if not (ascii_ok or hiragana_katakana or cjk):
            return True
    return False


def check_authenticity(
    card_sku: str,
    card_name: str,
    set_number: str,
    market_price: float,
    listed_price: float,
    scan_confidence: float,
) -> dict:
    score = 0.0
    flags: list[str] = []

    if market_price > 0 and listed_price > market_price * 3:
        score += 0.4
        flags.append("listed_price_above_market_3x")

    if not SET_NUMBER_PATTERN.match(set_number):
        score += 0.3
        flags.append("set_number_format_invalid")

    if scan_confidence < 0.7:
        score += 0.2
        flags.append("low_scan_confidence")

    if _has_suspicious_chars(card_name):
        score += 0.3
        flags.append("suspicious_characters")

    if "unknown" in card_sku.lower():
        score += 0.1
        flags.append("unknown_set")

    score = round(min(score, 1.0), 2)

    if score <= 0.3:
        risk_level = "low"
    elif score <= 0.6:
        risk_level = "medium"
    else:
        risk_level = "high"

    return {
        "risk_level": risk_level,
        "risk_score": score,
        "flags": flags,
        "recommendation": RECOMMENDATIONS[risk_level],
    }
