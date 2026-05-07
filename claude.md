# claude.md — Project Meta-Guide

## What This Project Is
Card scanning app (Pokémon, MTG, sports cards) → real-time market valuation.
**iOS build complete (paused — Apple Dev registration blocked). Android migration in progress.**
Stack: Kotlin + Jetpack Compose (Android, active) / SwiftUI (iOS, paused), FastAPI, PostgreSQL, TCGPlayer/eBay APIs.

---

## Android Migration Status (updated 2026-05-08)

### Why Android
Apple Developer registration errors unresolved. Google Play Console: $25 one-time fee, no approval queue. iOS code stays — resume when Apple Dev account resolves.

### Android Tech Stack
Kotlin + Jetpack Compose + Material 3, CameraX, ML Kit Text Recognition v2, Retrofit 2 + OkHttp + Moshi, Firebase Auth + Google Sign-In, EncryptedSharedPreferences, Room, DataStore, Play Billing 7+, Hilt, Kotlin Coroutines + Flow, Compose Navigation.

### Android Build Status

| Phase | Description | Files | Status |
|---|---|---|---|
| A0 | Project scaffolding — Gradle, DI stubs, theme, NavGraph | `android/` (21 files) | ✅ Done |
| A1 | Data layer — domain models, Room DB, SetResolver, SetDatabaseService, ScanCounterService | `android/` (15 items) | ✅ Done |
| A2 | Auth — SecureStorage, AuthInterceptor, ApiService, AuthRepository, AuthModule, AuthViewModel, SignInScreen, OnboardingScreen, NavGraph gating, MainActivity wiring | `android/` (9 new + 5 modified) | ✅ Done |
| A3 | Scanner — CameraX, ML Kit, ScannerViewModel, ScannerScreen | - | ❌ Next |
| A4 | Full features — networking, collection, billing, paywall | - | ❌ Not started |
| A5 | Polish — ProGuard, navigation gating, permission rationale | - | ❌ Not started |

### Next Session — Android
1. Get real `google-services.json` from Firebase Console — required before any Gradle build (`android/app/google-services.json`, replace `REPLACE_WITH_*` sentinels)
2. Plan + implement Phase A3 (Scanner — CameraX, ML Kit, ScannerViewModel, ScannerScreen)
3. After real `google-services.json` in place: run `./gradlew assembleDebug` and verify A2 build
4. Verify A2 nav flow: first launch → OnboardingScreen → SignInScreen; post-sign-in → ScannerScreen stays on reopen

### Key Decisions — Android Migration

| Decision | Rationale |
|---|---|
| `Theme.PokeScan` extends `android:Theme.Material.Light.NoActionBar` (not AppCompat) | Pure Compose app — no Views, no appcompat needed. Avoids extra dependency. |
| Placeholder `composable(Routes.SCANNER)` in NavGraph Phase 0 | Empty NavHost crashes on launch. Placeholder prevents crash, replaced Phase 3. |
| `android:allowBackup="false"` | PokeScan stores collection + financial data — local backup to Google Drive is a privacy risk. |
| All Gradle deps declared Phase 0 (not incrementally) | Phase 0 goal is "all deps wired." Declaring later-phase deps now locks versions early, avoids mid-development version conflicts. |
| `HttpLoggingInterceptor` gated on `BuildConfig.DEBUG` | Full HTTP body logging in release would expose JWTs and pricing responses. |
| `google-services.json` placeholder committed | Unblocks file structure, but Gradle build requires real file from Firebase Console. Placeholder has `REPLACE_WITH_*` sentinel strings. |
| `SetEntry` domain model has no Moshi annotation | Domain models must be infrastructure-free. Bundle + API parsing uses `SetEntryDto` (in `data/remote/dto/`), which maps to `SetEntry` via `toDomain()`. |
| `SetResolver.resolve()` takes `entries` as parameter | Enables reactive updates — caller passes current `SetDatabaseService.sets.value`. No singleton or constructor coupling. |
| `SetEntryDao.replaceAll()` uses `@Transaction` | Wraps `deleteAll()` + `upsertAll()` atomically. Without `@Transaction`, a crash between the two calls leaves Room empty with no recovery path until next API refresh. |
| `ScanCounterService` uses `Mutex` around read-check-write | `canScan` read and `recordScan` increment are not a single DataStore atomic operation. Mutex serializes concurrent callers and prevents two coroutines both passing the 20-scan limit check. |
| `SetDatabaseService` scope uses `SupervisorJob()` | Without `SupervisorJob`, an uncaught exception in any child coroutine cancels the parent scope, which kills the `stateIn` collector and stops `sets` from emitting forever. |
| Bundle `set_database.json` has no `printedTotal` field | All entries parse as `printedTotal=null`. `SetResolver` falls through to newest-wins for collisions until `pokemontcg.io` API refresh populates `printedTotal`. Tests cover both code paths explicitly. |
| `SecureStorage` uses `EncryptedSharedPreferences` (synchronous) | Token check at NavGraph `startDestination` computation must be sync — happens inside `remember {}` at composition time. No coroutine overhead for a single string read. |
| `AuthInterceptor` reads token from `SecureStorage` per-request | Token can be cleared after launch (sign-out). Live read per-request handles sign-out correctly. Captured-at-construction token would stay stale. |
| `NavGraph` receives `secureStorage` + `prefs` as params from `MainActivity` | NavGraph `startDestination` needs sync access to both. Both are synchronous (`EncryptedSharedPreferences` and `SharedPreferences`). No Hilt injection into composable needed — simpler. |
| Auth nav events via `SharedFlow<AuthEvent>(replay=0)` in `AuthViewModel` | `StateFlow` would replay the last event on recomposition (e.g. after screen rotation), causing double-navigation. `SharedFlow(replay=0)` fires once and done. |
| `material-icons-core` added to deps in A2 | `OnboardingScreen` uses `Icons.Default.*`. The artifact is NOT a transitive dep of `compose-material3` — must be declared explicitly. BOM-managed, no version conflict. |
| `RESULT_OK` guard in `SignInScreen` before `handleSignInResult` | User cancelling Google Sign-In returns `RESULT_CANCELED`. Without the guard, `task.result` throws `ApiException(SIGN_IN_CANCELLED)` and shows "Sign-in failed" error to the user. Cancellation should be silent. |

---

## iOS Build Status (updated 2026-05-06, pre-launch fixes applied)

### Completed

| Phase | Component | File | Status |
|---|---|---|---|
| 0 | Camera session + permissions | `Features/Scanner/CameraViewModel.swift` | ✅ Done |
| 0 | Scanner UI — reticle, state machine, button | `Features/Scanner/ScannerView.swift` | ✅ Done |
| 0 | Camera preview bridge | `Features/Scanner/CameraPreviewView.swift` | ✅ Done |
| 0 | Card model — SKU, multi-source price, language | `Models/Card.swift` | ✅ Done |
| 1 | Vision OCR delegate + frame throttle | `Services/VisionService.swift` | ✅ Done |
| 1 | Set number regex + card name + language detection | `Services/CardIdentificationService.swift` | ✅ Done |
| 1 | Pricing protocol + mock (simulates backend proxy) | `Services/PricingService.swift` | ✅ Done |
| 1 | Full state machine wired: scanning→detected→loading→result | `Features/Scanner/CameraViewModel.swift` | ✅ Done |
| 1 | Card detail sheet — name, set, SKU, price | `Features/CardDetail/CardDetailView.swift` | ✅ Done |
| 2 | Static set database (27 sets, total→setCode heuristic) | `Resources/set_database.json` + `Services/SetResolver.swift` | ✅ Done |
| 2 | Set code resolution wired into card identification | `Services/CardIdentificationService.swift` | ✅ Done |
| 2 | AppConfig — env-based backend URL + Vision level toggle | `Config/AppConfig.swift` | ✅ Done |
| 2 | Vision latency timing (DEBUG log) + `.accurate`/`.fast` toggle | `Services/VisionService.swift` | ✅ Done |
| 2 | FastAPI backend skeleton — `/health` + `/price/{card_sku}` (Phase 2 stub price) | `backend/app/` | ✅ Done |
| 2 | LivePricingService — URLSession → FastAPI, keeps MockPricingService for tests | `Services/PricingService.swift` | ✅ Done |
| 2 | Scan counter — 20/month free tier, UserDefaults + monthly reset | `Services/ScanCounterService.swift` | ✅ Done |
| 2 | Paywall sheet — fires on scan #21 attempt, single moment (G10) | `Features/Paywall/PaywallView.swift` | ✅ Done |
| 2 | CameraViewModel wired: LivePricingService + ScanCounterService + showPaywall | `Features/Scanner/CameraViewModel.swift` | ✅ Done |
| 3 | TCGPlayer live pricing — TTLCache, SKU→product_id, marketPrice | `backend/app/services/tcgplayer.py` | ✅ Done |
| 3 | eBay Finding API completed sales + weighted aggregator | `backend/app/services/ebay.py`, `aggregator.py` | ✅ Done |
| 3 | pokemontcg.io set DB refresh, base1/ex5 collision fix | `Services/SetDatabaseService.swift`, `SetResolver.swift` | ✅ Done |
| 3 | Sign in with Apple — Keychain JWT, restoreSession, backend verify | `Services/AuthService.swift`, `Features/Auth/SignInView.swift` | ✅ Done |
| 3 | Collection persistence — SwiftData + server-synced push/pull | `Persistence/CollectionStore.swift`, `Services/CollectionSyncService.swift` | ✅ Done |
| 3 | Collection backend routes — GET/POST/DELETE wired to PostgreSQL | `backend/app/routers/collection.py`, `database.py` | ✅ Done |
| 3 | StoreKit 2 Pro purchase, Transaction.updates, paywall wired | `Services/StoreKitService.swift`, `Features/Paywall/` | ✅ Done |
| 4 | Grade ROI backend — `POST /grading/roi`, condition→grade multipliers, break-even calc | `backend/app/services/grading_roi.py`, `routers/grading.py` | ✅ Done |
| 4 | Grade ROI iOS — condition/service picker, ROI results card, Pro gate | `Models/GradeROIModels.swift`, `Services/GradeROIService.swift`, `Features/GradeROI/` | ✅ Done |
| 4 | Fake detection backend — `POST /detection/authenticity`, rules-based scorer | `backend/app/services/authenticity.py`, `routers/detection.py` | ✅ Done |
| 4 | Fake detection iOS — risk badge in CardDetailView, detail sheet | `Models/AuthenticityResult.swift`, `Services/FakeDetectionService.swift` | ✅ Done |
| 4 | StoreKit IAP — real product IDs, purchasePending/purchaseError states, .storekit config | `Services/StoreKitService.swift`, `PokeScan.storekit` | ✅ Done |
| 4 | Backend deployment prep — Dockerfile, docker-compose (with PG healthcheck), CORS, rate limiting | `backend/Dockerfile`, `docker-compose.yml`, `.env.production.example` | ✅ Done |
| 5 | `app/dependencies.py` — extract `get_current_user_id` from `collection.py`, all 3 routers updated | `backend/app/dependencies.py` | ✅ Done |
| 5 | JP set database — 20 JP sets added (SV era + classic era), manual curation from Bulbapedia | `Resources/set_database.json` | ✅ Done |
| 5 | JP pricing path — JP SKUs skip TCGPlayer, use eBay-only; free+pro tiers both get eBay for JP | `backend/app/main.py` | ✅ Done |
| 5 | SetDatabaseService JP fix — removed `language: "english"` hardcode, infers from `-jp` suffix | `Services/SetDatabaseService.swift` | ✅ Done |
| pre | Auth product ID fix — `com.yourname.*` → dynamic from `settings.apple_bundle_id`; guard for empty bundle ID | `backend/app/routers/auth.py` | ✅ Done |
| android | `POST /auth/google` + `verify_google_token()` + `GOOGLE_CLIENT_ID` config + `google-auth>=2.29` dep | `backend/app/routers/auth.py`, `services/auth.py`, `config.py`, `requirements.txt` | ✅ Done |
| pre | `.env.production.example` — added `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `POSTGRES_USER/PASSWORD/DB` | `backend/.env.production.example` | ✅ Done |
| pre | docker-compose DB credentials — hardcoded `pokescan/pokescan` → `${POSTGRES_USER/PASSWORD/DB}` | `backend/docker-compose.yml` | ✅ Done |
| pre | `AppConfig.privacyPolicyURL` constant added; PP `Link` added to `PaywallView` (above Restore Purchases) | `Config/AppConfig.swift`, `Features/Paywall/PaywallView.swift` | ✅ Done |
| pre | `OnboardingView` — logo, 3 value prop rows, PP link, "Start Scanning" CTA | `Features/Onboarding/OnboardingView.swift` | ✅ Done |
| pre | `PokeScanApp` onboarding gate — `@AppStorage("hasSeenOnboarding")` shows OnboardingView on first launch | `App/PokeScanApp.swift` | ✅ Done |

### Stubs / Remaining

| Component | File | Phase | Status |
|---|---|---|---|
| Privacy Policy hosted URL — paste real UUID into `AppConfig.privacyPolicyURL` | `Config/AppConfig.swift` line 29 | Pre-launch | ⏸ Code done, needs real termly.io URL to replace `REPLACE_ME` |
| App Icon 1024×1024 PNG | `Assets.xcassets/AppIcon` in Xcode | Pre-launch | ❌ Not done — create in Canva, drag into Xcode |
| App Store Connect — create IAP products in dashboard | external | Pre-launch | ❌ Blocked: Apple Dev registration errors |
| Backend deploy to Railway/Fly.io | external | Pre-launch | ❌ Not done |
| eBay API keys | external | Pre-launch | ❌ Blocked: pending account approval |
| TCGPlayer API keys | external | Pre-launch | ⏸ Skipped for v1.0 — not self-service, requires email to api@tcgplayer.com |
| JWT_SECRET + Postgres credentials | `backend/.env` | Pre-launch | ❌ Not filled |
| E2E test after deploy | manual | Pre-launch | ❌ Not done |

---

## Next Session — Pre-launch (Updated 2026-05-07)

### Unblocked — Do Now

1. **Privacy Policy URL** — go to `termly.io`, generate free policy (Mobile App, "PokeScan"). Copy hosted URL. Replace `REPLACE_ME` in `Config/AppConfig.swift:29`. Paste URL into App Store Connect → App Information → Privacy Policy URL field.
2. **App Icon** — Canva → 1024×1024 PNG, no transparent background (Apple rejects). In Xcode Project Navigator: open `Assets.xcassets` → `AppIcon` → set Scales to "Single Scale" → drag PNG in.
3. **JWT_SECRET** — `openssl rand -hex 32` → paste into `backend/.env`. Set `POSTGRES_PASSWORD` to strong value. Update `DATABASE_URL` accordingly.

### Blocked — Waiting on External

5. **eBay API** — pending approval. When approved: fill `EBAY_APP_ID` + `EBAY_CERT_ID` in `backend/.env`, set `POKESCAN_USE_MOCK=0`.
6. **Apple Developer account** — registration errors unresolved. Required for: App Store Connect IAP setup, archiving, TestFlight. Resolve at `developer.apple.com`.
7. **TCGPlayer** — not self-service. Email `api@tcgplayer.com`. Not a launch blocker — app ships eBay-only pricing for v1.0.

### After All Unblocked

8. **App Store Connect** — create IAP products `com.pokescan.app.pro.monthly` ($4.99/mo) + `com.pokescan.app.pro.annual` ($39.99/yr), subscription group `pokescan_pro`.
9. **Deploy backend** — `docker-compose up --build` locally → Railway/Fly.io. Run `alembic upgrade head` against prod DB.
10. **Set `POKESCAN_ENV=production`** in Xcode release scheme before archiving.
11. **E2E test** — scan real card → price → Grade ROI → result. Test paywall → purchase → Pro unlocks. Scan JP card → eBay-only price.

---

## Backend — How to Run (Phase 3+)

```bash
cd backend
pip install -r requirements.txt

# Start PostgreSQL (Docker)
docker run -d --name pokescan-db \
  -e POSTGRES_USER=pokescan -e POSTGRES_PASSWORD=pokescan -e POSTGRES_DB=pokescan \
  -p 5432:5432 postgres:16

# Run migrations
alembic upgrade head

# Start server
uvicorn app.main:app --reload
# → http://localhost:8000/health
# → http://localhost:8000/price/{card_sku}
# → http://localhost:8000/collection  (requires Bearer JWT)
```

Env flags:
- `POKESCAN_USE_MOCK=1` → MockPricingService (no TCGPlayer/eBay calls)
- `POKESCAN_VISION_FAST=1` → Vision `.fast` mode for latency benchmarking

---

## Key Decisions Made (Phase 1)

| Decision | Rationale |
|---|---|
| `presentedCard` separate from `detectedCard` | Sheet must open only after price fetch, not on card detection. Two vars needed: one for mid-scan state display, one for sheet trigger. |
| `guard scanState == .loading else { return }` after price fetch | Prevents in-flight fetch Task from reopening sheet after user resets. |
| `VisionService.isProcessing` flag (not actor isolation) | Both `captureOutput` and Vision completion run on same serial `sessionQueue` — no race, no actor overhead needed. |
| `setCode = "unknown"` in Phase 1 | Real set resolution requires a set database (Pokémon TCG API mapping). Deferred to Phase 2; doesn't block scan flow. |
| Mock price 0.5–150 range | Wide range surfaces UI edge cases (sub-$1 display, 3-digit price). Real distribution is similar. |

## Key Decisions Made (Phase 2)

| Decision | Rationale |
|---|---|
| Static bundled `set_database.json` (not API call) | Offline resolution, zero latency, no network dep. Phase 3 adds pokemontcg.io refresh. Bundle is fallback. |
| `SetResolver` total→setCode heuristic (newest-wins) | Simple, deterministic. Known false positives: `base1`/`ex5` (both 102 cards) → resolves as Hidden Legends for vintage Base Set cards. Acceptable for Phase 2; fixed in Phase 3 with full API. |
| `LivePricingService` + `MockPricingService` kept side-by-side | `POKESCAN_USE_MOCK=1` env var enables mock without code change. Safe for CI and offline dev. |
| Phase 2 backend returns stub price ($4.99) | TCGPlayer SKU→product_id mapping requires catalog search (Phase 3). Stub proves URLSession pipeline end-to-end without blocking iOS wiring. |
| Paywall on `startScan()` guard, not after result | Cleanest UX: scan #20 completes without interruption, #21 attempt hits the gate. No mid-scan paywall. |
| `ScanCounterService.recordScan()` called only on `.result` | Failed detections and network errors don't consume a scan credit. Fair to user. |
| pydantic-settings v2 `model_config = SettingsConfigDict(...)` | `class Config` is pydantic v1 API, deprecated in v2. Avoids DeprecationWarning and future breakage. |

## Key Decisions Made (Phase 3)

| Decision | Rationale |
|---|---|
| `@MainActor class` (not `actor`) for `SetDatabaseService` | Swift actors cannot have `@Published`. `ObservableObject` requires a class. `@MainActor class` gives same isolation guarantee. |
| `SetResolver` DI via `init(entries:)`, removed singleton | `CameraViewModel` rebuilds `CardIdentificationService` + `SetResolver` via Combine when set DB refreshes. Singleton prevented live refresh. |
| StoreKit stub in T2, replaced in T6 | `PricingService.swift` references `StoreKitService.shared.isPro` at compile time. Stub (always `false`) unblocks T2 without T6 dependency. |
| `CollectionSyncService` reads JWT from Keychain directly | Avoids circular import with `AuthService`. Both share `KeychainKeys.serverToken` constant defined in `KeychainKeys.swift`. |
| `SignInWithAppleButton` + `handleAppleAuthorization(_:)` | `onCompletion:` receives `Result<ASAuthorization, Error>` internally — calling `auth.signIn()` inside causes double `ASAuthorizationController` crash. New public method forwards credential directly. |
| eBay Finding API uses `SECURITY-APPNAME` param, not OAuth Bearer | Finding API authenticates via app name query param. OAuth bearer is for Browse/Trading APIs only. Removed `get_bearer_token()` entirely. |
| `SELECT ... FOR UPDATE` in `get_or_create_user` | Serializes concurrent card inserts per user, preventing TOCTOU on free-tier 50-card limit. |
| `get_user` (no upsert) for DELETE route | Delete path should not create a user row. Separate `get_user` returns `None` → 404 cleanly. |
| `User.tier` needs both `server_default` and `default` | `server_default` is DB-level only; SQLAlchemy doesn't auto-refresh after flush. Python-level `default="free"` ensures in-memory object has correct tier for new users. |

## Key Decisions Made (Phase 4)

| Decision | Rationale |
|---|---|
| Grade multipliers hardcoded (not fetched from PSA pop report) | Phase 4 scope. Static multipliers (grade 10 = 4×, grade 9 = 2×, etc.) are directionally correct and sufficient for ROI decision. Dynamic multipliers require PSA API integration (Phase 5). |
| `_has_suspicious_chars` via codepoint comparison, not regex | Unicode regex with literal chars caused encoding corruption across platforms. Codepoint comparison is explicit, readable, and encoding-safe. |
| `listed_price` + `market_price` both sent to `/detection/authenticity` | Client sends market_price (from scan) and listed_price (price being evaluated). Backend compares them directly — no extra pricing API call needed per request. |
| `get_current_user_id` imported cross-router from `collection.py` | Function defined in collection router, not a shared module. Acceptable for Phase 4; refactor to `app/dependencies.py` before multi-developer work. |
| `@ObservedObject` for `StoreKitService.shared` in new views | Phase 3 used `@StateObject` in `PaywallView` (pre-existing, technically wrong). All new Phase 4 views use `@ObservedObject` — correct pattern for externally owned singletons. |
| `docker-compose` DB healthcheck + `depends_on: condition: service_healthy` | `depends_on: [db]` doesn't wait for PostgreSQL to accept connections. Without healthcheck, API crashes on startup race. |

---

## Key Decisions Made (Phase 5)

| Decision | Rationale |
|---|---|
| `get_current_user_id` extracted to `app/dependencies.py` | Was cross-imported from `collection.py` into `detection.py` + `grading.py`. Fragile. `dependencies.py` is the correct FastAPI pattern for shared auth logic. |
| JP set data manually curated (not from API) | `api.pokemontcg.io` is English-only — no `language=ja` filter exists. Static curation from Bulbapedia is one-time cost, zero runtime dep. |
| JP cards bypass tier gate in `aggregate()` | JP cards have no TCGPlayer data; tier distinction is meaningless. Passing `tier="pro"` to `aggregate()` when `is_japanese=True` gives eBay-only result for both free and pro JP users. Avoids `market_price: None` on free JP scans. |
| `SetDatabaseService` language infer from `-jp` suffix | `api.pokemontcg.io` never returns JP sets today, but fix is future-proofing. `contains("-jp")` chosen over `hasSuffix("-jp")` to catch potential mid-code variants. |
| JP set total collisions within JP language (e.g. base2-jp + base3-jp both 48) | `SetResolver` newest-wins tiebreaker applies within JP same-total sets, same as EN behavior. Acceptable Phase 5 limitation. |

## Key Decisions Made (Pre-launch)

| Decision | Rationale |
|---|---|
| Auth product IDs derived from `settings.apple_bundle_id` (not hardcoded) | Placeholder used `"com.yourname.pokescan.pro.*"` — wouldn't match iOS client's `"com.pokescan.app.pro.*"`. All Pro purchases would be rejected. Fix reads bundle ID from env var, zero hardcoded strings. |
| Guard `if not settings.apple_bundle_id` before building `valid_ids` | Empty default would produce `{".pro.monthly", ".pro.annual"}` — nonsensical but technically passable by a crafted request. 503 fails loudly on misconfiguration instead of silently degrading. |
| Docker-compose DB credentials via `${VAR}` (not hardcoded `pokescan/pokescan`) | Hardcoded credentials in docker-compose.yml = plaintext secrets in git. `${VAR}` reads from `.env` at compose time, zero code change for different envs. |
| TCGPlayer skipped for v1.0 launch | API not self-service — requires email to api@tcgplayer.com, approval takes weeks. eBay-only pricing sufficient for launch. TCGPlayer added post-launch when keys arrive. |
| `POKESCAN_USE_MOCK=1` until eBay keys arrive | Keeps app runnable locally during API approval wait. Flip to `0` when `EBAY_APP_ID` + `EBAY_CERT_ID` are filled. |
| Privacy Policy, App Icon, Onboarding identified as missing pre-launch requirements | Coin-app launch checklist analysis (2026-05-07) revealed 3 items not in original pre-launch plan. All required before App Store submission. |
| `privacyPolicyURL` as static `let` in `AppConfig` (not env var) | URL is public-facing and non-sensitive. Env var would add deploy complexity for zero security benefit. Placeholder `REPLACE_ME` in code; real UUID swapped before archive. |
| Onboarding gate via `@AppStorage` on `App` struct, not `UserDefaults` directly | `@AppStorage` is reactive — `PokeScanApp` body re-evaluates automatically when `hasSeenOnboarding` flips in `OnboardingView`. Direct `UserDefaults` read in `WindowGroup` would be non-reactive and require manual state bridging. |
| PP `Link` placed above "Restore Purchases" in `PaywallView` | Apple App Review guideline: legal links must be visible before any purchase action. Below "Restore" = after purchase UI = potential rejection. |
| Single `OnboardingView` (not multi-screen flow) | 3-screen carousel adds navigation state, page indicators, and gesture handling — week of work. Single screen with 3 feature rows conveys same value props in 30min. Revisit post-launch if retention data suggests onboarding drop-off. |

---

## Competitor Pain Points → Project Goals

Research source: App Store reviews across PokeScope, Dex, Acorn, Pokellector, TCGPlayer, TCG Card Scanner, Pokedata (2024–2025).

| # | Competitor Pain Point | Severity | Our Project Goal |
|---|---|---|---|
| P1 | **Reprint/variant misidentification** — apps confuse reprints (Celebrations Pikachu → original Base). Price delta can be 100×. | Critical | G1: Scan accuracy ≥97% on reprints/variants using set number + art hash disambiguation, not image-only |
| P2 | **Slow or unreliable scanner** — "kind of slow and the scanner doesn't work a lot of times." Fails in low-light or through binder sleeves. | High | G2: Scan-to-result latency ≤600ms on-device. Must work in dim light and through standard sleeves. |
| P3 | **Inaccurate pricing** — apps report $86 for a $0.20 card. Single-source pricing (TCGPlayer only) misses real market. | Critical | G3: Market price = weighted avg of TCGPlayer + eBay 30-day completed sales + CardMarket. Flag staleness if >24h. Never use listing price. |
| P4 | **No grading ROI at scan time** — users tab between 3+ sites to decide if a card is worth grading. No app delivers this in-flow. | High | G4: Grade ROI screen on every scan: condition slider → expected PSA grade → net profit after fee. Pop report inline. |
| P5 | **Paywalled scanner that doesn't work** — Dex charges $4/mo for a "beta-quality" scanner. Users call it fraud. | High | G5: Core scanner always free. Paywall only on portfolio analytics, Grade ROI, and price alerts. Never gate scan accuracy. |
| P6 | **No fake/counterfeit detection** — forgeries replicate holos. No app flags suspicious cards. SEA market particularly exposed. | Medium | G6: Fake-flag layer (premium): font weight + holo pattern hash + card number format check. "High risk" verdict triggers expert CTA. |
| P7 | **New sets added too slowly** — "the 2025 holiday calendar came out a month ago, still not in the app." | Medium | G7: Automated set ingestion pipeline. New sets live in app ≤48h after official Pokémon TCG API update. Zero manual step. |
| P8 | **No Japanese card support** — multiple apps English-only despite JP being 41.5% of TCG Pocket revenue. | Medium | G8: Japanese card support in v1.5. Same CV pipeline, JP card database via Pokémon TCG API + supplementary scrape. |
| P9 | **Collection data loss on crash/reinstall** — Dex and Pokellector users report hours of collection data wiped. | High | G9: Collection data persisted server-side (authenticated) with local cache. Never localStorage-only. Sync on every add. |
| P10 | **Aggressive paywall friction** — "constantly tries to get me to get premium insights." Users churn on push, not value gap. | Medium | G10: Single paywall moment: after scan #20 (free tier limit). No interstitials, no banner upsells, no nag screens. |

### Goal enforcement rule
Before any feature spec or code task: map it to G1–G10. If it doesn't address a goal, explicitly state why we're building it anyway or deprioritize.

---

## 6-Step Business Pipeline (Enforce on Every Decision)

Apply this pipeline whenever evaluating a feature, pricing decision, or scope change.

### Step 1 — Diagnose
Before building anything, define the problem precisely.
- What is the exact user failure mode? (Cite a review or data point.)
- Which competitor has this problem and why haven't they fixed it?
- What is the cost to the user of this problem existing? (Money lost, time wasted, trust broken.)
- Output: 1-sentence problem statement. No solution yet.

### Step 2 — Pick Tools
Choose the right tech for the diagnosed problem. Always evaluate:
- On-device vs server-side (privacy, latency, cost tradeoff)
- Build vs API (e.g. Vision framework vs OpenAI Vision — default to on-device unless accuracy gap >5%)
- Single source vs multi-source (pricing always multi-source per G3)
- Output: options table with tradeoffs + recommendation.

### Step 3 — Calculate ROI
Formula: `Hourly rate × hours/4 weeks × 12 months = annual value`
Apply to every feature build decision:
- Estimate engineering hours to build + maintain for 12 months
- Estimate revenue impact: conversion lift, churn reduction, or new tier unlock
- If ROI < 1× (costs more than it earns in year 1), defer or cut scope
- Example: Grade ROI screen — 40h build × $150/hr = $6,000 cost. If it converts 2% more free→Pro at 10K MAU ($4.99/mo) = $999/mo × 12 = $11,988 annual. ROI = 2×. Build it.

### Step 4 — Tiered Packaging
Every feature maps to a tier. Default logic:

| Tier | Price | What's in it |
|---|---|---|
| Free | $0 | 20 scans/mo, TCGPlayer price only, basic collection (50 cards) |
| Pro | $4.99/mo or $39/yr | Unlimited scans, all 3 markets, Grade ROI, price alerts, full collection |
| Shop | $29/mo | Bulk scan API, fake flag, branded reports, inventory export |

Rules:
- Core scanner accuracy (G1, G2) is NEVER paywalled — see G5.
- Grade ROI (G4) and fake detection (G6) are Pro/Shop gates.
- If a feature only matters to <5% of users, it's Shop tier or cut.

### Step 5 — Avoid Traps

**Underpricing:**
- Do not price Pro below $3.99/mo. At $2.99, conversion math breaks: you need 3× more users to hit the same MRR as a $8.99 app.
- Annual discount max 35% ($39/yr vs $59.88). Beyond that signals desperation.
- Never offer "lifetime" pricing before 1K paying users — no baseline churn data.

**Small retainers / scope creep:**
- Do not accept custom feature work for individual users or shops for <$500 flat or <$29/mo ongoing.
- Any custom integration (ERP, POS, custom grading API) = Shop Enterprise tier at $99+/mo minimum.
- If a user requests a feature not on the roadmap, log it. Build it when 10+ users request the same thing.

**Other traps:**
- Do not launch on Android before iOS v1.0 is profitable. Split focus kills both.
- Do not add a new TCG (MTG, One Piece) before Pokémon-only MAU exceeds 5K.
- Do not partner with PSA/CGC before community trust scores are verified — PSA scandal ongoing.

### Step 6 — Prototype & QA

**Prototype standard (Figma/ProtoPie):**
- Ship prototype to 5 target users before writing any Swift code for a new feature.
- Success threshold: ≥4/5 users reach the target action without prompting.
- If <4/5: iterate prototype. Do not proceed to build.

**QA gates before any release:**
- [ ] Scan accuracy ≥97% on reprint test set (30 cards: 10 reprints, 10 holos, 10 promos)
- [ ] Latency ≤600ms median on iPhone 13 (baseline device)
- [ ] Price delta vs manual TCGPlayer check ≤5% on 20 random cards
- [ ] Grade ROI output matches manual PSA pop + eBay calc within 10%
- [ ] No raw scan images written to disk or transmitted to server
- [ ] Collection data survives app kill + reinstall cycle
- [ ] Paywall appears exactly once at scan #21, not before

**Regression tests (run on every PR):**
- Scan 10 known cards from canonical test set. All 10 must match.
- Hit pricing endpoint for 5 cards. All must return within 800ms.
- Add 3 cards to collection, kill app, reopen — all 3 must be present.

---

## How to Use Claude Effectively Here

### Prompt Patterns That Work

**Architecture decision:**
```
Context: [current state]
Decision: [what needs deciding]
Constraints: [hard limits]
→ Give options table + recommendation
```

**Code task:**
```
File: [filename]
Goal: [exact behavior]
Existing code: [paste or describe]
→ Return complete function/class, no explanation
```

**Research/analysis:**
```
Question: [specific question]
Data available: [what I have]
Output needed: [table / bullets / number]
```

**Debugging:**
```
Error: [exact error]
Context: [what was expected]
Tried: [what failed]
```

**Pipeline check:**
```
Feature: [name]
Pipeline step: [1–6]
→ Output the required artifact for that step only
```

### Trigger Phrases
| Phrase | Claude behavior |
|---|---|
| `"options?"` | Return comparison table, pick a winner |
| `"quick"` | ≤5 bullets, no elaboration |
| `"deep dive"` | Full technical analysis, include edge cases |
| `"draft"` | First-pass doc/spec, I'll iterate |
| `"security check"` | Review for vulnerabilities + privacy violations |
| `"so what?"` | Force implication analysis on any topic |
| `"pipeline [1-6]"` | Run only that step of the 6-step pipeline |
| `"roi check"` | Run Step 3 only: hours × rate × 12 calculation |
| `"tier?"` | Map a feature to Free/Pro/Shop + justification |

---

## Context to Always Include

### For iOS/SwiftUI tasks
- Target iOS version (assume iOS 17+ unless stated)
- Whether feature is on-device or requires network
- Relevant existing models/services it integrates with

### For pricing/data tasks
- Card name + set + condition (PSA grade if known)
- Target API (TCGPlayer vs eBay vs PSA)
- Freshness requirement (real-time vs cached ok)

### For backend tasks
- Endpoint context (public vs authenticated)
- Expected load (scans/minute estimate)
- Whether response is sync or async/webhook

---

## What Claude Will Push Back On
- Storing raw scan images server-side
- Client-side API keys
- Pseudocode when real code was asked for
- Mixing foil/non-foil pricing without explicit reason
- Using listing price as "market price"
- Paywalling core scan accuracy (violates G5)
- Building a feature without mapping it to G1–G10 and Steps 1–6
- Pricing Pro below $3.99/mo (violates Step 5)
- Skipping prototype testing before Sprint 1 coding (violates Step 6)

---

## Project Glossary
| Term | Meaning |
|---|---|
| `card_sku` | Unique ID: name + set + language + variant (foil/non-foil) + condition |
| `market_price` | Weighted avg of last 30-day completed sales (TCGPlayer + eBay + CardMarket) |
| `grade` | PSA/BGS condition score 1–10, null if raw |
| `scan_session` | One user scan event — discarded after price returned |
| `price_staleness` | >24h since last market data refresh |
| `reprint_flag` | Card identified as potential reprint — requires set number confirmation |
| `grade_roi` | Net profit = (expected_grade_sell_price − raw_price − grading_fee) |
| `pipeline_step` | One of Steps 1–6 of the business pipeline |

---

## Anti-Patterns (Never Do)
- Never ask Claude to "build the whole app" in one prompt — break into components
- Never skip specifying card condition — prices swing 10x between PSA 7 and PSA 10
- Never treat eBay listing price as sold price — use completed sales filter only
- Never build a feature that doesn't survive Step 3 ROI check
- Never launch a tier without completing Step 4 packaging table for it
- Never gate core scan accuracy behind a paywall (G5, Step 5)