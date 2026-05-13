"""
GDPR / Subject Access Agent

Mimics an EU/UK user exercising data rights:
  - Right to access: GET /collection must return exactly their cards, nothing else.
  - Right to erasure: DELETE /collection/{id} must remove the card.
  - Authentication enforcement: both endpoints must reject unauthenticated requests.
  - Free-tier cap: adding a 51st card must be rejected (403), not silently dropped.

Feedback collected:
  - Does GET /collection return 401 without a token?
  - Does DELETE /collection/{id} return 404 for a card that doesn't exist?
  - If a user has no cards, does GET return [] (not 404 or 500)?
  - Does the free-tier 50-card limit error message mention "50" so users understand why?
"""

from datetime import datetime, timezone
from unittest.mock import AsyncMock

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.database import get_db
from app.dependencies import get_current_user_id


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_user(tier: str = "free"):
    return type("User", (), {"id": "db-user-id", "tier": tier})()


def _make_card(card_id: str = "card-uuid-1", name: str = "Pikachu"):
    return type("Card", (), {
        "id": card_id,
        "card_sku": "sv1-025-198",
        "name": name,
        "set_code": "sv1",
        "set_number": "025/198",
        "language": "english",
        "market_price": 5.00,
        "price_source": "tcgplayer",
        "scanned_at": datetime.now(timezone.utc),
    })()


# ---------------------------------------------------------------------------
# Authentication enforcement
# ---------------------------------------------------------------------------

class TestAuthEnforcement:

    def test_list_collection_requires_auth(self, client):
        """GET /collection without Bearer token must return 401."""
        resp = client.get("/collection")
        assert resp.status_code == 401, f"Expected 401, got {resp.status_code}"

    def test_add_card_requires_auth(self, client):
        """POST /collection without Bearer token must return 401."""
        resp = client.post("/collection", json={
            "card_sku": "sv1-025-198",
            "name": "Pikachu",
            "scanned_at": datetime.now(timezone.utc).isoformat(),
        })
        assert resp.status_code == 401

    def test_delete_card_requires_auth(self, client):
        """DELETE /collection/{id} without Bearer token must return 401."""
        resp = client.delete("/collection/some-card-id")
        assert resp.status_code == 401

    def test_invalid_jwt_returns_401_on_collection(self, client):
        """Malformed Bearer token must return 401, not 500."""
        resp = client.get(
            "/collection",
            headers={"Authorization": "Bearer not.a.real.token"},
        )
        assert resp.status_code == 401


# ---------------------------------------------------------------------------
# Right to access
# ---------------------------------------------------------------------------

class TestRightToAccess:

    def test_authenticated_user_gets_empty_collection(self, authed_client, mocker):
        """Authenticated user with no cards gets [] — not 404."""
        mocker.patch("app.routers.collection.col_svc.get_or_create_user", new_callable=AsyncMock,
                     return_value=_make_user())
        mocker.patch("app.routers.collection.col_svc.list_cards", new_callable=AsyncMock,
                     return_value=[])

        resp = authed_client.get("/collection")
        assert resp.status_code == 200
        assert resp.json() == []

    def test_authenticated_user_gets_their_cards(self, authed_client, mocker):
        """Authenticated user with one card receives it in the response."""
        card = _make_card()
        mocker.patch("app.routers.collection.col_svc.get_or_create_user", new_callable=AsyncMock,
                     return_value=_make_user())
        mocker.patch("app.routers.collection.col_svc.list_cards", new_callable=AsyncMock,
                     return_value=[card])

        resp = authed_client.get("/collection")
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 1
        assert data[0]["name"] == "Pikachu"

    def test_collection_response_has_expected_fields(self, authed_client, mocker):
        """Each card in the collection response contains all required GDPR-relevant fields."""
        card = _make_card()
        mocker.patch("app.routers.collection.col_svc.get_or_create_user", new_callable=AsyncMock,
                     return_value=_make_user())
        mocker.patch("app.routers.collection.col_svc.list_cards", new_callable=AsyncMock,
                     return_value=[card])

        resp = authed_client.get("/collection")
        assert resp.status_code == 200
        item = resp.json()[0]
        required_fields = {"server_id", "card_sku", "name", "language", "scanned_at"}
        missing = required_fields - item.keys()
        assert not missing, f"Response missing GDPR fields: {missing}"


# ---------------------------------------------------------------------------
# Right to erasure
# ---------------------------------------------------------------------------

class TestRightToErasure:

    def test_user_can_delete_their_card(self, authed_client, mocker):
        """Authenticated user can delete a card they own."""
        mocker.patch("app.routers.collection.col_svc.get_user", new_callable=AsyncMock,
                     return_value=_make_user())
        mocker.patch("app.routers.collection.col_svc.soft_delete_card", new_callable=AsyncMock,
                     return_value=True)

        resp = authed_client.delete("/collection/card-id-123")
        assert resp.status_code == 200
        assert resp.json() == {"deleted": True}

    def test_delete_nonexistent_card_returns_404(self, authed_client, mocker):
        """Deleting a card that doesn't exist returns 404, not 500."""
        mocker.patch("app.routers.collection.col_svc.get_user", new_callable=AsyncMock,
                     return_value=_make_user())
        mocker.patch("app.routers.collection.col_svc.soft_delete_card", new_callable=AsyncMock,
                     return_value=False)

        resp = authed_client.delete("/collection/does-not-exist")
        assert resp.status_code == 404

    def test_delete_when_user_not_found_returns_404(self, authed_client, mocker):
        """If the user record doesn't exist at all, DELETE returns 404 not 500."""
        mocker.patch("app.routers.collection.col_svc.get_user", new_callable=AsyncMock,
                     return_value=None)

        resp = authed_client.delete("/collection/any-card-id")
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# Free-tier data limits
# ---------------------------------------------------------------------------

class TestFreeTierLimits:

    def test_free_tier_blocks_51st_card_with_403(self, authed_client, mocker):
        """Free tier cannot add a 51st card — returns 403 with clear error message."""
        mocker.patch("app.routers.collection.col_svc.get_or_create_user", new_callable=AsyncMock,
                     return_value=_make_user(tier="free"))
        mocker.patch("app.routers.collection.col_svc.count_active_cards", new_callable=AsyncMock,
                     return_value=50)

        resp = authed_client.post("/collection", json={
            "card_sku": "sv1-025-198",
            "name": "Pikachu",
            "scanned_at": datetime.now(timezone.utc).isoformat(),
        })
        assert resp.status_code == 403
        assert "50" in resp.json()["detail"], "Error message should mention the 50-card limit"

    def test_pro_tier_is_not_blocked_at_50_cards(self, authed_client, mocker):
        """Pro users are not blocked at 50 cards."""
        mocker.patch("app.routers.collection.col_svc.get_or_create_user", new_callable=AsyncMock,
                     return_value=_make_user(tier="pro"))
        mocker.patch("app.routers.collection.col_svc.count_active_cards", new_callable=AsyncMock,
                     return_value=50)
        mock_add = mocker.patch("app.routers.collection.col_svc.add_card", new_callable=AsyncMock,
                                return_value=_make_card())

        resp = authed_client.post("/collection", json={
            "card_sku": "sv1-025-198",
            "name": "Pikachu",
            "scanned_at": datetime.now(timezone.utc).isoformat(),
        })
        assert resp.status_code == 200
        mock_add.assert_called_once()

    def test_free_tier_can_add_card_49(self, authed_client, mocker):
        """Free tier user with 49 cards can still add one more."""
        mocker.patch("app.routers.collection.col_svc.get_or_create_user", new_callable=AsyncMock,
                     return_value=_make_user(tier="free"))
        mocker.patch("app.routers.collection.col_svc.count_active_cards", new_callable=AsyncMock,
                     return_value=49)
        mocker.patch("app.routers.collection.col_svc.add_card", new_callable=AsyncMock,
                     return_value=_make_card())

        resp = authed_client.post("/collection", json={
            "card_sku": "sv1-025-198",
            "name": "Pikachu",
            "scanned_at": datetime.now(timezone.utc).isoformat(),
        })
        assert resp.status_code == 200
