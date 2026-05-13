"""
Opt-Out Agent

Verifies that once a user opts out of Pro features (no subscription or
cancelled subscription), the backend actually stops delivering Pro-tier data.

The key security invariant: ?tier=pro is only honoured when the request
carries a valid server JWT. Without it, the backend silently downgrades
to free tier (TCGPlayer only for EN, eBay for JP).

Feedback collected:
  - Does ?tier=pro without a JWT call eBay? (It must NOT.)
  - Does ?tier=pro with an invalid/expired JWT call eBay? (It must NOT.)
  - Does ?tier=pro with a valid JWT call eBay? (It MUST.)
  - Does JP pricing always use eBay regardless of tier/JWT? (It MUST.)
  - Does the free tier never call eBay for EN cards? (It must NOT.)
"""

from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

from app.main import app


# ---------------------------------------------------------------------------
# Tier downgrade security
# ---------------------------------------------------------------------------

class TestTierDowngradeSecurity:

    def test_pro_tier_without_token_falls_back_to_free(self, client, mocker):
        """`?tier=pro` without Bearer token must NOT call eBay."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                                return_value={"market_price": 5.00, "staleness_flag": False})
        mock_ebay = mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                                 return_value=6.00)

        resp = client.get("/price/sv1-025-198?tier=pro")

        assert resp.status_code == 200
        mock_ebay.assert_not_called()
        mock_tcg.assert_called_once()

    def test_pro_tier_with_invalid_token_falls_back_to_free(self, client, mocker):
        """`?tier=pro` with a garbage Bearer token must NOT call eBay."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                                return_value={"market_price": 5.00, "staleness_flag": False})
        mock_ebay = mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                                 return_value=6.00)

        resp = client.get(
            "/price/sv1-025-198?tier=pro",
            headers={"Authorization": "Bearer this.is.garbage"},
        )

        assert resp.status_code == 200
        mock_ebay.assert_not_called()

    def test_pro_tier_with_valid_token_calls_ebay(self, client, valid_token, mocker):
        """`?tier=pro` with a valid server JWT MUST call eBay for EN cards."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                                return_value={"market_price": 5.00, "staleness_flag": False})
        mock_ebay = mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                                 return_value=6.00)

        resp = client.get(
            "/price/sv1-025-198?tier=pro",
            headers={"Authorization": f"Bearer {valid_token}"},
        )

        assert resp.status_code == 200
        mock_ebay.assert_called_once()
        mock_tcg.assert_called_once()

    def test_pro_response_uses_aggregated_price_source(self, client, valid_token, mocker):
        """Pro tier with both sources returns price_source='aggregated'."""
        mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                     return_value={"market_price": 5.00, "staleness_flag": False})
        mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                     return_value=6.00)

        resp = client.get(
            "/price/sv1-025-198?tier=pro",
            headers={"Authorization": f"Bearer {valid_token}"},
        )

        assert resp.status_code == 200
        assert resp.json()["price_source"] == "aggregated"


# ---------------------------------------------------------------------------
# Free-tier enforcement (EN cards)
# ---------------------------------------------------------------------------

class TestFreeTierEnforcement:

    def test_free_tier_uses_tcgplayer_only(self, client, mocker):
        """Default `?tier=free` (no tier param) must NEVER call eBay for EN cards."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                                return_value={"market_price": 5.00, "staleness_flag": False})
        mock_ebay = mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                                 return_value=6.00)

        resp = client.get("/price/sv1-025-198")

        assert resp.status_code == 200
        mock_ebay.assert_not_called()
        mock_tcg.assert_called_once()

    def test_free_tier_explicit_param_also_blocks_ebay(self, client, mocker):
        """Explicit `?tier=free` must not call eBay even without JWT check."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                                return_value={"market_price": 5.00, "staleness_flag": False})
        mock_ebay = mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                                 return_value=6.00)

        resp = client.get("/price/sv1-025-198?tier=free")

        assert resp.status_code == 200
        mock_ebay.assert_not_called()

    def test_free_response_price_source_is_tcgplayer(self, client, mocker):
        """Free tier response names TCGPlayer as the price source."""
        mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                     return_value={"market_price": 5.00, "staleness_flag": False})
        mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                     return_value=6.00)

        resp = client.get("/price/sv1-025-198")
        data = resp.json()
        assert data["price_source"] == "tcgplayer"


# ---------------------------------------------------------------------------
# JP cards always use eBay (tier irrelevant)
# ---------------------------------------------------------------------------

class TestJPCardPricing:

    def test_jp_sku_always_calls_ebay(self, client, mocker):
        """JP SKUs bypass tier gate — eBay called regardless of JWT."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock)
        mock_ebay = mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock,
                                 return_value=8.00)

        resp = client.get("/price/base1-jp-006-102")

        assert resp.status_code == 200
        mock_ebay.assert_called_once()

    def test_jp_sku_never_calls_tcgplayer(self, client, mocker):
        """TCGPlayer must NOT be called for JP SKUs (no JP catalog)."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock)
        mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock, return_value=8.00)

        client.get("/price/base1-jp-006-102")

        mock_tcg.assert_not_called()

    def test_jp_sku_with_mid_hyphen_also_detected(self, client, mocker):
        """SKUs with '-jp-' in the middle are also detected as Japanese."""
        mock_tcg = mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock)
        mocker.patch("app.main.ebay_fetch", new_callable=AsyncMock, return_value=9.00)

        resp = client.get("/price/sv-jp-025-198")

        assert resp.status_code == 200
        mock_tcg.assert_not_called()


# ---------------------------------------------------------------------------
# Response contract
# ---------------------------------------------------------------------------

class TestResponseContract:

    def test_price_response_has_required_fields(self, client, mocker):
        """Price response must include all required fields regardless of tier."""
        mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                     return_value={"market_price": 5.00, "staleness_flag": False})

        resp = client.get("/price/sv1-025-198")
        assert resp.status_code == 200
        data = resp.json()
        required = {"card_sku", "market_price", "price_source", "is_completed_sale", "fetched_at"}
        missing = required - data.keys()
        assert not missing, f"Response missing fields: {missing}"

    def test_is_completed_sale_is_always_true(self, client, mocker):
        """is_completed_sale must always be True — we never use listing prices (G3)."""
        mocker.patch("app.main.tcg_fetch", new_callable=AsyncMock,
                     return_value={"market_price": 5.00, "staleness_flag": False})

        resp = client.get("/price/sv1-025-198")
        assert resp.json()["is_completed_sale"] is True

    def test_health_endpoint_always_accessible(self, client):
        """Health check must return 200 — no auth required."""
        resp = client.get("/health")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"
