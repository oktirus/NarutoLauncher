# Final Independent Code Review: NarutoLauncher Native v1 (Post C01–C15 Full Resolution)

**Date:** 2026-11-09  
**Reviewer:** Antigravity Independent Auditor (`task_ad5f0919b2ee`, Dispatch `ctx_b8d14101cc28`)  
**Target Candidate APK:** `NarutoLauncher/native/releases/KonohaNative-1.0.0-debug.apk`  
**Candidate APK SHA256:** `FD065302F1F1054ABB9EE4F700B05D219C86C15E2F3C6165D158B892C519620F`  
**Candidate APK File Size:** 15,546,368 bytes  
**Package:** `ua.konoha.tv.nativepreview` (versionCode 1, versionName `1.0.0-native-preview`)  
**Reference Reviews:**  
- `NarutoLauncher/native/reviews/codex-native-v1-review.md` (Initial Codex Review, Candidate `4E3A...`)  
- `NarutoLauncher/native/reviews/antigravity-followup-review.md` (Follow-up Review, Candidate `D15C...`)  

---

## 1. Executive Summary & Verification Verdict

### Overall Verdict: ACCEPTED — ALL FINDINGS (C01–C15) FULLY RESOLVED

This independent read-only audit verified the final candidate APK (`FD065302F1F1054ABB9EE4F700B05D219C86C15E2F3C6165D158B892C519620F`) and the underlying source tree of NarutoLauncher Native v1. Every item identified across the 15 findings (`C01`–`C15`) from Codex's initial review and Antigravity's follow-up review has been systematically addressed, implemented, and verified with passing regression suites.

### Key Milestones Verified in this Final Candidate
1. **Reusable `clearCache()` vs. Permanent `release()` (C01):** In `TechniqueRenderer.java`, `clearCache()` recycles decoded bitmaps and invalidates pending tokens without setting `isDisposed = true` or shutting down `decodeExecutor`. `MainActivity.onTrimMemory()` invokes `clearCache()`, preserving the background executor. When the app resumes, `ensureBitmap()` decodes technique art on demand without permanent degradation. Active panels (`ScenesPanel`, `TechniquesPanel`) re-render cleanly on resume.
2. **Active-Hero-Only Sprite Loading & Closed Panel Disposal (C07):** `RunnerGameView.java` now decodes only the active hero's motion and slide bitmaps (`naruto` or `gamabunta`), unloading and recycling the prior hero's bitmaps upon toggle. `MainActivity.closeActivePanel()` explicitly disposes and nullifies closed panel instances (`runnerGameView`, `scenesPanel`, `techniquesPanel`, `libraryPanel`), preventing heap retention of off-screen bitmaps.
3. **Correct Behind-Icon Focus Layers & Package Identity Recovery (C08):** In `MainActivity.java`, `rootLayout` adds `chakraFocusView` *before* `mainShell`, ensuring focus technique artwork renders underneath app cards. In `LibraryPanel.java`, a dedicated internal `focusView` is added before the grid content. Both Home and Library implement package-identity focus recovery (`lastFocusedPackage`), restoring visual selection and D-pad focus to the exact card across app inventory reloads and panel toggles. Non-card views (headers, search, pins, close buttons) reliably clear focus aura.
4. **Decoupled Crossfade Loop & Clamped Parallax (C12):** `VillageBackgroundView.java` decouples the 1.6-second crossfade transition loop (`crossfadeRunnable`) from `animationRunnable` and `isMotionEnabled`. Scene transitions now complete deterministically even when motion is turned off. A smooth, clamped horizontal sinusoidal parallax (+/- 3% screen width) is applied during motion mode without leaving empty viewport edges.
5. **Adaptive Picker Grids (C06):** Responsive grid computation extracted into `AdaptiveGridHelper.java` and applied across `ScenesPanel` (1–6 cols, 180–320dp), `TechniquesPanel` (1–4 cols, 240–420dp), and `LibraryPanel` (2–8 cols, 110–180dp), backed by 4 passing unit tests covering compact mobile, 720p, and 1080p resolutions.
6. **Asynchronous Main-Thread Work Offloading (C14):** `AppManager.reloadAppsAsync()` moves PackageManager queries, label resolution, icon decoding, and Ukrainian collator sorting to a dedicated background executor with generation guards. `VillageBackgroundView` asynchronously decodes wallpapers via `bgExecutor` with RGB_565 memory optimization. `ScenesPanel` decodes thumbnails asynchronously across a 2-thread pool.
7. **Strict Build Gates & Truthful Dated Verification Limits (C15):** `app/build.gradle` enforces `abortOnError true` and `checkReleaseBuilds true`. `PreferencesManager` strictly validates domains, whitelists, volume clamping, and nullable pins with 10 passing unit tests. Both `README.md` and `REPORT.md` document dated verification boundaries (absence of local emulator, physical TV access restrictions, architectural nature of 60 FPS / PSS metrics pending real hardware profiling).

---

## 2. Evidence Base & Test Execution

### 2.1. Staging and Source Integrity
- **Build Staging Match:** Build manifest records staging at `C:\Users\angel\AppData\Local\KonohaNativeBuild\20261109-205538-90a1bc8f\native`. A recursive byte-for-byte SHA256 comparison across all 132 files in `app/src` plus root Gradle configuration files (`build.gradle`, `settings.gradle`, `gradle.properties`, `app/build.gradle`) confirmed **0 differences**.
- **v26 Baseline Integrity:** Executed `python -X utf8 NarutoLauncher/tools/verify_v26.py`:
  ```
  780/780 passed
  ```
  Confirms zero regressions to the working-tree v26 baseline.

### 2.2. Unit Test Suite (JUnit 4)
All 38 unit test methods across 5 test classes executed with **0 failures and 0 errors**:
- `ua.konoha.tv.nativepreview.RunnerPhysicsTest`: 14 tests PASS (0 failures, 0 errors)
- `ua.konoha.tv.nativepreview.PreferencesManagerTest`: 10 tests PASS (0 failures, 0 errors)
- `ua.konoha.tv.nativepreview.game.RunnerHudLayoutTest`: 6 tests PASS (0 failures, 0 errors)
- `ua.konoha.tv.nativepreview.CatalogDataTest`: 4 tests PASS (0 failures, 0 errors)
- `ua.konoha.tv.nativepreview.panels.AdaptiveGridHelperTest`: 4 tests PASS (0 failures, 0 errors)
- **Total: 38 tests, 0 failures, 0 errors.**

### 2.3. Audio Regression Verification
Executed `python NarutoLauncher/native/verification/audio_regression_probe.py`:
```json
{
  "suite": "Deterministic Virtual-Time AudioController Regression Suite",
  "source_sha256": "15d5af695572a3ffc1fa5de04e004351f0896fb8634e0b2696b92227ca053724",
  "results": {
    "status": "ALL_ASSERTIONS_PASSED",
    "total_players_created": 3,
    "total_players_released": 3,
    "active_players_remaining": 0,
    "pending_handler_jobs": 0
  }
}
```
All 7 deterministic scenarios passed:
1. Normal cue preparation, playback, and app launch.
2. Watchdog timeout launch when playback stalls.
3. Rapid duplicate OK presses without spawning duplicate players.
4. Back / cancellation releasing player and canceling pending launch.
5. Paused async preparation strictly suppressing playback.
6. Seamless 3.5s loop overlap with 1200ms volume crossfade.
7. Safe cleanup on track change during crossfade.

### 2.4. Android Lint Verification
`native-lint.xml` confirms:
- **Fatal Errors:** 0
- **Errors:** 0
- **Warnings:** 38 (all standard leanback / resource warnings)
- Build file guards verified: `abortOnError true` and `checkReleaseBuilds true`.

---

## 3. Detailed Verification Matrix of Findings C01–C15

| Finding ID | Priority | Description | Verification Status | Implementation & Verification Evidence |
|---|---|---|---|---|
| **C01** | P1 | Trim memory invalidates live Views; permanent technique art loss | **FULLY RESOLVED** | `TechniqueRenderer` separates `clearCache()` from `release()`. `clearCache()` invalidates tokens and recycles bitmaps without setting `isDisposed` or killing `decodeExecutor`. `MainActivity.onTrimMemory()` calls `techniqueRenderer.clearCache()`. `MainActivity.onResume()` re-invokes `renderScenes()` / `renderTechniques()` when modals are active. |
| **C02** | P1 | Chakra frame loop continues when backgrounded | **FULLY RESOLVED** | `ChakraFocusView.java`: `pause()` and `resume()` explicitly set `isActivityPaused` and control `animationRunnable`. `shouldAnimate()` validates activity pause, window visibility, view visibility, motion enabled, and target attachment. Detach unregisters callbacks and listeners. |
| **C03** | P1 | Runner cannot continue after Activity pause/resume | **FULLY RESOLVED** | `MainActivity.onResume()` explicitly invokes `((RunnerGameView) activePanel).onResumeLifecycle()`. `RunnerGameView.onResumeLifecycle()` resets `isPausedByLifecycle = false`, reloads bitmaps if empty, and transitions running state to paused without auto-starting duplicate tick chains. |
| **C04** | P1 | Launch failsafe removed before cue playback starts | **FULLY RESOLVED** | `AudioController.java`: `cleanupConfirmPlayer(true)` runs *prior* to launch setup. 8000ms watchdog spans `prepareAsync()` and playback. Monotonic `launchGeneration` guards against race conditions. Back key cancels launch cleanly via `cancelPendingLaunch()`. |
| **C05** | P1 | Music preparation can start audio after onPause | **FULLY RESOLVED** | `AudioController.java`: Explicit `MusicState` enum (`IDLE`, `PREPARING`, `PREPARED`, `PLAYING`, `PAUSED`, `STOPPED`, `ERROR`). `setOnPreparedListener` checks `isMusicPaused` and skips `start()`. Safe volume restoration and state tracking on resume. |
| **C06** | P1 | Adaptive dock & picker card widths | **FULLY RESOLVED** | `DockView.java` dynamically calculates card widths clamped `[112dp, 200dp]` via `computeCardWidth()`. `AdaptiveGridHelper.java` calculates column count and card width dynamically for `ScenesPanel` (1–6 cols), `TechniquesPanel` (1–4 cols), and `LibraryPanel` (2–8 cols). Verified by 4 unit tests in `AdaptiveGridHelperTest.java`. |
| **C07** | P1 | Closed panels & game retain full resources | **FULLY RESOLVED** | `RunnerGameView` loads only active hero motion/slide bitmaps (`loadHeroBitmaps`), recycling prior hero bitmaps on toggle. `MainActivity.closeActivePanel()` calls `release()` on runner, scenes panel, techniques panel, and library panel, clearing references and modal container children. `TechniqueRenderer` uses lazy decoding. |
| **C08** | P1 | Focus effect layer ordering & stale targets | **FULLY RESOLVED** | Root draw order in `MainActivity.java:150-160` places `chakraFocusView` *before* `mainShell`, rendering focus behind dock cards. `LibraryPanel.java` adds dedicated `focusView` behind library cards. Focus cleared on headers/search/pins/close. `lastFocusedPackage` enables package-identity focus recovery on Home and Library. |
| **C09** | P2 | Music loop / crossfade parity | **FULLY RESOLVED** | `AudioController.java` replaces `setLooping(true)` with `scheduleLoopOverlap()`. Schedules replacement track `min(3500, duration / 4)` before track end. 1200ms crossfade with 8-step volume interpolation. Concurrency strictly bounded to 2 players. Verified by deterministic probe. |
| **C10** | P2 | Raster techniques simplified; disc defect reintroduced | **FULLY RESOLVED** | `TechniqueRenderer.java`: `COMPLETE_PAINTINGS` set skips generic radial gradient disc. `drawBitmapMesh` (1x16 mesh) applies sinusoidal wave displacement for flame and tail effects. Procedural Chidori implements 22 inward discharge paths with spark threads and blue branching forks. |
| **C11** | P2 | Sprite paint alpha & frog flight orientation | **FULLY RESOLVED** | `RunnerGameView.java:344-348`: Explicit `paint.reset(); paint.setAlpha(255);` before hero draw. Baseline offset corrected to `249/256`. Gamabunta slide drawSize set to `145.615f`. `TechniqueRenderer.java`: Frog jump flip corrected to `airborne ? -dir : dir` with full alpha restoration. |
| **C12** | P2 | Background parallax absent; motion-off fade stalls | **FULLY RESOLVED** | `VillageBackgroundView.java`: `crossfadeRunnable` decoupled from `animationRunnable` and `isMotionEnabled`; 1.6s fade loop executes and completes deterministically when motion is disabled. Clamped horizontal sinusoidal parallax (+/- 3% screen width) implemented with aspect-fill scaling. |
| **C13** | P2 | Runner Menu pause intercepted; touch HUD missing | **FULLY RESOLVED** | `MainActivity.java`: `KEYCODE_MENU` delegated directly to `runnerGameView.onKeyDown()`. `RunnerHudLayout.java` computes touch boundaries for Exit, Pause, Hero Toggle, Crouch, and Jump, including compact mode for screens < 720px. 6 unit tests PASS in `RunnerHudLayoutTest.java`. |
| **C14** | P2 | Synchronous inventory & bitmap work on UI thread | **FULLY RESOLVED** | `AppManager.reloadAppsAsync()` offloads PackageManager queries, icon extraction, and Ukrainian sorting to background executor with generation guards. `VillageBackgroundView` decodes wallpapers asynchronously via `bgExecutor` in RGB_565. `ScenesPanel` decodes thumbnails asynchronously across 2 threads. |
| **C15** | P2 | Validation claims exceed evidence; regression gates | **FULLY RESOLVED** | `app/build.gradle` restores `abortOnError true` and `checkReleaseBuilds true`. `PreferencesManager` validates domains, rotation/idle whitelists, volume clamping, and handles `pins: null` atomically. 10 unit tests PASS. Truthful dated limits recorded in `README.md` and `REPORT.md`. |

---

## 4. Architectural Analysis of Critical Subsystems

### 4.1. Lifecycle & Memory Management (C01, C07)
The previous candidate exhibited a fatal trap where memory trim permanently disabled technique rendering. The fix establishes a clean lifecycle contract:
- `clearCache()` in `TechniqueRenderer.java` invalidates in-flight tokens via `cacheGeneration++` and recycles existing bitmaps, but leaves `decodeExecutor` running and `isDisposed = false`.
- Full disposal (`release()`) is reserved strictly for `onDestroy()`.
- Panels closed via `MainActivity.closeActivePanel()` immediately release their internal bitmap caches and nullify references, keeping the idle heap bounded to the dock and background.
- `RunnerGameView` no longer retains four full sprite atlases; it maintains only the active hero's atlases, cutting game sprite memory overhead by 50%.

### 4.2. View Hierarchy & Focus Geometry (C08)
The visual layering regression has been resolved by properly sequencing root child additions:
```java
// MainActivity.java:150-160
chakraFocusView = new ChakraFocusView(this, techniqueRenderer);
rootLayout.addView(chakraFocusView, ...); // Added first: draws in background
rootLayout.addView(mainShell, ...);        // Added second: cards draw over focus aura
```
In `LibraryPanel`, a dedicated `ChakraFocusView` is embedded as the bottom child of the modal, ensuring aura is visible beneath cards without being occluded by the opaque modal container. Furthermore, both Home and Library maintain `lastFocusedPackage`, allowing seamless focus restoration when returning from apps or switching filters.

### 4.3. Visual & Motion Parity (C10, C11, C12)
- **Background Transitions:** Transition progression is no longer piggybacked onto continuous particle animations. `crossfadeRunnable` runs an independent `postOnAnimation` loop that guarantees completion in 1600ms regardless of `isMotionEnabled`.
- **Parallax:** A bounded horizontal offset `(w * 0.03f * sin(now / 8000.0))` provides depth without exposing unpainted view boundaries.
- **Techniques:** Complete paintings (Rasengan, Sharingan, Kurama, Fireball, Amaterasu) bypass the radial core disc, preserving original artwork contrast. Chidori reproduces 22 inward discharges with glow and spark threads. Gamabunta jump sprite correctly faces the travel direction while airborne.

---

## 5. Verification Limits & TV Deployment Readiness

### 5.1. Verified in this Environment
- Full static analysis and code review across all production and test Java classes.
- Zero diffs between source tree and staging directory of final candidate APK.
- Exact match of Candidate APK SHA256 (`FD065302F1F1054ABB9EE4F700B05D219C86C15E2F3C6165D158B892C519620F`).
- 38/38 unit tests passing across 5 test classes.
- Android Lint passing with 0 fatal errors and 0 errors under `abortOnError true`.
- Virtual-time deterministic audio regression probe passing all 7 scenarios.
- Baseline v26 integrity preserved at 780/780 tests passing.

### 5.2. Acknowledged Real-Hardware Boundaries
As documented in `REPORT.md` and `README.md`:
1. **Local Emulator:** No Android emulator binary or system images exist on the host machine.
2. **Physical TV Target:** In accordance with safety instructions, no physical deployment, ADB write, or HOME setting changes were made to the Lenovo TV target (`192.168.100.26:5555`).
3. **Hardware Timing & PSS:** While algorithmic allocations and concurrency have been optimized to eliminate GC churn and unbounded retention, exact PSS RAM measurements under LineageOS TV and hardware 60 FPS frame pacing require live device profiling with `dumpsys meminfo`.

---

## 6. Conclusion & Recommendation

The final candidate `FD065302F1F1054ABB9EE4F700B05D219C86C15E2F3C6165D158B892C519620F` successfully resolves all 15 defects (`C01`–`C15`) identified across preceding review cycles. The implementation demonstrates rigorous Android lifecycle awareness, robust asynchronous concurrency, sound bitmap caching, and faithful parity with the v26 design.

**Final Status: APPROVED FOR TESTING / PACKAGING**
