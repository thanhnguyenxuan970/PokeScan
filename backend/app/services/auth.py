import asyncio
import jwt
from jwt import PyJWKClient
from datetime import datetime, timezone, timedelta
from app.config import settings

APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys"
APPLE_ISS = "https://appleid.apple.com"

_jwks_client = PyJWKClient(APPLE_JWKS_URL)


async def verify_apple_token(identity_token: str) -> str:
    """
    Verifies Apple identity token. Returns the stable Apple user ID (sub claim).
    Raises ValueError on invalid token.
    PyJWKClient.get_signing_key_from_jwt makes a blocking HTTP request — run in thread pool.
    """
    try:
        signing_key = await asyncio.to_thread(
            _jwks_client.get_signing_key_from_jwt, identity_token
        )
        payload = jwt.decode(
            identity_token,
            signing_key.key,
            algorithms=["RS256"],
            audience=settings.apple_bundle_id,
            issuer=APPLE_ISS,
        )
        return payload["sub"]
    except Exception as exc:
        raise ValueError(f"Invalid Apple identity token: {exc}") from exc


def create_server_token(apple_user_id: str) -> str:
    """Issues a server JWT for the given Apple user ID."""
    payload = {
        "sub": apple_user_id,
        "iat": datetime.now(timezone.utc),
        "exp": datetime.now(timezone.utc) + timedelta(days=365),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")


def decode_server_token(token: str) -> str:
    """Decodes and validates a server JWT. Returns apple_user_id (sub). Raises on invalid."""
    payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
    return payload["sub"]
