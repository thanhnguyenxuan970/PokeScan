from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from app.dependencies import get_current_user_id
from app.services import collection as col_svc
from app.services import authenticity as auth_svc
from app.database import get_db

router = APIRouter(prefix="/detection", tags=["detection"])


class ChecklistItem(BaseModel):
    id: str
    title: str
    description: str
    category: str


class ChecklistResponse(BaseModel):
    items: list[ChecklistItem]
    # Backward compat for iOS v1.x clients that decode risk_level/risk_score
    risk_level: str = "none"
    risk_score: float = 0.0


@router.post("/authenticity", response_model=ChecklistResponse)
async def get_authenticity_checklist(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db),
):
    user = await col_svc.get_or_create_user(db, user_id)
    if user.tier != "pro":
        raise HTTPException(status_code=403, detail="Pro tier required")

    items = [ChecklistItem(**item) for item in auth_svc.get_inspection_checklist()]
    return ChecklistResponse(items=items)
