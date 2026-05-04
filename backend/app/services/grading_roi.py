from typing import Optional

CONDITION_TO_GRADE: dict[str, int] = {
    "near_mint": 9,
    "lightly_played": 7,
    "moderately_played": 5,
    "heavily_played": 3,
}

GRADE_MULTIPLIERS: dict[int, float] = {
    10: 4.0,
    9: 2.0,
    8: 1.3,
    7: 1.0,
    6: 0.85,
    5: 0.75,
    4: 0.75,
    3: 0.75,
    2: 0.75,
    1: 0.75,
}

GRADING_FEES: dict[str, float] = {
    "psa": 25.0,
    "bgs": 20.0,
    "cgc": 18.0,
}


def calculate_roi(raw_price: float, condition: str, service: str) -> dict:
    if condition not in CONDITION_TO_GRADE:
        raise ValueError(f"Unknown condition: {condition!r}. Valid: {list(CONDITION_TO_GRADE)}")
    if service not in GRADING_FEES:
        raise ValueError(f"Unknown service: {service!r}. Valid: {list(GRADING_FEES)}")

    expected_grade = CONDITION_TO_GRADE[condition]
    multiplier = GRADE_MULTIPLIERS[expected_grade]
    grading_fee = GRADING_FEES[service]

    graded_market_value = raw_price * multiplier
    net_roi = graded_market_value - raw_price - grading_fee
    roi_pct = net_roi / (raw_price + grading_fee) * 100

    break_even_grade: Optional[int] = None
    for grade in range(1, 11):
        gv = raw_price * GRADE_MULTIPLIERS[grade]
        if gv - raw_price - grading_fee >= 0:
            break_even_grade = grade
            break

    if raw_price > 10:
        confidence = "high"
    elif raw_price >= 2:
        confidence = "medium"
    else:
        confidence = "low"

    return {
        "expected_grade": expected_grade,
        "graded_market_value": round(graded_market_value, 2),
        "grading_fee": grading_fee,
        "net_roi": round(net_roi, 2),
        "roi_pct": round(roi_pct, 1),
        "break_even_grade": break_even_grade,
        "confidence": confidence,
    }
