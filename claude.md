# claude.md — Project Meta-Guide

## What This Project Is
Card scanning app (Pokémon, MTG, sports cards) → real-time market valuation.
**iOS build complete (paused — Apple Dev registration blocked). Android migration in progress.**
Stack: Kotlin + Jetpack Compose (Android, active) / SwiftUI (iOS, paused), FastAPI, PostgreSQL, TCGPlayer/eBay APIs.

---

## Android Migration Status (updated 2026-05-17, dev workflow automation)

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
| A3 | Scanner — CameraX, ML Kit, ScannerViewModel, ScannerScreen | `android/` (7 new + 2 modified) | ✅ Done |
| A4 | Full features — networking, collection, billing, paywall | `android/` (10 new + 8 modified), `backend/app/routers/auth.py` | ✅ Done |
| A5 | Polish — ProGuard, navigation gating, permission rationale | `android/` (1 new + 9 modified) | ✅ Done |

### Dev Workflow — `android/dev.ps1` (added 2026-05-17)

Replaces the 5-command manual ADB loop. Run from `android/` directory.

```powershell
.\dev.ps1 install   # incremental build + install (keeps Room/SharedPrefs data)
.\dev.ps1 launch    # install + launch app
.\dev.ps1 watch     # auto-rebuild on any .kt/.xml/.json save (Ctrl+C to stop)
.\dev.ps1 connect   # check/fix ADB device connection (non-destructive)
```

Key: `.\gradlew.bat :app:installDebug` uses `adb install -r` (reinstall without uninstall) — preserves app data. Gradle daemon caches unchanged modules: ~15–30 s per incremental change. `watch` uses `FileSystemWatcher.WaitForChanged` with 2 s debounce.

### Next Session — Android (updated 2026-05-17, fix sign-out regression — latency + data race)

**Completed this session (2026-05-17) — Fix sign-out regression bugs (latency + multi-account data race):**
- ✅ `android/.../AuthRepository.kt` — push timeout reduced `3_000L` → `1_000L`; parallelized push completes in <1s on normal connection
- ✅ `android/.../AuthRepository.kt` — Google `signOut()` moved to `applicationScope.launch { withTimeoutOrNull(3_000L) }` (fire-and-forget); `signOut()` now returns in ~1s max (was ~5s)
- ✅ `android/.../AuthRepository.kt` — `applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` added as `@Singleton` class field; `clearToken()` + `deleteAll()` still run synchronously before return
- ✅ Race window reduced 5s → 1s; multi-account data wipe eliminated
- **Tests: all 100 passing (no test changes needed)**

### Next Session — Android (updated 2026-05-17, bug fixes — sign-out lag + session leak + UI overlap)

**Completed this session (2026-05-17) — Bug fixes — sign-out lag, cross-account session leak, scanner UI overlap:**
- ✅ `android/.../AuthRepository.kt` — `withTimeoutOrNull(3_000L)` wraps `pushPending()`; caps sign-out block at 3s (was unbounded N×~500ms per card)
- ✅ `android/.../AuthRepository.kt` — `withTimeoutOrNull(2_000L)` wraps Google `signOut()` GMS callback; total max sign-out ≤5s
- ✅ `android/.../AuthRepository.kt` — `deleteAllSynced()` → `deleteAll()`; fixes cross-account data leak where unsynced cards (serverID=null) persisted in Room and were pushed to new account's JWT on next login
- ✅ `android/.../ScannerScreen.kt` — `ScanCounterPill` modifier: `padding(top=60.dp)` → `.statusBarsPadding().padding(top=8.dp)`; pill now below status bar, no overlap with reticle
- ✅ `android/.../ScannerScreen.kt` — `ReticleOverlay` modifier: `padding(bottom=96.dp)` → `padding(top=80.dp, bottom=96.dp)`; reticle centers in lower portion, 24dp+ gap from pill on all screen sizes
- ✅ `android/CLAUDE.md` — 4 Key Decisions added for `deleteAll()`, `withTimeoutOrNull`, `statusBarsPadding`, and `padding(top=80.dp)` rationale
- **Tests: all 100 passing (no new tests needed — behavior covered by manual E2E verification)**

### Next Session — Android (updated 2026-05-17, bug fixes — sign-out perf + data persistence + scanner layout)

**Completed this session (2026-05-17) — Bug fixes — sign-out performance, data persistence, scanner UI layout:**
- ✅ `android/.../CollectionRepository.kt` — `pushPending()` parallelized with `coroutineScope { async }` + `awaitAll()`; N cards push concurrently instead of sequentially; sign-out time ≈ 1× network round-trip (was N×)
- ✅ `android/.../CardRecordDao.kt` — `deleteAllSynced()` added: `DELETE FROM card_records WHERE serverID IS NOT NULL`; only removes cards confirmed on server
- ✅ `android/.../AuthRepository.kt` — `signOut()` calls `deleteAllSynced()` instead of `deleteAll()`; unsynced cards survive sign-out in Room, pushed by `syncAll()` on next login
- ✅ `android/.../ScannerScreen.kt` — `ReticleOverlay` gets `Modifier.fillMaxSize().padding(bottom = 96.dp)`; reticle frame shifts up, clear 96dp gap between reticle bottom and Scan button
- **Tests: all passing, BUILD SUCCESSFUL**

### Next Session — Android (updated 2026-05-17, bug fixes — sync + paywall + delete UX)

**Completed this session (2026-05-17) — Bug fixes + test corrections:**
- ✅ `android/.../AuthRepository.kt` — `CollectionRepository` injected; `signOut()` calls `collectionRepository.pushPending()` as first action before `clearToken()` + `deleteAll()` + `resetCount()` + Google session clear; prevents collection loss on logout
- ✅ `android/.../AuthRepositoryTest.kt` (new) — `coVerifyOrder` test verifies `pushPending()` precedes `clearToken()` in `signOut()`
- ✅ `android/.../PaywallScreen.kt` — `windowInsetsPadding(WindowInsets.systemBars)` on outer `Box`; `IconButton` close moved AFTER `Column` (Compose z-order fix); "No ads" added as second entry in `proFeatures`
- ✅ `android/.../ScannerScreen.kt` — `ButtonDefaults.buttonColors(disabledContainerColor = Color.White.copy(alpha=0.15f), disabledContentColor = Color.White.copy(alpha=0.75f))` on `ScanButton`; "Scanning…" text now readable on `#0A0A0A` background
- ✅ `android/.../CollectionScreen.kt` — `SwipeToDismissBox` + `SwipeToDeleteCard` removed; `cardToDelete` state + `AlertDialog` added; `CardRow` gets trash `IconButton` + `onDeleteClick` param
- ✅ `android/test/.../SetResolverTest.kt` — all 11 assertions updated to `.setCode` (`SetResolver` returns `ResolvedSet`, not `String`)
- ✅ `android/test/.../ScanCounterServiceTest.kt` + `PowerUserAgentTest.kt` — `FREE_MONTHLY_LIMIT` assertions updated 20 → 10
- **Tests: 100 passing, 0 failures**

### Next Session — Android (updated 2026-05-17, UI/UX polish — camera bypass + reticle)

**Completed this session (2026-05-17) — Camera bypass + reticle polish:**
- ✅ `android/.../ScannerScreen.kt` — all camera permission logic removed (no permission request, no `CameraPreview`, no permission-denied fallback); dark background + reticle + scan UI always visible on Scanner tab entry; "Scan" button directly triggers ViewModel `startScan()` → 1.8s mock delay → `CardDetailSheet`
- ✅ `android/.../ScannerScreen.kt` — `ReticleOverlay` reticle border upgraded: `Color.Yellow` → `animateColorAsState` with `MaterialTheme.colorScheme.primary` (brand blue `#2563EB`) for Scanning state, `Color(0xFF22C55E)` (green-500) for Result state; border width 2dp → 3dp; `import androidx.compose.animation.animateColorAsState` added
- ✅ Swipe-to-delete (`SwipeToDismissBox` in `CollectionScreen.kt`) — confirmed already fully implemented; no changes needed

### Next Session — Android (updated 2026-05-16, mock scan bypass + scan count reset fix)

**Completed this session (2026-05-16) — Mock scan bypass + scan count state leak fix:**
- ✅ `android/.../ScannerScreen.kt` — `onTextDetected = {}` (no-op lambda); real ML Kit OCR frames no longer forwarded to ViewModel; only `startScan()` 1.8s mock path runs; eliminates `NoCardDetected` snackbar from real OCR interference
- ✅ `android/.../ScanCounterService.kt` — `suspend fun resetCount()` added; writes `SCAN_COUNT_KEY = 0` + `SCAN_RESET_DATE_KEY = now` to DataStore; called on sign-out to prevent counter bleed across sessions
- ✅ `android/.../AuthRepository.kt` — `ScanCounterService` injected via constructor; `signOut()` now calls `scanCounterService.resetCount()` before Google session clear; covers both auth user logout and guest sign-out paths (NavGraph `handleSignOut` routes both through `authRepository.signOut()`)

**Completed this session (2026-05-16) — Auth 401 fix + real OCR scan pipeline:**
- ✅ `backend/app/services/auth.py` — `verify_google_token()` wrapped in retry loop (max 2 attempts, 300ms sleep); only non-ValueError transport errors are retried; `ValueError` (bad token) still immediate 401; fixes cold urllib3 pool causing 401 on first sign-in after backend startup
- ✅ `android/.../AuthInterceptor.kt` — Bearer token no longer attached to `/auth/*` endpoints; guard: `token != null && !encodedPath.contains("/auth/")`; defensive fix per Key Decision
- ✅ `android/.../ScannerScreen.kt` — permanent permission denial handled: launcher callback checks `shouldShowRequestPermissionRationale()` after denial; if permanently denied → "Open Settings" button deep-links to app settings; if soft denial → "Grant Permission" button (existing behavior)
- ✅ `android/.../CameraPreviewComposable.kt` — real `ImageAnalysis` use case added alongside `Preview`; ML Kit `TextRecognition` client created via `remember` + closed via `DisposableEffect.onDispose`; `STRATEGY_KEEP_ONLY_LATEST` backpressure; `proxy.close()` in `addOnCompleteListener`; `onTextDetected: (List<String>) -> Unit` callback parameter wired to ViewModel
- ✅ `android/.../ScannerViewModel.kt` — mock Charizard replaced with real pipeline: `CardIdentificationService` + `PricingService` injected; `startScan()` sets 5s timeout in `scanJob`; `onFrameAnalyzed(lines)` called per frame with `isProcessing` flag guard; successful OCR → cancel timeout → `fetchPrice` → `ScanState.Result`; `NoCardDetected` on timeout or pricing error; scan counter + saveLocal unchanged

### Next Session — Android (updated 2026-05-15, physical device auth fix)

**Status note:** Firebase Google Sign-In unblocked. Real `google-services.json` with OAuth client in place. `strings.xml` placeholder removed. Debug cleartext HTTP override added for physical device. **Remaining user action**: add `DEBUG_BASE_URL=http://<LAN-IP>:8000/` to `android/local.properties`, then rebuild + reinstall.

**Completed this session (2026-05-16) — Mock scan flow + scan limit reduction:**
- ✅ `ScannerViewModel.kt` — `startScan()` replaced with 1.8s mock delay → random card from `MOCK_CARDS` (Charizard ex / Pikachu ex / Mewtwo ex); `isProcessing = true` set before `ScanState.Scanning` to close OCR race window; mock card gets fresh UUID + timestamps via `.copy()`; `scanCounterService.recordScan()` + `saveLocal()` fire-and-forget wired same as real pipeline; `CardLanguage` + `PriceSource` imports added; `onFrameAnalyzed()` preserved intact (real pipeline blocked by `isProcessing` flag; camera stays live)
- ✅ `ScanCounterService.kt` — `FREE_MONTHLY_LIMIT` changed from 20 → 10; paywall triggers on 11th scan attempt

**Completed this session (2026-05-16) — Camera permission + preview fixes:**
- ✅ `CameraPreviewComposable.kt` — full CameraX setup moved into `factory` block (runs once, not on every recompose); `onSurfaceProviderReady` callback removed; `ProcessCameraProvider` future `remember`-ed; self-contained composable, no external wiring needed
- ✅ `ScannerScreen.kt` — runtime `CAMERA` permission request added via `rememberLauncherForActivityResult(RequestPermission)`; auto-requested on first screen entry via `LaunchedEffect("permission")`; all scanner UI (preview + reticle + counter + scan button + snackbar + sheet) gated behind `hasCameraPermission`; permission-denied state shows full-screen fallback ("Camera access required" + "Grant Permission" button, no reticle overlay obscuring it)

**Completed this session (2026-05-16) — Logout UX + Auth Persistence fixes:**
- ✅ `CollectionScreen.kt` — `showAuthSignOutDialog` state added; new `AlertDialog` for authenticated users ("Your collection is saved to your account...") with "Sign Out" (error color) + "Cancel"; logout `IconButton` now routes to this dialog instead of calling `onSignOut()` directly; guest path unchanged
- ✅ `AuthRepository.kt` — `GoogleSignInClient` injected into constructor (Hilt auto-wires from existing `@Singleton` in `AuthModule`); `signOut()` now calls `googleSignInClient.signOut()` via `suspendCancellableCoroutine` before returning — Google session cleared on logout, account picker shown on next sign-in attempt

**Completed this session (2026-05-15) — Physical device Google Auth fix:**
- ✅ `android/app/src/debug/res/xml/network_security_config.xml` created — `<base-config cleartextTrafficPermitted="true">` allows cleartext HTTP to any host in debug builds; source-set overlay replaces main config for debug, release unaffected
- ⏸ `android/local.properties` — user must add `DEBUG_BASE_URL=http://<LAN-IP>:8000/` (find IP: `ipconfig | Select-String "IPv4"`); backend must run `--host 0.0.0.0`

**Completed this session (2026-05-14) — UI/UX Sync & Bug Fixes:**
- ✅ Prototype alignment — `OnboardingScreen`: tagline → "Scan any Pokémon card. Know its real value. Instantly.", feature icons → emoji (⚡ $ ★), titles/descriptions match prototype, CTA → "Get Started", Privacy Policy link moved before CTA
- ✅ Prototype alignment — `SignInScreen`: heading → "Sign in to PokeScan", subtitle → "Sync your collection across devices.", Google button styled as `OutlinedButton` with Google G circle + "Continue with Google", Guest → `OutlinedButton`, terms footer added
- ✅ Prototype alignment — `PaywallScreen`: title → "Unlock Pro", feature checklist added (5 items with ✓), close button (X) at top-right, "Not now" button removed, `verticalScroll` added
- ✅ Dev Login removed — `BuildConfig.DEBUG` "Skip Auth (Dev)" block deleted from `SignInScreen`
- ✅ Auth bug diagnosed — root cause: `REPLACE_WITH_WEB_CLIENT_ID` placeholder in `strings.xml` → `idToken = null` → never navigates; added Logcat logging to `AuthViewModel` to trace exact failure point; `extraBufferCapacity = 1` prevents nav event drop on slow LaunchedEffect start
- ✅ Privacy Policy 404 identified — `https://thanhnguyenxuan970.github.io/pokescan-privacy` doesn't exist; URL is correct in `AppConfig.kt`; requires user to create GitHub Pages repo `pokescan-privacy`

**Completed this session (2026-05-14) — Fixes & enhancements:**
- ✅ Google Sign-in dev bypass — `BuildConfig.DEBUG` "Skip Auth (Dev)" `TextButton` in `SignInScreen`; calls `onAuthSuccess()` directly so full post-login flow testable without Firebase config
- ✅ Onboarding first-launch fix — `hasSeenOnboarding = true` moved from button-tap lambda to `LaunchedEffect(Unit)` in NavGraph ONBOARDING composable; force-close before tapping no longer re-shows onboarding
- ✅ Mock scan result — `triggerMockScan()` in `ScannerViewModel` + DEBUG bug-icon `IconButton` (top-right, camera view) in `ScannerScreen`; tapping shows Charizard ex/$45.99 CardDetailSheet after 800ms
- ✅ Guest sign-out warning — `AlertDialog` in `CollectionScreen` when `isGuest=true`; tapping Logout shows "Your scanned cards will be lost" with "Sign Out" + "Create Account" options; `isGuest` threaded NavGraph → `MainScreen` → `CollectionScreen`

**Completed this session (2026-05-14) — UI/auth polish:**
- ✅ Onboarding contrast fix — `ValuePropRow` removed `Surface(surfaceVariant)` wrapper; now plain `Row` with `Modifier.border()` + `clip()`; description text color `onSurfaceVariant` → `onSurface` (dark, high contrast on white bg)
- ✅ Branding — "PokeScan" title in `OnboardingScreen` + `SignInScreen` now `FontWeight.Black` + `MaterialTheme.colorScheme.primary` (brand blue `#2563EB`), matching icon visual weight
- ✅ Auth loop fix (Issue 3B) — `NavGraph` unauthorized-event collector now guards: skips navigate-to-SIGN_IN if already on `SIGN_IN` or `ONBOARDING`; `CollectionViewModel.syncAll()` guarded behind `secureStorage.getToken() != null` — no 401 storm on guest/unauthenticated launch
- ✅ Guest mode — "Continue as Guest" `TextButton` added to `SignInScreen`; `isGuest` SharedPreferences flag wired into `NavGraph` startDestination + `handleSignOut`; guest → MAIN with no sync; sign-out clears flag

**Completed previous session (2026-05-14):**
- ✅ Agent test suite: 9 Android + 2 Python backend test files, all passing
  - `libs.versions.toml` — added missing `test-junit`, `test-mockk`, `test-coroutines` entries (build was failing with unresolved reference)
  - `build.gradle.kts` — added `testOptions { unitTests.isReturnDefaultValues = true }` (fixes `android.util.Log` stubs throwing in JVM tests)
- ✅ `@PlainOkHttpClient` qualifier — `SetDatabaseService` now gets an unauthenticated OkHttpClient; was incorrectly sharing the `AuthInterceptor`-equipped client
- ✅ `collectAsStateWithLifecycle` — replaced `collectAsState()` in `SignInScreen`, `CollectionScreen`, and `PaywallScreen`; added `lifecycle-runtime-compose` dep to toml + `build.gradle.kts`
- ✅ `PaywallScreen` offer detection — `firstOrNull { offerTags.contains("base-plan") }` + `pricingPhaseList.lastOrNull()` (shows base price, not free-trial "$0.00")
- ✅ `PaywallScreen` PP URL crash guard — `isNotBlank()` before `Uri.parse()` prevents crash on placeholder URL
- ✅ App icon — adaptive icon XML done (`ic_launcher_foreground.xml` Pokéball + scan-beam design, `ic_launcher_background.xml` #FAFAFA, `mipmap-anydpi-v26/*.xml`)
- ✅ Full `check_code` review: 1 CRITICAL + 4 WARNING + 4 INFO found and fixed across 5 files; final verification cycle clean

**Completed this session (2026-05-15) — OnboardingScreen redesign + SignIn polish:**
- ✅ `OnboardingScreen`: title `displaySmall` → `headlineLarge`; subtitle "Instantly." accented in `primary` color via `buildAnnotatedString`; `ValuePropRow` param renamed `iconTint` → `iconColor`; default `iconContainerColor` kept `primaryContainer`, ⚡ and $ rows explicitly pass `primary` (solid blue)
- ✅ `SignInScreen`: fake Google G circle replaced with real `ic_google` drawable (`tint = Color.Unspecified` preserves brand colors); `TermsFooter` rewritten as `ClickableText` with inline `pushStringAnnotation` spans (TOS + PP clickable separately)
- ✅ `network_security_config.xml` added — `cleartextTrafficPermitted=true` scoped to `10.0.2.2`/`localhost`/`127.0.0.1` only; wired into `AndroidManifest.xml`
- ⚠️ `TermsFooter` TOS link routes to `AppConfig.PRIVACY_POLICY_URL` (TODO — no ToS URL yet); misleading until resolved

**Completed this session (2026-05-15) — CardDetailSheet redesign + Card model expansion:**
- ✅ T1 — `Card.kt` + `CardRecordEntity.kt`: 8 nullable fields added (`tcgPlayerPrice`, `ebayPrice`, `variant`, `setName`, `setYear`, `isAuthentic`, `priceUpdatedAt`, `gradeRoiPsaGrade`, `gradeRoiSellValue`, `gradeRoiNetProfit`); `toDomain()` + `toEntity()` updated; `scannedAt` default `0L` → `System.currentTimeMillis()`
- ✅ T1 — `AppDatabase.kt`: version 1→3; manual `MIGRATION_1_2` + `MIGRATION_2_3` companion object vals; `DatabaseModule` wired
- ✅ T2 — `SetResolver.kt`: `resolve()` now returns `ResolvedSet(setCode, setName, releaseYear)` instead of `String`; `CardIdentificationService.kt` destructures `ResolvedSet`, passes `setName`/`setYear` into `IdentifiedCard`; OCR set-number regex accepts `l`/`|` as `/`; `noiseLineRegex` filters HP/copyright/trainer noise lines
- ✅ T3 — `PricingService.kt`: `fetchPrice()` threads `tcgPlayerPrice`, `ebayPrice`, `setName`, `setYear`, `priceUpdatedAt` into `Card`
- ✅ T4 — `ScannerViewModel.kt`: Charizard mock has full `gradeRoi*` fields; 3-card random mock pool; `scanTimeoutJob` cancellation in `resetScan()` + `triggerMockScan()`; `ScanEvent.NoCardDetected` added; 5s scan timeout
- ✅ T5 — `CardDetailSheet.kt`: set subtitle uses `setName`/`setYear`; Holo + Authentic chips; `"MARKET PRICE · 30-DAY"` label; source+age subtitle; `TCGPLAYER`/`EBAY SOLD` grid; ROI gated on `isPro && gradeRoiPsaGrade != null`; `RoiStatCell` green param; `Button("View Collection")` + `OutlinedButton("Scan another")`
- ✅ T6 — `AuthViewModel.kt`: `idToken == null` → explicit error (`BuildConfig.DEBUG` shows Firebase config hint; release shows generic message)
- ✅ `ScannerScreen.kt`: `SnackbarHostState` added; `ScanEvent.NoCardDetected` → snackbar "No card detected. Try again."
- ✅ Firebase OAuth unblocked: real `google-services.json` placed; `strings.xml` placeholder removed

**Completed this session (2026-05-15) — SignIn bug fixes:**
- ✅ Guest button always visible — `OutlinedButton("Continue as Guest")` hoisted outside `when (val s = state)` block in `SignInScreen.kt`; now renders in `Idle`, `Error`, AND `Loading` states; was hidden when `AuthState.Error` hit
- ✅ CLEARTEXT error UX — `AuthViewModel.handleSignInResult` backend catch block now classifies network errors (CLEARTEXT, Unable to resolve host, Failed to connect, timeout, SocketException); release builds show "Unable to connect. Check your connection and try again." instead of raw OkHttp/Android error string; debug builds still see raw error for diagnostics

**Completed this session (2026-05-15) — Screen pixel-match to reference screenshots:**
- ✅ `SignInScreen` + `OnboardingScreen`: bare `Image(ic_launcher_foreground)` wrapped in `Box(size=88dp, clip=RoundedCornerShape(16dp), background=#FAFAFA)`; gives adaptive-icon look (white rounded-rect + scan-corner marks) matching screenshots
- ✅ `OnboardingScreen` ⚡ row: `iconContainerColor` `primary` → `primaryContainer` (light tonal blue container, dark bolt via `onPrimaryContainer`)
- ✅ `OnboardingScreen` ★ row: `iconContainerColor` `Color(0x1AF59B0B)` (10% opacity, near-invisible) → `Color(0xFFFEF3C7)` (opaque light amber, Tailwind amber-100 equivalent)
- ✅ `OnboardingScreen` `ValuePropRow` description: `onSurface` (near-black) → `onSurfaceVariant` (medium gray, lighter than title, matching screenshots)

**Completed this session (2026-05-15) — Mock scan flow + CardDetailSheet bugfixes:**
- ✅ `ScannerViewModel.kt` rewritten — stripped CameraX/ML Kit; `startScan()` → 1800ms delay → `ScanState.Result(Charizard)`; `ScanState` simplified to 3 states (Idle/Scanning/Result); `scansThisMonth` Flow exposed; `saveLocal` fire-and-forget wrapped in `try/catch` + `Log.w`
- ✅ `ScannerScreen.kt` rewritten — removed camera permission + `CameraPreview`; dark `#0A0A0A` background; `ScanCounterPill` top-left ("X/20 scans"); `SnackbarHost` for `NoCardDetected` event; `CardDetailSheet` shown on `ScanState.Result`
- ✅ `CardDetailSheet.kt` double drag handle fixed — `dragHandle = {}` on `ModalBottomSheet` suppresses M3 default; custom 36×4dp pill is now the only handle
- ✅ `CardDetailSheet.kt` `hoursAgo` overflow fixed — `.coerceAtLeast(0)` before `.toInt()` prevents wrap on zero/ancient `priceUpdatedAt`
- ✅ `CardDetailSheet.kt` leading ` · ` in price subtitle fixed — rebuilt with `listOfNotNull(sourceLabel, updatedSuffix?.let { "updated $it" }).joinToString(" · ")`
- ✅ `CardDetailSheet.kt` "Save to Collection" → "View Collection" — card already saved in `startScan()` before sheet opens; button navigates, not saves
- ✅ Build verified: `assembleDebug` → BUILD SUCCESSFUL after all changes

**Completed this session (2026-05-15) — Physical device UI/Auth fixes + code review:**
- ✅ `OnboardingScreen.kt` L69: `MaterialTheme.colorScheme.onBackground` → `Color.Black` for "Poke" span — `onBackground = PokeScanWhite` in dark scheme → text invisible on device in system dark mode
- ✅ `Theme.kt`: `darkTheme: Boolean = isSystemInDarkTheme()` → `false` — force light mode globally; dark theme not designed; removed now-unused `isSystemInDarkTheme` import
- ✅ `SignInScreen.kt` L97–104: plain `Text("Sign in to PokeScan")` → `buildAnnotatedString` two-tone heading ("Poke" `Color.Black` + "Scan" `primary` blue)
- ✅ `CardDetailSheet.kt` L290: "View Collection" → "Save to Collection" — prototype Image #2 match; reversal of prior session's rename
- ✅ `AuthViewModel.kt` L71–75: removed `!BuildConfig.DEBUG` guard — friendly network error always shown on device; raw message retained in Logcat via `Log.e`
- ✅ `AuthViewModel.kt` L42–47: applied same network error classification to `task.result` catch block — same IOException can surface at Google Sign-In layer before backend call
- ✅ `OnboardingScreen.kt`: inlined `goldColor` val (was declared, used once at ★ row)
- ✅ `onViewCollection` → `onSaveToCollection` rename propagated across `CardDetailSheet.kt`, `ScannerScreen.kt`, `NavGraph.kt`
- ✅ Full `check_code` (2 cycles) + `caveman-review` clean — 0 issues in final cycle; `assembleDebug` → BUILD SUCCESSFUL

**Step 1 — Unblock OAuth** ✅ Done (2026-05-15)
- Real `google-services.json` downloaded from Firebase Console (OAuth `client_type: 3` entry present)
- SHA-1 fingerprint registered in Firebase Console for debug keystore
- `strings.xml` `REPLACE_WITH_WEB_CLIENT_ID` placeholder removed — Firebase Gradle plugin now auto-generates `default_web_client_id`

**Step 1a — Privacy Policy** (user action, 15 min)
- Create GitHub repo `pokescan-privacy` under `thanhnguyenxuan970`, add `index.html` with privacy policy, enable GitHub Pages → URL `https://thanhnguyenxuan970.github.io/pokescan-privacy` goes live (already wired in `AppConfig.kt`)

**Step 1b — local.properties** (if testing on physical device via WSL)
- Add `DEBUG_BASE_URL=http://<your-LAN-IP>:8000/` to `android/local.properties` (gitignored)
- Emulator default `10.0.2.2:8000` works without this

**Step 2 — Rebuild APK**
```bash
cd android && ./gradlew assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

**Step 3 — Device E2E test** (run local backend first)
```bash
# PowerShell: $env:POKESCAN_USE_MOCK=1; uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
# bash: POKESCAN_USE_MOCK=1 uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
- First launch → Onboarding → SignIn (Google) → MainScreen
- Scan → price result → card in Collection
- 21st scan → PaywallScreen → test purchase → auto-dismiss
- Kill + relaunch → cards persist; swipe-to-delete works
- Simulate 401 → auto-navigate to SignIn; logout → collection empty
- Guest mode: tap "Continue as Guest" → MainScreen, Collection empty (no sync error)
- Guest persistence: kill + relaunch → stays in MainScreen (isGuest=true), no re-prompt
- Guest sign-out: tap Logout → AlertDialog with warning → "Sign Out" clears isGuest + lands on SignIn; "Create Account" navigates to SignIn without clearing Room data
- DEBUG mock scan: tap bug icon (top-right, camera view) → yellow reticle → CardDetailSheet "Charizard ex / $45.99" after 800ms

**Step 4 — Fix all bugs** — no bypasses

**Step 5 — Deploy backend** (must precede release build)
- Deploy to Railway or Fly.io; run `alembic upgrade head`
- Update base URL in `NetworkModule.kt` to prod URL

**Step 6 — Release build + signing keystore**
- Generate keystore (store outside repo): `keytool -genkey -v -keystore ~/pokescan-release.jks ...`
- Set env vars `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (signingConfigs already wired in build.gradle.kts)
- `./gradlew assembleRelease` — verify R8 clean, no `REPLACE_WITH_*` in APK

**Step 7 — Google Play Console submission** (play.google.com/console)
- Upload signed AAB; fill store listing + content rating
- Set up IAPs: `com.pokescan.app.pro.monthly` ($4.99) + `com.pokescan.app.pro.annual` ($39.99)

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
| `ProcessCameraProvider.getInstance().addListener()` (not `awaitInstance`) | CameraX 1.3.4 doesn't have `awaitInstance()` (added in 1.4+). `awaitInstance` requires coroutines-guava dep not in project. Listener on `getMainExecutor()` is correct for 1.3.x. |
| `@Volatile isProcessing` flag (not coroutine channel) | ML Kit analyzer callback runs on a single-threaded executor. `@Volatile` provides visibility guarantee without Mutex overhead. State transitions handle the semantic guard (`!is Scanning`). |
| 4-rect Canvas draw for reticle dim overlay | `BlendMode.Clear` on Canvas requires `CompositingStrategy.Offscreen` — its import path changed across Compose versions. Drawing 4 filled rects around the reticle hole is portable and always correct. |
| `ScanButton` "Scan Another" calls `resetScan()` not `startScan()` | `startScan()` guards on `state is Idle`. Calling it from `Result` state returns immediately — button would appear active but do nothing. Separate `onReset` callback routes to the correct action. |
| `ML Kit flatMap { it.lines }` not `map { it }` on textBlocks | `TextBlock.text` may contain embedded newlines — one block → one string with `\n`. `flatMap { it.lines }.map { it.text }` gives individual line strings, matching what the set number regex expects. |
| `isCameraStarted` guard in `startCamera` | `CameraPreview` `AndroidView.update` block can fire on each recomposition. Without the guard, multiple `ProcessCameraProvider.bindToLifecycle()` calls would rebind the camera on every recompose — causing flicker and resource waste. |
| `ScannerViewModel` catches exception from `cameraProviderFuture.get()` | `ListenableFuture.get()` can throw if the camera provider initialization fails (device restriction, permissions race). Without catch, the app crashes. Reset `isCameraStarted = false` to allow retry. |
| `BillingRepository` is `@Singleton` plain class, not ViewModel | Shared `isPro: StateFlow<Boolean>` across ScannerViewModel + PaywallViewModel. Singleton survives recomposition. `CoroutineScope(SupervisorJob() + Dispatchers.IO)` instead of viewModelScope. |
| `queryAndVerifyEntitlements()` on billing setup (not `restorePurchases()`) | Cold start restore uses local Play Billing cache — no network call, near-instant. `restorePurchases()` (user-triggered, with server verify) is separate. |
| Outer + inner NavHost (not single NavHost with conditional bottom bar) | Auth screens and Paywall must not show bottom nav. Conditional visibility is fragile; nested NavHost is clean. startDestination → MAIN (not SCANNER) after auth. |
| `saveLocal()` fires as fire-and-forget after `ScanState.Result` | Card in Room before user can close sheet — no data loss if dismiss is fast. Nested `viewModelScope.launch` creates a sibling coroutine, not a child of the existing launch. |
| `syncAll()` fire-and-forget in `CollectionViewModel.init` | No loading spinner in A4. Network errors logged only, retry on next launch. |
| `material-icons-extended` (not `material-icons-core`) for bottom nav icons | `CameraAlt` and `Style` icons are NOT in material-icons-core. Switched to extended in `libs.versions.toml`. R8 tree-shakes unused icons in release builds. |
| `scanned_at` serialized as ISO 8601 string in DTOs | `Instant.ofEpochMilli(ms).toString()` → ISO 8601. Safe on minSdk 26 (`java.time.Instant` available). Backend uses datetime field. |
| No interface for CollectionRepository or BillingRepository | Concrete `@Singleton @Inject constructor` — Hilt auto-wires. RepositoryModule unchanged. |
| `SwipeToDismissBox` (not deprecated `SwipeToDismiss`) for collection delete | Compose BOM 2024.09.00 = M3 1.3.0. Old `SwipeToDismiss` + `rememberDismissState` deprecated. `SwipeToDismissBox` + `rememberSwipeToDismissBoxState` is the current API. |
| `POST /auth/verify-receipt/android` added as separate endpoint | Existing `/auth/verify-receipt` is iOS-only (checks `apple_bundle_id`, expects `transaction_id`). Android needs `purchase_token`. Separate endpoint avoids breaking iOS flow. |
| `AuthEventBus` `SharedFlow<Unit>(replay=0, extraBufferCapacity=1)` for 401 events | `replay=0` prevents re-navigation on NavGraph recomposition (e.g. screen rotation). `extraBufferCapacity=1` + `tryEmit` lets OkHttp background thread emit without suspending. Single-consumer pattern — NavGraph `LaunchedEffect` is the only collector. |
| 401 guard: `!request.url.encodedPath.contains("auth/")` in `AuthInterceptor` | Without the guard, a 401 from `POST /auth/google` (bad token at sign-in) would emit `unauthorizedEvents` → NavGraph navigates to SignIn while already on SignIn → broken back stack. Guard scopes 401 handling to authenticated endpoints only. |
| `AuthEventBus` injected into `AuthInterceptor` (not `NavGraph`) | `AuthInterceptor` is OkHttp-layer; `NavGraph` is Compose-layer. Injecting the bus into the interceptor keeps the flow direction clean: OkHttp → bus → NavGraph. Reverse injection would create circular Hilt dependency (NetworkModule → NavGraph). |
| `signOut()` purges `card_records` but NOT `set_entries` | `card_records` is user PII (scanned collection). `set_entries` is reference data (Pokémon set catalog, no user linkage). Purging reference data on sign-out would cause a blank set database for the next user — unnecessary. |
| `isShrinkResources = true` requires `isMinifyEnabled = true` | Android build system enforces this constraint. R8 must run first to shrink code; resource shrinker then removes resources referenced only by removed code. Both flags set in release block. |
| `tier=pro` validated server-side via optional Bearer JWT | `?tier=pro` query param was unauthenticated — any client could receive Pro pricing for free. `HTTPBearer(auto_error=False)` allows the param but forces `tier=free` if JWT is missing or invalid. No breaking change to free-tier clients. |
| JP SKU detection uses delimiter-aware check (`endswith("-jp")` or `"-jp-" in sku`) | `"jp" in sku.lower()` matched unrelated SKUs (e.g. "jumper-001-150"). Delimiter-aware check eliminates false positives without regex overhead. |
| `queryAndVerifyEntitlements()` now server-verifies receipt on cold start | Previously trusted Play Billing cache only — refunded subscriptions persisted `isPro=true` for hours. Now mirrors `restorePurchases()` logic: server receipt verify required before granting Pro. |
| `onBillingServiceDisconnected` reconnects immediately | Was a no-op comment. Without reconnect, any billing service disruption (OS kill, GC) permanently breaks purchase + entitlement flows until app restart. |
| `acknowledgePurchase` result logged on non-OK codes | `BillingResult` was silently discarded. Non-OK results now emit `Log.w` — surfaced in Logcat for debugging without crashing (Play auto-acknowledges eventually). |
| `DEBUG_BASE_URL` read from `local.properties` (not hardcoded) | WSL LAN IP (`172.19.208.x`) is machine-specific — hardcoding breaks every other dev machine. `local.properties` is gitignored; emulator fallback `10.0.2.2:8000` used when file absent. |
| `signingConfigs` reads keystore from env vars (not `gradle.properties`) | Keystore path + passwords in `gradle.properties` = plaintext secrets in version control. Env vars keep secrets out of repo, compatible with CI/CD. |
| `android/local.properties` added to root `.gitignore` | Was untracked but not ignored — would have been accidentally committed with machine-specific LAN IP. File contains no secrets but is not portable across machines. |
| `@PlainOkHttpClient` qualifier for `SetDatabaseService` | `SetDatabaseService` calls `api.pokemontcg.io` — unauthenticated public endpoint. Sharing the `AuthInterceptor`-equipped client attached a Bearer JWT to every set DB refresh, which is unnecessary and fails if token is absent on first launch. |
| `collectAsStateWithLifecycle` instead of `collectAsState` in screens | Lifecycle-aware — stops collecting when composable is below STARTED state. Prevents recompositions and potential crashes when app is backgrounded. Requires `lifecycle-runtime-compose` dep (separate from `lifecycle-viewmodel-compose`). |
| `pricingPhaseList.lastOrNull()` for subscription price display | Play Billing phases are ordered chronologically (free trial → intro → base). `firstOrNull()` showed "$0.00" for free-trial SKUs. `lastOrNull()` always shows the regular recurring price. |
| `subscriptionOfferDetails.firstOrNull { offerTags.contains("base-plan") }` | Targets the base-plan offer token explicitly. `firstOrNull()` without filter could pick a free-trial offer token for purchase, billing the wrong phase. Falls through to `lastOrNull()` if tag absent. |
| `testOptions { unitTests.isReturnDefaultValues = true }` in `build.gradle.kts` | `android.util.Log` stubs throw `RuntimeException("Stub!")` in JVM unit tests by default. This flag makes all android stubs return zero/null/false — required for tests that exercise code paths with `Log.w()` (e.g., `CollectionRepository` catch blocks). |
| Test library aliases in `libs.versions.toml` | `build.gradle.kts` referenced `libs.test.junit/mockk/coroutines` but the toml had no such entries. All test builds failed with unresolved reference. Entries must be declared explicitly — not auto-generated from `testImplementation()` calls. |
| Adaptive icon XML (not PNG raster) for app icon | Vector adaptive icon scales perfectly on all densities, no need for multiple mipmap-* PNG assets. `minSdk=26` guarantees adaptive icon support. Pokéball + scan-beam design conveys app purpose at a glance. |
| `Box(clip+background)` wraps `ic_launcher_foreground` in both Onboarding + SignIn screens | `ic_launcher_foreground` includes the Pokéball + scan-corner marks but has no background. Wrapping in a `Box` with `#FAFAFA` background + `RoundedCornerShape(16dp)` gives the adaptive-icon look (white rounded-rect container) matching the prototype. Modifier order `clip → background` required: clip creates shape boundary, background fills it; children also clipped to rounded rect. |
| `OnboardingScreen` ⚡ row uses `primaryContainer` not `primary` | `primary` (solid blue) made the first feature row too heavy. `primaryContainer` gives the light tonal blue shown in the prototype. `Surface(color=primaryContainer)` sets `LocalContentColor = onPrimaryContainer` (dark navy); emoji ⚡ renders with native color regardless, but container contrast is correct. |
| `OnboardingScreen` ★ row uses `Color(0xFFFEF3C7)` (fully opaque light amber) | Previous `Color(0x1AF59B0B)` at 10% opacity was near-invisible on white background. `0xFFFEF3C7` is Tailwind amber-100 equivalent — fully opaque, warm, and readable. `iconColor = Color(0xFFF59B0B)` explicitly sets gold star regardless of `LocalContentColor` from the custom-color Surface. |
| `ValuePropRow` description uses `onSurfaceVariant` not `onSurface` | `onSurface` in Material3 is near-black — made description same visual weight as title. Prototype shows descriptions as clearly lighter secondary text. `onSurfaceVariant` (medium gray) gives correct two-level hierarchy. Was previously changed to `onSurface` for contrast; reverted to match prototype after pixel comparison. |

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
| `ValuePropRow` uses `Modifier.border()` + `clip()` on plain `Row`, no `Surface(surfaceVariant)` | `surfaceVariant` on `#FAFAFA` white produces near-invisible cards (low contrast). Border-only approach gives card shape with zero background fill. `onSurface` for description text ensures WCAG contrast. |
| "PokeScan" title uses `FontWeight.Black` + `colorScheme.primary` in both Onboarding and SignIn | SVG icon uses bold geometry + brand blue `#2563EB` as visual identity. System-default weight at theme color didn't read as brand mark — Black weight matches icon's visual mass. |
| `NavGraph` unauthorized-event collector guards current route before navigating to SIGN_IN | Without guard, 401 from `CollectionViewModel.syncAll()` (fires on MAIN entry) re-navigated to SIGN_IN immediately after successful sign-in — appeared as an auth loop. Guard: skip if already on SIGN_IN or ONBOARDING. |
| `CollectionViewModel.syncAll()` guarded by `secureStorage.getToken() != null` | Guests and unauthenticated restarts have no token — calling `syncAll()` would hit backend unauthenticated, get 401, trigger `unauthorizedEvents`, loop. Guard skips sync entirely when no token; Room local data still observed via `observeAll()`. |
| Guest mode uses `SharedPreferences("isGuest")` flag, not a separate auth state | Guest is a UI-layer concept only — no server session, no token. Reuses existing MAIN route; `isGuest=true` in startDestination check bypasses SIGN_IN on relaunch. Cleared on sign-out and on successful Google sign-in. |
| `BuildConfig.DEBUG` "Skip Auth (Dev)" button calls `onAuthSuccess()` directly | Placeholder `REPLACE_WITH_WEB_CLIENT_ID` → `idToken == null` → real Google Sign-In never completes. DEBUG bypass lets the full post-login flow be tested without Firebase config. Stripped from release builds by R8. |
| `hasSeenOnboarding = true` written in `LaunchedEffect(Unit)`, not button-tap lambda | Writing on button tap → force-close before tapping → flag never written → onboarding shown again next launch. Writing on screen entry (LaunchedEffect fires on first composition) is idempotent and survives force-close. |
| `triggerMockScan()` uses `UUID.randomUUID()` for mock card `id` | Hardcoded `"mock-001"` → second mock scan same session upserts same Room record, which is fine, but a unique id makes each mock scan traceable in the collection list during dev. |
| `triggerMockScan()` guards `_state.value !is ScanState.Scanning` after `delay(800)` | Without guard, calling `resetScan()` during delay brings state back to Idle; after delay, `ScanState.Result` would override Idle and show the mock sheet unexpectedly. Guard mirrors pattern in `handleOcrResult()`. |
| Guest sign-out `AlertDialog` threaded via `isGuest: Boolean` param (not read inside CollectionScreen) | Reading `prefs` directly inside `CollectionScreen` would couple the UI layer to `SharedPreferences`. Param injection keeps CollectionScreen testable and consistent with the existing NavGraph → MainScreen → CollectionScreen prop-passing pattern. |

## Key Decisions Made (UI/UX Sync 2026-05-14)

| Decision | Rationale |
|---|---|
| Onboarding emoji icons (⚡ $ ★) instead of Material icons | Prototype (L924/L931/L938) uses emoji in feat-icon divs. Emoji renders consistently without icon library dependency. Box + Surface background gives same shaped container. |
| `ValuePropRow` icon param `String` not `ImageVector` | Emoji-as-string is simpler than adding new Material icon for ★. No `material-icons-extended` size increase. |
| `SignInButton` → `OutlinedButton` + Google G circle + "Continue with Google" | Prototype (L961–L963) shows `.google-btn` with Google G logo. OutlinedButton matches white-bg + border style. Inline `Box(CircleShape, Color(0xFF4285F4))` renders Google blue G without asset import. |
| Guest button `TextButton` → `OutlinedButton` | Prototype (L965) uses `.btn-secondary` (outlined style). TextButton was visually lower hierarchy than warranted. |
| Terms footer combines ToS + PP into single `TextButton` link | No ToS URL defined — NEEDS CONFIRMATION on separate URL. Single combined button avoids placeholder dead link. Uses `PRIVACY_POLICY_URL` for both until ToS URL is confirmed. |
| Privacy Policy link order: before CTA in Onboarding | Prototype (L945–947) shows pp link before the Get Started button. Keeps Apple-style "legal before action" ordering. |
| `PaywallScreen` close button replaces "Not now" TextButton | Prototype has X close button at top-right. Top-right X is standard modal close pattern; bottom TextButton was non-standard and added scroll depth. |
| `PaywallScreen` adds `verticalScroll` | Feature list + plan buttons + footer now overflow on small screens. ScrollState is stable across recompositions via `rememberScrollState`. |
| `AuthViewModel._events` `extraBufferCapacity = 1` | `SharedFlow(replay=0)` drops events emitted before LaunchedEffect collector starts. `extraBufferCapacity=1` buffers one nav event without replay risk (buffer consumed on first collect, not replayed). |
| "Skip Auth (Dev)" removed entirely | No longer needed: DEBUG mock scan (bug icon) already tests scanner flow without auth. Keeping debug auth bypass creates UX divergence between debug and release — removed to keep UI parity. |
| Auth bug root cause: `REPLACE_WITH_WEB_CLIENT_ID` placeholder | `idToken == null` → never emits NavigateToScanner → stuck on SignInScreen. Not a code bug. Logcat logging added to `AuthViewModel` to surface exact failure point. Fix = Firebase setup (user action). |
| `strings.xml` manual `default_web_client_id` entry must be REMOVED after Firebase setup | Firebase Gradle plugin auto-generates this resource from `google-services.json`. Manual entry causes duplicate resource compile error. Entry exists now only as placeholder. |

## Key Decisions Made (CardDetail + Room migration 2026-05-15)

| Decision | Rationale |
|---|---|
| Manual `MIGRATION_1_2` + `MIGRATION_2_3` instead of `AutoMigration` | `exportSchema = true` requires schema JSON files to exist in `android/app/schemas/` before AutoMigration spec can reference them. Manual migrations avoid Gradle schema-file dependency and are explicit about the SQL. |
| `ResolvedSet(setCode, setName, releaseYear)` replaces `String` return from `SetResolver.resolve()` | Enables setName/setYear passthrough from OCR → `IdentifiedCard` → `PricingService` → `Card` without adding a parallel lookup or changing `IdentifiedCard`'s interface. Single-call resolution. |
| OCR set-number regex accepts `l`/`\|` as `/` | OCR (ML Kit) sometimes reads `/` as lowercase `l` or `\|`. Additive change to character class — doesn't break existing EN detection, adds robustness for common misread. |
| `noiseLineRegex` uses `\bTrainer\b` word boundary | Bare `Trainer` match would incorrectly filter card names like "Trainer's Choice". Word boundary limits match to standalone token. Same pattern applied to `\bItem\b`, `\bSupporter\b`, `\bStadium\b`. |
| `BuildConfig.DEBUG` branch in `AuthViewModel` null-idToken error | Release users see generic "Sign-in failed" (no config detail leakage). Debug builds show exact cause ("Firebase not configured — set up google-services.json") to aid dev. |
| `SnackbarHostState` for `NoCardDetected` (not `AlertDialog` or `Toast`) | Snackbar is non-blocking — camera stays live, user can immediately retry without dismissing a modal. Toast is deprecated API. AlertDialog would interrupt the scan flow. |

## Key Decisions Made (OnboardingScreen + SignIn polish 2026-05-15)

| Decision | Rationale |
|---|---|
| `iconColor` renamed from `iconTint` in `ValuePropRow` | `iconTint` implied color transformation (like `ImageVector` tinting). Param applies to emoji strings where color is irrelevant — `iconColor` is a more honest name. |
| Default `iconContainerColor = primaryContainer`; callers pass `primary` explicitly | Changing the default to `primary` would make `ValuePropRow` unsafe to reuse elsewhere (any caller not passing an override gets solid blue). Explicit call-site is safer API contract. |
| `Color.White` dropped from `$` row | `Surface(color = primary)` propagates `LocalContentColor = onPrimary` (white). Explicit `Color.White` was redundant — `Text(color = Color.Unspecified)` already inherits white via LocalContentColor. |
| `ic_google` drawable replaces fake "G" `Box` in `GoogleSignInButton` | Fake circle with "G" text is off-brand. Real SVG drawable with `tint = Color.Unspecified` preserves Google's brand colors without extra assets. |
| `ClickableText` with `pushStringAnnotation` for `TermsFooter` | Single `TextButton` couldn't route TOS and PP to different URLs. `ClickableText` gives per-span tap targets. `ClickableText` is soft-deprecated but stable on current Compose BOM. |
| TOS annotation routes to `PRIVACY_POLICY_URL` until ToS URL exists | Placeholder avoids a dead link crash. ⚠️ Must be replaced when ToS URL is created — tapping "Terms of Service" currently opens Privacy Policy. |
| `network_security_config.xml` scoped to dev hosts only | `cleartextTrafficPermitted=true` for `10.0.2.2`/`localhost`/`127.0.0.1` only. Prod domains always use HTTPS. File is untracked — must be staged explicitly before next commit. |

## Key Decisions Made (SignIn bug fixes 2026-05-15)

| Decision | Rationale |
|---|---|
| Guest button hoisted outside `when (state)` block | Button was inside `else` branch only — disappeared on `AuthState.Error`. Moving it outside renders it in all states. Tapping Guest during `AuthState.Loading` is safe: `onGuestMode` navigates away, ViewModel clears, `viewModelScope` cancels the in-flight coroutine. |
| Network error classified by substring match on `e.message` (not exception type) | OkHttp and Android throw `IOException` for both CLEARTEXT and DNS failures — same type, different messages. Substring match on known Android/OkHttp error strings is the only way to distinguish them without adding a typed wrapper around every Retrofit call. |
| Release shows user-friendly message; debug shows raw error | Raw CLEARTEXT message tells devs exactly what to fix (missing config). Users don't need Android framework internals — "Unable to connect" is actionable. `BuildConfig.DEBUG` gate keeps both. |

## Key Decisions Made (Mock scan flow + CardDetailSheet bugfixes 2026-05-15)

| Decision | Rationale |
|---|---|
| `ScanState` simplified to 3 states (Idle/Scanning/Result) | `Detected` and `Loading` were unreachable in mock flow (YAGNI). Simplification removes dead `when()` branches and makes state machine readable. Restore when real OCR pipeline is wired back in. |
| Mock `startScan()` — `delay(1_800)` not `delay(800)` | Prototype specifies 1.8s scan animation. Prior `triggerMockScan()` used 800ms (was a dev shortcut). Unified to match prototype fidelity. |
| `saveLocal` wrapped in `try/catch(Exception) { Log.w }` in sibling coroutine | Sibling `viewModelScope.launch` exceptions are not propagated to parent — they would be silently swallowed. `Log.w` surfaces errors in Logcat without crashing or blocking the scan result from showing. |
| `dragHandle = {}` on `ModalBottomSheet` | M3 `ModalBottomSheet` renders its own 32×4dp handle by default. `CardDetailSheet` had a custom 36×4dp Box handle inside the Column — this caused two handles to display. `dragHandle = {}` suppresses the M3 default; custom Box is the only handle. |
| `listOfNotNull(...).joinToString(" · ")` for price subtitle | `"${sourceLabel ?: ""}$updatedSuffix"` produced ` · updated Xh ago` (leading ` · `) when `sourceLabel = null`. `listOfNotNull` drops nulls before joining — no leading separator possible. |
| "View Collection" replaces "Save to Collection" | Card is saved via `saveLocal()` in `startScan()` before `ScanState.Result` is set — sheet opens after save is already in flight. "Save to Collection" implied the button triggers the save, which is wrong. "View Collection" accurately describes the navigation action. |
| `hoursAgo` clamped with `.coerceAtLeast(0)` | `((currentMs - priceUpdatedAt) / 3_600_000).toInt()` wraps to `Int.MIN_VALUE` if `priceUpdatedAt` is `0L` or far-future. Clamp produces `0` → displays "just now" instead of overflowed garbage. |

## Key Decisions Made (Physical device UI/Auth fixes 2026-05-15)

| Decision | Rationale |
|---|---|
| `Color.Black` hardcoded for "Poke" span in both Onboarding + SignIn | `MaterialTheme.colorScheme.onBackground` resolves to `PokeScanWhite` in dark scheme — white text on white/light card background is invisible. `Color.Black` is explicit and correct regardless of scheme. Paired with `darkTheme=false` for belt-and-suspenders. |
| `darkTheme: Boolean = false` (force light mode globally) | Device was in system dark mode. Dark color scheme not designed — only light tokens are defined per spec. `isSystemInDarkTheme()` was pulling untested dark colors. Forced light until dark theme assets are designed and tested. |
| "Save to Collection" label restored (reverting prior "View Collection") | Prototype Image #2 explicitly shows "Save to Collection". Prior rename to "View Collection" was semantically accurate (save fires in `startScan()`) but diverged from spec. Prototype is authoritative for UI copy; code comment documents the actual behavior. |
| Removed `!BuildConfig.DEBUG` guard on network error message in backend catch | Debug APK on physical device was showing raw OkHttp error string. Classification (`isNetworkError`) was already correct — only the `DEBUG` branch bypassed it. Removing guard: raw error stays in Logcat via `Log.e`, UI always shows friendly string. |
| Network classification applied to `task.result` catch (not just backend catch) | `task.result` throws `ApiException` on network failure at Google Sign-In layer — same IOException class, same message patterns. Without classification, device network errors surfaced as raw `ApiException` message at the first catch block before reaching the backend catch. |
| `onViewCollection` → `onSaveToCollection` rename across 3 files | Callback name must match button label. After label reverted to "Save to Collection", `onViewCollection` was semantically wrong and would confuse future readers tracing the nav callback. Renamed in `CardDetailSheet`, `ScannerScreen`, `NavGraph`. |
| `src/debug/res/xml/network_security_config.xml` with `<base-config cleartextTrafficPermitted="true">` | Physical device can't reach `10.0.2.2:8000` (emulator-only address). LAN IPs (192.168.x.x) were also blocked by the main `network_security_config.xml` (only 3 explicit hosts allowed cleartext). Debug source-set overlay replaces the main config for debug builds — release stays HTTPS-only. |

## Key Decisions Made (Camera permission + preview 2026-05-16)

| Decision | Rationale |
|---|---|
| `CameraPreview` self-contained — no `onSurfaceProviderReady` callback | Callback was invoked in `update` block which fires every recomposition → repeated `bindToLifecycle` calls → flicker. Moving full CameraX setup into `factory` (runs once per `AndroidView` lifetime) eliminates the re-bind bug at the correct layer. |
| `ProcessCameraProvider` future wrapped in `remember` | `ProcessCameraProvider.getInstance(context)` starts an async initialization. `remember` ensures it's created once per composition entry, not on every recompose. |
| All scanner UI gated behind `hasCameraPermission` (not just `CameraPreview`) | Showing reticle overlay + scan button over a permission-denied message creates confusing UI — dim overlay obscures the message. Gating all scanner composables together gives a clean full-screen fallback with no visual conflicts. |
| Permission auto-requested via `LaunchedEffect("permission")` on screen entry | Named key `"permission"` distinguishes it from the event-collector `LaunchedEffect(Unit)` at the same composable level — clearer intent, no ambiguity about which effect fires for what. |
| `hasCameraPermission` re-evaluated on every composition entry (via `remember { mutableStateOf(checkSelfPermission(...)) }`) | If user grants permission in Settings and navigates back, `remember` resets and `checkSelfPermission` re-reads the current OS state — no re-prompt needed, preview shows immediately on return. |

## Key Decisions Made (Logout UX + Auth Persistence 2026-05-16)

| Decision | Rationale |
|---|---|
| Auth-user confirmation dialog separate from guest dialog | Guest dialog ("cards will be lost") needs "Create Account" CTA — not appropriate for authenticated users. Authenticated dialog ("collection is saved") reassures instead of warns. Two dialogs, two messages, same `onSignOut()` callback. |
| `GoogleSignInClient.signOut()` via `suspendCancellableCoroutine` (no new dep) | `kotlinx-coroutines-play-services` (provides `Task.await()`) is not in the project. `suspendCancellableCoroutine` wrapping `addOnCompleteListener` achieves the same result with zero new dependencies. `cont.resume(Unit)` is safe on a cancelled continuation — no-op. |
| `GoogleSignInClient` injected into `AuthRepository` (not called in NavGraph) | Logout is a repository-layer concern. Calling `googleSignInClient.signOut()` in `NavGraph.handleSignOut` (UI layer) would leak Google API knowledge into navigation. `AuthModule` already provides `@Singleton GoogleSignInClient` — Hilt auto-wires with no changes to `AuthModule`. |
| `showAuthSignOutDialog` only triggers when `!isGuest` | Existing `showSignOutDialog` path for guests is unchanged. If `isGuest` check logic changes in future, both dialogs stay independent and correct without cross-coupling. |

## Key Decisions Made (Auth 401 + Real Scan Pipeline 2026-05-16)

| Decision | Rationale |
|---|---|
| Retry loop (max 2, 300ms sleep) in `verify_google_token` — only on non-`ValueError` exceptions | Cold urllib3 pool on backend startup causes transient `TransportError` on first JWKS cert fetch → 401. Retry once with warm pool succeeds. `ValueError` (bad token, wrong audience) is never transient — retry would be wrong and delay the error. |
| `AuthInterceptor` guards Bearer from `/auth/*` endpoints | `POST /auth/google` ignores the header, but attaching a token to the endpoint that issues tokens is semantically wrong. Consistent with existing 401-event guard (`contains("auth/")`). No functional change; defensive correctness. |
| Permission permanent-denial detected via `shouldShowRequestPermissionRationale()` in launcher callback | After "Don't ask again", `launch()` silently does nothing — button appears active but is dead. `shouldShowRequestPermissionRationale()` returning `false` after a denial is the only reliable signal for permanent denial on Android. Settings deep-link is the only recovery path. |
| `context as? Activity ?: return@rememberLauncherForActivityResult` — nullable cast in permission callback | `LocalContext.current` in `ComponentActivity`-hosted composables is always the `Activity`. Nullable cast + early return is defensive for edge cases (ContextWrapper, testing) without crashing. |
| `TextRecognizer` created via `remember` + closed via `DisposableEffect.onDispose` | `remember` ensures single instance per composable lifetime. `DisposableEffect` guarantees `close()` when composable leaves composition — prevents resource leak across multiple scanner screen entries. |
| `STRATEGY_KEEP_ONLY_LATEST` for `ImageAnalysis` backpressure | Camera produces frames faster than ML Kit can process them. `KEEP_ONLY_LATEST` drops stale frames so ML Kit always processes the most recent one — no queue buildup, no latency creep. |
| `proxy.close()` in `addOnCompleteListener` (not `addOnSuccessListener`) | `addOnSuccessListener` only fires on success. A failure (e.g., ML Kit internal error) would skip the listener and never close `ImageProxy` — blocking the analyzer pipeline. `addOnCompleteListener` fires always. |
| `scanJob` holds the 5s timeout — `onFrameAnalyzed` cancels it on successful OCR | Single `scanJob` variable simplifies `resetScan()` (one cancel covers both timeout and in-progress scan). `onFrameAnalyzed` cancels the timeout coroutine before launching `fetchPrice` — prevents false `NoCardDetected` event after a successful identification. |
| `@Volatile isProcessing` flag instead of channel/mutex | ML Kit analyzer and `onFrameAnalyzed` both run on main executor (single thread). `@Volatile` provides JVM visibility guarantee without Mutex overhead. State guard (`!is Scanning`) handles the semantic invariant; `isProcessing` handles the in-flight pricing call guard. |
| `isProcessing = false` in `finally` block of pricing coroutine | Ensures flag is always reset even if `fetchPrice` throws or the coroutine is cancelled mid-way. Without `finally`, a cancelled coroutine leaves `isProcessing = true` permanently — scanner never recovers. |
| `isProcessing = true` set BEFORE `_state.value = ScanState.Scanning` in mock `startScan()` | ML Kit executor and viewModelScope run on different threads. Setting `isProcessing` after the state change leaves a nanosecond window where `onFrameAnalyzed` sees `state=Scanning` + `isProcessing=false` and enters the real OCR pipeline. Setting it first closes that window completely. |
| Mock `startScan()` uses `MOCK_CARDS.random().copy(id=UUID, scannedAt=now, priceUpdatedAt=now)` | MOCK_CARDS is a static `companion object` list. IDs in the list are fixed placeholders (`"mock-charizard"` etc.). `.copy()` generates a fresh UUID per scan — prevents Room from upsert-deduplicating all Charizard scans into one record. |
| `FREE_MONTHLY_LIMIT` reduced 20 → 10 | Tighter free-tier gate to drive Pro conversion earlier. Single constant change; `canScan()` and `recordScan()` reference it directly — no other changes needed. |
| `onFrameAnalyzed()` preserved intact during mock phase | Real OCR pipeline code stays in place for when backend is integrated. `isProcessing = true` during mock delay blocks it from executing. Zero dead-code removal — easy to restore real pipeline by removing mock `startScan()` body. |
| `onTextDetected = {}` in `ScannerScreen` (no-op lambda) | Real `onFrameAnalyzed()` pipeline was running in parallel with mock `startScan()` — could emit `NoCardDetected` snackbar or preempt mock result by cancelling `scanJob`. Disconnect at call site (ScannerScreen) rather than in ViewModel to keep ViewModel wiring intact for future real-pipeline restore. ML Kit still runs frames inside CameraPreviewComposable (unavoidable at this layer) but results are silently discarded. |
| `resetCount()` not guarded by mutex in `ScanCounterService` | `canScan()` / `recordScan()` use mutex for read-check-write atomicity. `resetCount()` is only called from `signOut()` which kills the scanner coroutine scope first — no concurrent `recordScan()` in flight. DataStore serializes writes internally preventing corruption. Adding mutex would be over-engineering for this call pattern. |
| `scanCounterService.resetCount()` in `AuthRepository.signOut()` (not NavGraph) | Counter reset is a data-layer concern — keeping it in the repository matches existing pattern (`cardRecordDao.deleteAll()` and `secureStorage.clearToken()` are also in `signOut()`). NavGraph's `handleSignOut` routes both authenticated and guest sign-out through `authRepository.signOut()`, so single callsite covers both cases. |

## Key Decisions Made (UI/UX Polish 2026-05-17)

| Decision | Rationale |
|---|---|
| Camera permission + `CameraPreview` removed entirely from `ScannerScreen` (not just bypassed) | Mock-only scan phase has no need for camera access — removing all permission code eliminates the permission dialog on first launch, dead imports, and the permission-denied fallback branch. Cleaner than a feature flag or conditional bypass. Real pipeline restore: re-add `CameraPreview` + permission logic when OCR is wired back. |
| `animateColorAsState` for reticle border (not plain `val`) | State transitions (Idle→Scanning, Scanning→Result) are instantaneous without animation — border color jumps abruptly. `animateColorAsState` gives a smooth ~300ms cross-fade that matches Material motion principles. Zero API version concern: compose-animation is in BOM. |
| Scanning border color `MaterialTheme.colorScheme.primary` (brand blue) not `Color.Yellow` | `Color.Yellow` is raw Android `FFFF00` — not in the design system, clashes with the `#0A0A0A` dark background at full saturation. `primary = #2563EB` is high-contrast on dark, used consistently across OnboardingScreen and SignInScreen. Single source of truth — follows theme if primary ever changes. |
| Result border color `Color(0xFF22C55E)` (Tailwind green-500) not `Color.Green` | `Color.Green` is `#00FF00` — neon, off-brand. `0xFF22C55E` is a muted, professional green visible on dark backgrounds without clashing with brand blue. Consistent with color palette already used in UI (`0xFFFEF3C7` amber-100 used in OnboardingScreen). |
| Border width 2dp → 3dp | 2dp reticle border is barely visible against the dim overlay on a physical device screen. 3dp maintains card-frame readability without looking heavy on 1x or high-density screens. |
| `dev.ps1` uses `& .\gradlew.bat ... \| Out-Host` not bare `& .\gradlew.bat` | In PowerShell, a native executable's stdout inside a function goes to the function's pipeline. Without `\| Out-Host`, callers that capture the return value get all Gradle output lines mixed with the return value — `$LASTEXITCODE` check logic breaks. `\| Out-Host` routes stdout to the console while keeping the function pipeline clean; callers check `$LASTEXITCODE` directly. |

---

## Key Decisions Made (Bug Fixes 2026-05-17 — sign-out perf + data persistence + scanner layout)

| Decision | Rationale |
|---|---|
| `pushPending()` parallelized with `coroutineScope { async } + awaitAll()` | Sequential for-loop caused N×~500ms sign-out block. Each `async` block catches internally — `awaitAll()` never rethrows, `coroutineScope` always completes. Room serializes concurrent `upsert` calls via its own executor — no additional locking needed. |
| `deleteAllSynced()` (WHERE serverID IS NOT NULL) replaces `deleteAll()` on sign-out | `deleteAll()` wiped cards that failed to push (no `serverID`) — permanently lost on re-login since `pullFromServer()` only restores server-side cards. `deleteAllSynced()` preserves unsynced cards in Room through sign-out; `syncAll()` on next login picks them up automatically. Trade-off: on shared devices with multiple accounts, prior account's unsynced cards persist until next login. Acceptable for single-user-per-device design. |
| `padding(bottom = 96.dp)` applied to `ReticleOverlay` modifier, not the outer `Box` | Adding padding to the outer `Box` would shift `ScanButton` up too (all siblings move). Adding it only to `ReticleOverlay` narrows the reticle render area — button stays at `BottomCenter` with its own `padding(bottom = 64.dp)`. Result: 96dp clear gap between reticle bottom and button top on all screen sizes. |

---

## Key Decisions Made (Bug Fixes 2026-05-17)

| Decision | Rationale |
|---|---|
| `collectionRepository.pushPending()` is first call in `signOut()` before `clearToken()` | Token must be valid when `pushPending()` fires — `ApiService` attaches Bearer JWT per-request. Calling `clearToken()` first makes the push unauthenticated → 401. Single ordering constraint covers both authenticated and guest-sign-out paths routed through `signOut()`. |
| `CollectionRepository` injected into `AuthRepository` (not called in NavGraph) | Sign-out is a repository-layer concern. `CollectionRepository` only depends on `CardRecordDao` + `ApiService` — no back-reference to `AuthRepository`, no circular DI. Follows existing pattern: `GoogleSignInClient` and `ScanCounterService` are already injected into `AuthRepository`. |
| `SwipeToDismissBox` replaced with trash `IconButton` + `AlertDialog` | Swipe is easy to accidentally trigger in a vertical-scroll `LazyColumn`. Trash icon + confirmation dialog requires deliberate action — prevents accidental deletion of G9-critical collection data. No API change: `viewModel.deleteCard()` and `CardRecordEntity` model unchanged. |
| `ButtonDefaults.buttonColors(disabled*)` override on `ScanButton` | Material3 default disabled colors (`onSurface.copy(alpha=0.38f)` text, `onSurface.copy(alpha=0.12f)` container) resolve to near-invisible on `Color(0xFF0A0A0A)` dark scanner background. `Color.White.copy(alpha=0.15f)` container + `Color.White.copy(alpha=0.75f)` text maintain visible contrast without suggesting the button is active. |
| `IconButton` declared AFTER `Column` in PaywallScreen `Box` | Compose draws `Box` children in declaration order — later children are topmost and receive touch events first. With `IconButton` before `Column`, the scrollable `Column` was on top and intercepted all taps — close button was unreachable. Moving it after `Column` puts it on the topmost layer. |
| `windowInsetsPadding(WindowInsets.systemBars)` on PaywallScreen `Box` | Without insets, `Box` starts at y=0 behind the device status bar on edge-to-edge displays. Padding pushes content + `IconButton` below the status bar — close button was partially hidden by status bar. `WindowInsets.systemBars` covers both status bar and navigation bar. |
| `signOut()` calls `deleteAll()` (not `deleteAllSynced()`) | `deleteAllSynced()` left unsynced cards (serverID = null) in Room. On account switch, `CollectionViewModel.init` → `syncAll()` → `pushPending()` posted those orphaned cards under the new user's JWT — cross-account data leak. `deleteAll()` ensures complete Room isolation on sign-out. Trade-off: unsynced cards lost; mitigated by 3s best-effort `withTimeoutOrNull` push before deletion. |
| `withTimeoutOrNull(3_000L)` on `pushPending()` + `withTimeoutOrNull(2_000L)` on Google `signOut()` | `pushPending()` was unbounded — N cards × ~500ms = 5–10s block before Login appears. Google `signOut()` suspends 1–3s. Both are best-effort: token clear + Room wipe happen regardless of timeout. Max sign-out duration: ~5s (was unbounded). |
| `ScanCounterPill` uses `.statusBarsPadding()` + `padding(top=8.dp)` (was `padding(top=60.dp)`) | Fixed 60dp top padding had no status bar awareness. On edge-to-edge displays, pill was partially behind the status bar. On 640dp screens, reticle top = 58dp, pill bottom = 96dp → 38dp overlap with reticle frame. `statusBarsPadding()` pushes pill below status bar; `padding(top=8.dp)` gives 8dp breathing room. |
| `ReticleOverlay` gains `padding(top=80.dp)` (was `padding(bottom=96.dp)` only) | Reticle centered in full height minus bottom padding put it too high on small screens, colliding with counter pill. Adding 80dp top padding shifts the reticle center down — 80dp gap filled by the outer Box's dark `0xFF0A0A0A` background (visually identical to dim overlay; no camera preview in mock phase). |
| `applicationScope` in `AuthRepository` — `CoroutineScope(SupervisorJob() + Dispatchers.IO)` as class field | Google `signOut()` GMS callback is non-critical (only affects account picker on next sign-in). Fire-and-forget via `applicationScope.launch` eliminates 2s blocking from `signOut()`. `SupervisorJob` prevents child failure from cancelling the scope. Lives for process lifetime — idiomatic pattern for `@Singleton` repositories needing app-lifetime scope without a Hilt `@ApplicationScope` module. Push timeout also reduced 3s → 1s (parallelized push completes in <1s on normal connection). Combined: `signOut()` returns in ~1s max (was ~5s). |

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