import os

# Must be set before any app import — pydantic-settings reads env at Settings() construction
# and SQLAlchemy create_async_engine validates URL at module load time in database.py.
os.environ.setdefault("DATABASE_URL", "postgresql+asyncpg://test:test@localhost:5432/pokescan_test")
os.environ.setdefault("JWT_SECRET", "test-jwt-secret-minimum-32-chars-ok!")

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.database import get_db
from app.dependencies import get_current_user_id
from app.main import app
from app.services.auth import create_server_token

TEST_USER_ID = "test-user-001"
TEST_JWT_SECRET = "test-jwt-secret-minimum-32-chars-ok!"


@pytest.fixture(autouse=True)
def patch_jwt_secret(monkeypatch):
    """Force a known JWT secret for all tests — encode and decode use same key."""
    monkeypatch.setattr(settings, "jwt_secret", TEST_JWT_SECRET)


@pytest.fixture
def valid_token() -> str:
    return create_server_token(TEST_USER_ID)


@pytest.fixture
def mock_db_session(mocker):
    """AsyncMock session — injected via dependency_override so no real DB needed."""
    return mocker.AsyncMock()


@pytest.fixture
def db_override(mock_db_session):
    """Override get_db with a mock session that never connects to PostgreSQL."""
    async def _fake_db():
        yield mock_db_session

    app.dependency_overrides[get_db] = _fake_db
    yield mock_db_session
    app.dependency_overrides.pop(get_db, None)


@pytest.fixture
def auth_override():
    """Override authentication — returns TEST_USER_ID without validating a JWT."""
    app.dependency_overrides[get_current_user_id] = lambda: TEST_USER_ID
    yield TEST_USER_ID
    app.dependency_overrides.pop(get_current_user_id, None)


@pytest.fixture
def authed_client(db_override, auth_override):
    """TestClient with both DB and auth dependencies mocked — ready for collection tests."""
    return TestClient(app)


@pytest.fixture
def client():
    """Plain TestClient — no dependency overrides."""
    return TestClient(app)
