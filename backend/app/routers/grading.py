from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.routers.collection import get_current_user_id
from app.services import collection as col_svc
from app.services import grading_roi
from app.database import get_db

router = APIRouter(prefix="/grading", tags=["grading"])


class GradeROIRequest(BaseModel):
    card_sku: str
    raw_price: float
    condition: str  # near_mint | lightly_played | moderately_played | heavily_played
    service: str    # psa | bgs | cgc


class GradeROIResponse(BaseModel):
    expected_grade: int
    graded_market_value: float
    grading_fee: float
    net_roi: float
    roi_pct: float
    break_even_grade: Optional[int]
    confidence: str


@router.post("/roi", response_model=GradeROIResponse)
async def grade_roi(
    body: GradeROIRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user = await col_svc.get_or_create_user(db, user_id)
    if user.tier != "pro":
        raise HTTPException(status_code=403, detail="Pro tier required")

    try:
        result = grading_roi.calculate_roi(
            raw_price=body.raw_price,
            condition=body.condition,
            service=body.service,
        )
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc))
    return GradeROIResponse(**result)
