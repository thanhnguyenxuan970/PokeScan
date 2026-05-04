from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from app.routers.collection import get_current_user_id
from app.services import collection as col_svc
from app.services import authenticity as auth_svc
from app.database import get_db

router = APIRouter(prefix="/detection", tags=["detection"])


class AuthenticityRequest(BaseModel):
    card_sku: str
    card_name: str
    set_number: str
    market_price: float
    listed_price: float
    scan_confidence: float = 1.0


class AuthenticityResponse(BaseModel):
    risk_level: str
    risk_score: float
    flags: list[str]
    recommendation: str


@router.post("/authenticity", response_model=AuthenticityResponse)
async def check_authenticity(
    body: AuthenticityRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user = await col_svc.get_or_create_user(db, user_id)
    if user.tier != "pro":
        raise HTTPException(status_code=403, detail="Pro tier required")

    result = auth_svc.check_authenticity(
        card_sku=body.card_sku,
        card_name=body.card_name,
        set_number=body.set_number,
        market_price=body.market_price,
        listed_price=body.listed_price,
        scan_confidence=body.scan_confidence,
    )
    return AuthenticityResponse(**result)
