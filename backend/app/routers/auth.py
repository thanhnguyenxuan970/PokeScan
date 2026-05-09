from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from app.config import settings
from app.services.auth import verify_apple_token, verify_google_token, create_server_token, decode_server_token

router = APIRouter(prefix="/auth", tags=["auth"])


class AppleSignInRequest(BaseModel):
    identity_token: str
    apple_user_id: str


class AppleSignInResponse(BaseModel):
    token: str


@router.post("/apple", response_model=AppleSignInResponse)
async def sign_in_with_apple(body: AppleSignInRequest) -> AppleSignInResponse:
    """
    Verifies Apple identity token and returns a server JWT.
    """
    try:
        verified_user_id = await verify_apple_token(body.identity_token)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail=str(exc))

    if verified_user_id != body.apple_user_id:
        raise HTTPException(status_code=401, detail="User ID mismatch")

    server_token = create_server_token(verified_user_id)
    return AppleSignInResponse(token=server_token)


class GoogleSignInRequest(BaseModel):
    id_token: str


@router.post("/google", response_model=AppleSignInResponse)
async def sign_in_with_google(body: GoogleSignInRequest) -> AppleSignInResponse:
    """Verifies Google ID token and returns a server JWT."""
    try:
        user_id = await verify_google_token(body.id_token)
    except ValueError as exc:
        raise HTTPException(status_code=401, detail=str(exc))
    return AppleSignInResponse(token=create_server_token(user_id))


_bearer = HTTPBearer()


class VerifyReceiptRequest(BaseModel):
    product_id: str
    transaction_id: str


@router.post("/verify-receipt")
async def verify_receipt(
    body: VerifyReceiptRequest,
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
) -> dict:
    """
    Verifies a StoreKit transaction and marks the user as Pro.
    Full App Store Server API verification wired in Phase 3 infra setup.
    """
    try:
        apple_user_id = decode_server_token(credentials.credentials)
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")

    # Placeholder: accept any valid JWT + known product ID as Pro verification.
    # Phase 3 infra wires real App Store Server API receipt validation here.
    if not settings.apple_bundle_id:
        raise HTTPException(status_code=503, detail="Server misconfigured")
    valid_ids = {
        f"{settings.apple_bundle_id}.pro.monthly",
        f"{settings.apple_bundle_id}.pro.annual",
    }
    if body.product_id not in valid_ids:
        raise HTTPException(status_code=400, detail="Unknown product")

    return {"status": "pro", "apple_user_id": apple_user_id}


class AndroidVerifyReceiptRequest(BaseModel):
    product_id: str
    purchase_token: str


@router.post("/verify-receipt/android")
async def verify_android_receipt(
    body: AndroidVerifyReceiptRequest,
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
) -> dict:
    try:
        decode_server_token(credentials.credentials)
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")
    valid_ids = {
        "com.pokescan.app.pro.monthly",
        "com.pokescan.app.pro.annual",
    }
    if body.product_id not in valid_ids:
        raise HTTPException(status_code=400, detail="Unknown product")
    # Placeholder: any valid JWT + known product ID = active.
    # Wire real Google Play Developer API verification post-launch.
    return {"active": True}
