import asyncio
from datetime import datetime, timezone, timedelta

import jwt
import urllib3 as _urllib3
from google.auth.transport import urllib3 as google_transport
from google.oauth2 import id_token as google_id_token
from jwt import PyJWKClient

from app.config import settings

_http = _urllib3.PoolManager()

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


async def verify_google_token(id_token: str) -> str:
    """Verifies Google ID token. Returns stable Google user ID (sub). Raises ValueError on failure."""
    last_exc: Exception | None = None
    for attempt in range(2):
        try:
            info = await asyncio.to_thread(
                google_id_token.verify_oauth2_token,
                id_token,
                google_transport.Request(_http),
                settings.google_client_id,
            )
            return info["sub"]
        except ValueError:
            raise  # bad token or wrong audience — no retry
        except Exception as exc:
            last_exc = exc
            if attempt == 0:
                await asyncio.sleep(0.6)
    raise ValueError(f"Google token verification failed: {last_exc}") from last_exc


async def warmup_google_auth() -> None:
    """Pre-warm the shared urllib3 pool used by verify_google_token. Best-effort."""
    try:
        await asyncio.to_thread(
            _http.request,
            "GET",
            "https://www.googleapis.com/oauth2/v3/certs",
            timeout=5.0,
        )
    except Exception:
        pass


def create_server_token(apple_user_id: str) -> str:
    """Issues a server JWT for the given user ID (Apple sub or Google sub)."""
    now = datetime.now(timezone.utc)
    payload = {
        "sub": apple_user_id,
        "iat": now,
        "exp": now + timedelta(days=365),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")


def decode_server_token(token: str) -> str:
    """Decodes and validates a server JWT. Returns apple_user_id (sub). Raises on invalid."""
    payload = jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])
    return payload["sub"]
