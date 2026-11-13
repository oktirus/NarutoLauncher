# Independent Follow-Up Code Review: NarutoLauncher Native v1 (Post C01–C15 Fixes)

**Date:** 2026-11-09  
**Reviewer:** Antigravity Independent Auditor (`task_6d68a906c42e`, Dispatch `ctx_ef5a7f5e6e1d`)  
**Target Candidate APK:** `NarutoLauncher/native/releases/KonohaNative-1.0.0-debug.apk`  
**Candidate APK SHA256:** `D15CB568B79E16B02B8E378CF7A08787D3BEC66550BE0F98D6AB930E1B9429BA` (15,539,532 bytes)  
**Package:** `ua.konoha.tv.nativepreview` (versionCode 1, versionName `1.0.0-native-preview`)  
**Reference Review:** `NarutoLauncher/native/reviews/codex-native-v1-review.md`  

---

## 1. Executive Summary & Verification Limits

This independent read-only audit evaluated the post-fix candidate of NarutoLauncher Native v1 against the 15 findings (`C01`–`C15`) identified in Codex's review of candidate `4E3A...`.

### Overall Assessment
**Status: CHANGES RECOMMENDED BEFORE TV DEPLOYMENT**  
Significant engineering progress was made: 8 findings are **Fully Resolved**, 3 are **Substantially Resolved**, 3 are **Partially Resolved** with remaining architectural regressions, and 1 finding (**C12**) was **Unresolved / Untouched**.

### Key Highlights & Regressions
1. **Critical Trim Regression in C01:** In `TechniqueRenderer.java`, `release()` sets `isDisposed = true` and invokes `decodeExecutor.shutdownNow()`. When `MainActivity.onTrimMemory(TRIM_MEMORY_RUNNING_LOW)` calls `techniqueRenderer.release()`, `isDisposed` permanently prevents future bitmap decoding via `ensureBitmap()`. The executor is never revived, and `MainActivity` does not re-instantiate `TechniqueRenderer` on resume. A single memory trim permanently disables raster technique rendering for the remaining lifetime of the app process.
2. **Unaddressed Background Parallax & Motion-Off Crossfade (C12):** `VillageBackgroundView.java` was unchanged from the previous candidate. Parallax is completely absent. Furthermore, `animationRunnable` remains gated by `isMotionEnabled`, causing crossfade transitions initiated while motion is disabled to stall indefinitely without external invalidations.
3. **Resource Retention in Game & Panels (C07) and View Layering (C08):** `RunnerGameView` still eagerly decodes all 4 motion/slide atlases for both heroes on construction. `MainActivity.closeActivePanel()` does not dispose or release closed panel views. In `MainActivity`, root layout order remains `mainShell -> chakraFocusView -> modalContainer`, maintaining the inverted layer order where focus aura draws over Home icons and is obscured under the opaque modal in Library.

### Audit Environment & Evidence Base
- **Staging Verification:** The candidate build staging recorded in `build-manifest.json` (`C:\Users\angel\AppData\Local\KonohaNativeBuild\20261109-195748-1b75a499\native`) was compared byte-for-byte against `NarutoLauncher/native/`. **0 differences** detected across all production source files, assets, and Gradle build files.
- **Baseline v26 Integrity:** `python -X utf8 NarutoLauncher/tools/verify_v26.py` verified **780/780 PASS**.
- **JUnit 4 Test Suite:** 34 tests across 4 test classes passed with **0 failures and 0 errors** (up from 21 tests):
  - `CatalogDataTest`: 4 tests PASS.
  - `RunnerPhysicsTest`: 14 tests PASS.
  - `PreferencesManagerTest`: 10 tests PASS (including null pins, clamping, atomic failures).
  - `RunnerHudLayoutTest`: 6 tests PASS (touch bounds & responsive compact layout).
- **Audio Regression Probe:** Production `AudioController.java` was verified with virtual-time deterministic test harness `audio_regression_probe.py`: **ALL 7 SCENARIOS PASSED** (0 active players remaining, 0 leaked Handler jobs, safe 3.5s streamed loop overlap).
- **Android Lint:** Passed with **0 Fatal / 0 Error**, 38 Warnings. `abortOnError true` and `checkReleaseBuilds true` were verified in `app/build.gradle`.
- **Hardware Limits:** No local Android emulator image or physical TV target was available in this review session. Real TV frame timing (60 FPS) and exact PSS RSS memory under LineageOS TV remain unmeasured on hardware.

---

## 2. Detailed Verification of Findings C01–C15

### C01. Trim invalidates live ImageViews and permanently removes technique art
- **Codex Finding:** `onTrimMemory` calls `release()` at every level >= `RUNNING_LOW`. Panels retain child `ImageView`s referencing Bitmaps which `release()` recycles without clearing drawables, risking crash upon returning. `TechniqueRenderer` cleared all plates with no lazy reload mechanism.
- **Review of Fixes:**
  - `TechniquesPanel.java`: `release()` now calls `grid.removeAllViews()` and `previewViews.clear()` before recycling `previewBitmaps`.
  - `ScenesPanel.java`: `release()` now calls `grid.removeAllViews()` before recycling `thumbCache`.
  - `TechniqueRenderer.java`: Eliminated eager constructor loading; introduced lazy `ensureBitmap()` decoding via background `decodeExecutor`.
- **Remaining Regressions / Gaps:**
  - **Fatal `isDisposed` Trap:** `TechniqueRenderer.release()` sets `isDisposed = true` and runs `decodeExecutor.shutdownNow()`. When `MainActivity.onTrimMemory()` invokes `techniqueRenderer.release()`, `isDisposed` remains permanently `true`. Any subsequent call to `ensureBitmap()` returns `null` immediately. The executor is dead, and `MainActivity.onResume()` does not recreate `techniqueRenderer`. Technique art is permanently lost until the process is restarted.
  - **Blank Panel on Return:** When trim occurs while `scenesPanel` or `techniquesPanel` is the active panel, `grid.removeAllViews()` clears the view hierarchy. On resume, `MainActivity.onResume()` does not call `renderScenes()` or `renderTechniques()`, leaving the user on a blank modal.
- **Status:** **PARTIALLY RESOLVED / REMAINING REGRESSION**

---

### C02. Chakra frame loop keeps running while Activity is backgrounded
- **Codex Finding:** `ChakraFocusView` animation loop checked only targetView and visibility, continuing to post frames when Activity lost visibility. No `onDetachedFromWindow` cleanup.
- **Review of Fixes:**
  - `ChakraFocusView.java` now includes explicit `pause()` and `resume()` methods controlling `isActivityPaused` and calling `removeCallbacks(animationRunnable)`.
  - `shouldAnimate()` enforces `!isActivityPaused`, `isAttachedToWindow()`, `getWindowVisibility() == VISIBLE`, `getVisibility() == VISIBLE && isShown()`, `isMotionEnabled`, and attached/visible targetView state.
  - `onDetachedFromWindow()` properly unregisters `animationRunnable` and the bitmap load listener.
  - Lifecycle integration verified in `MainActivity.onPause()` (`chakraFocusView.pause()`) and `MainActivity.onResume()` (`chakraFocusView.resume()`).
- **Status:** **FULLY RESOLVED**

---

### C03. Runner cannot continue after Activity pause/resume
- **Codex Finding:** `onPauseLifecycle` set `isPausedByLifecycle=true`. `MainActivity.onResume` never called `onResumeLifecycle()`. Pressing OK did not resume tick because flag remained true.
- **Review of Fixes:**
  - `MainActivity.java:682-684`: `onResume()` now explicitly checks `if (activePanel instanceof RunnerGameView) { ((RunnerGameView) activePanel).onResumeLifecycle(); }`.
  - `RunnerGameView.java:174-183`: `onResumeLifecycle()` sets `isPausedByLifecycle = false`, reloads bitmaps if cleared, and transitions `"running"` to `"paused"`.
  - Physics stays safely paused upon resume, waiting for user input (OK / Menu / Touch) without duplicate tick loops or premature hero death.
- **Status:** **FULLY RESOLVED**

---

### C04. Launch failsafe is removed before cue playback even starts
- **Codex Finding:** `launchWithTechnique` posted `failsafeRunnable` and immediately called `cleanupConfirmPlayer`, removing the failsafe. Watchdog was absent during cue playback; synchronous `prepare()` blocked the UI thread.
- **Review of Fixes:**
  - `AudioController.java:526-620`: `cleanupConfirmPlayer(true)` is now executed *prior* to scheduling the new launch.
  - `failsafeRunnable` is posted for 8000ms spanning the full asynchronous preparation and cue playback cycle.
  - Synchronous `prepare()` replaced with non-blocking `prepareAsync()`.
  - Added monotonic `launchGeneration` fencing against out-of-order completion, errors, and cancellations.
  - `cancelPendingLaunch()` added for Back/Pause events.
  - Verified by deterministic tests in `audio_regression_probe.py` (Tests 1, 2, 3, 4).
- **Status:** **FULLY RESOLVED**

---

### C05. Music preparation can start audio after onPause
- **Codex Finding:** Unconditional `setOnPreparedListener(MediaPlayer::start)` caused delayed preparation callbacks to play music while the Activity was backgrounded. Lacked explicit state management.
- **Review of Fixes:**
  - Implemented explicit `MusicState` enum (`IDLE`, `PREPARING`, `PREPARED`, `PLAYING`, `PAUSED`, `STOPPED`, `ERROR`).
  - In `setOnPreparedListener`:
    ```java
    if (isMusicPaused || !prefs.isAudioEnabled() || !prefs.isAmbienceEnabled()) {
        musicState = MusicState.PAUSED;
        return; // strictly skips player.start()
    }
    ```
  - `pauseMusic()` flags `isMusicPaused = true`, releases `pendingMusicPlayer`, and pauses active players.
  - `resumeMusic()` checks `MusicState.PAUSED`, starts playback, and restores volume safely.
  - Verified in `audio_regression_probe.py` (Test 5: Paused Async Prepare & Volume Restore).
- **Status:** **FULLY RESOLVED**

---

### C06. Adaptive dock regression: cards have constant width regardless of item count
- **Codex Finding:** Cards had fixed 130dp width + 12dp margins, leaving wide screens sparse with few favorites and causing unnecessary scrolling on compact screens.
- **Review of Fixes:**
  - `DockView.java`: Implemented `computeCardWidth(availableWidthPx, itemCount)`:
    - Calculates available space minus container padding (32dp) and card margins (12dp per item).
    - Clamped between `minWidth = dp(112)` and `maxWidth = dp(200)`.
  - Added dynamic resizing in `onSizeChanged(w, h, oldw, oldh)`.
  - Applied in `render()` using display metrics and total item count (`dockApps.size() + 1`).
- **Remaining Observations:**
  - Picker modal layouts (`ScenesPanel` 3-column, `TechniquesPanel` 2-column, `LibraryPanel` 4-column) retain fixed card dimensions.
- **Status:** **SUBSTANTIALLY RESOLVED (Dock adaptive; pickers remain fixed)**

---

### C07. Closed panels and game still retain their full resources
- **Codex Finding:** `closeActivePanel` only detached views; `MainActivity` retained panels, thumbnail maps, and runner. Game loaded both heroes' four atlases at construction. TechniqueRenderer eagerly preloaded all 10 plates.
- **Review of Fixes:**
  - `TechniqueRenderer.java`: Replaced eager constructor preloading with lazy `ensureBitmap()` decoding.
- **Remaining Regressions / Gaps:**
  - `RunnerGameView.java:87-105`: `loadBitmaps()` continues to eagerly decode all 5 bitmaps (`forest`, `naruto-motion`, `naruto-slide`, `gamabunta-motion`, `gamabunta-slide`) for both heroes at once upon initialization. Active-hero-only loading was not implemented.
  - `MainActivity.closeActivePanel()`: Only detaches `activePanel` from `modalContainer`. All panel instances (`libraryPanel`, `orderPanel`, `scenesPanel`, `techniquesPanel`, `settingsPanel`, `runnerGameView`) remain pinned on `MainActivity` fields with full bitmap caches intact.
- **Status:** **PARTIALLY RESOLVED**

---

### C08. Focus effect is layered above dock artwork and below the opaque library; target can be stale
- **Codex Finding:** Root draw order `mainShell -> ChakraFocusView -> modalContainer` caused technique effect to paint over Home app icons and under the opaque Library panel. Detached card targets could linger.
- **Review of Fixes:**
  - `ChakraFocusView.java`: Added detachment safety in `onDraw()` (`if (!targetView.isAttachedToWindow()) targetView = null;`).
  - `DockView.java`: Added `onDockFocusCleared()` listener clearing the aura when focus moves outside the dock.
- **Remaining Regressions / Gaps:**
  - **Root Layout Order Unchanged:** In `MainActivity.java:155-172`, `rootLayout` still adds `mainShell`, then `chakraFocusView`, then `modalContainer`. Technique focus aura paints above icons on Home, and `modalContainer` paints above `chakraFocusView` in Library.
  - **Identity-Based Focus Recovery:** Focus is not preserved by package identity across dock reloads or app launches.
- **Status:** **PARTIALLY RESOLVED**

---

### C09. Music loop/crossfade parity is missing
- **Codex Finding:** Single MediaPlayer with `setLooping(true)`; switching stopped audio abruptly. No overlap or crossfade scheduling existed.
- **Review of Fixes:**
  - `AudioController.java`: Replaced `setLooping(true)` with `scheduleLoopOverlap()`, scheduling a secondary MediaPlayer 3.5s before track end.
  - Implemented 8-step volume fade (`startFadeIn()` / `startFadeOut()`) over 1200ms.
  - Bounded concurrency: maximum 2 active `MediaPlayer` instances during crossfade; previous player safely released immediately upon fade completion.
  - Verified by `audio_regression_probe.py` across two consecutive loop seams and stop/release events (Tests 6 and 7).
- **Status:** **FULLY RESOLVED**

---

### C10. Raster techniques are visually simplified and old filled-disc defect was reintroduced
- **Codex Finding:** Radial-gradient disc drawn indiscriminately under all techniques (including complete paintings). Fireball/Amaterasu/Kurama lacked wave deformation. Procedural Chidori lacked fork and glow structures.
- **Review of Fixes:**
  - `TechniqueRenderer.java`: `COMPLETE_PAINTINGS` set (`rasengan`, `sharingan`, `kurama`, `fireball`, `amaterasu`) explicitly skips `drawTechniqueCore()`, eliminating the radial disc.
  - Implemented `drawDeformedPlate()` using `canvas.drawBitmapMesh()` (1x16 mesh) with sinusoidal wave displacement for "flame" and "tails" modes matching v26.
  - Implemented `drawChidori()` with 22 inward discharges, inner white spark thread, outer blue aura, and random branching forks (`COLOR_CHIDORI_GLOW`).
- **Status:** **FULLY RESOLVED**

---

### C11. Sprite paint state makes game heroes and Gamabunta focus translucent; frog flight flip differs
- **Codex Finding:** Shared paint alpha set to 60 for shadow and not reset to 255 before drawing hero/frog. Horizontal flip in frog jump did not invert opposite-facing sprite. Baseline 0.95 vs 249/256. Gamabunta slide drawSize ignored.
- **Review of Fixes:**
  - `RunnerGameView.java:344-348`: Explicit `paint.reset(); paint.setAlpha(255);` before hero draw.
  - `RunnerGameView.java:337`: Implemented exact Gamabunta slide size `145.615f`.
  - `RunnerGameView.java:341`: Corrected baseline offset to `drawSize * (249.0f / 256.0f)`.
  - `TechniqueRenderer.java:510-516`: Corrected frog flight orientation `scaleX = (airborne ? -dir : dir)` and restored `paint.setAlpha(255)`.
- **Status:** **FULLY RESOLVED**

---

### C12. Background parallax is absent; motion-off scene changes do not get a fade frame loop
- **Codex Finding:** `drawScaledBitmap` centers images with no parallax. Crossfade progress was gated by `isMotionEnabled`; when motion was off, scene changes stalled.
- **Review of Candidate:**
  - `VillageBackgroundView.java` was **NOT modified** in this candidate (SHA256 `A675CF0B...` matches previous freeze).
  - `animationRunnable` remains gated by `if (!isDecorPaused && isMotionEnabled && getVisibility() == VISIBLE)`.
  - With `isMotionEnabled == false`, `setScene(scene, true)` sets `isCrossfading = true` and invalidates once, but no animation loop runs to progress or complete the 1.6s fade.
  - Background parallax remains completely unimplemented.
- **Status:** **UNRESOLVED / NOT FIXED**

---

### C13. Runner's Menu pause is intercepted by the Activity; touch controls lost
- **Codex Finding:** `MainActivity` consumed MENU key for settings before Runner could pause. Touch controls for exit, pause, and hero toggle were missing.
- **Review of Fixes:**
  - `MainActivity.java:650-653`: Delegated KEYCODE_MENU to `runnerGameView.onKeyDown()` when Runner is active.
  - Introduced `RunnerHudLayout.java`: Calculates responsive touch bounding boxes for Exit, Pause, Hero Toggle, Crouch, and Jump, with compact mode for screens < 720px.
  - `RunnerGameView.onTouchEvent()`: Dispatches touch events via `hudLayout.resolveAction()`.
  - Verified with 6 unit tests in `RunnerHudLayoutTest.java`.
- **Status:** **FULLY RESOLVED**

---

### C14. Expensive inventory and bitmap work remains synchronous on the main thread
- **Codex Finding:** `AppManager.reloadApps()` queried `PackageManager` and loaded all icons synchronously on the main thread during `onCreate` and `onResume`. Scenes and thumbnails decoded synchronously.
- **Review of Fixes:**
  - `TechniqueRenderer.java`: Technique plates decoding was moved to background `decodeExecutor`.
- **Remaining Regressions / Gaps:**
  - `AppManager.java` was not modified: `reloadApps()` still executes synchronous `pm.queryIntentActivities()`, `info.loadLabel()`, and `info.loadIcon()` on the UI thread.
  - `ScenesPanel.java`: `loadThumbBitmap()` synchronously decodes 18 WebP thumbnails inside `createSceneCard()`.
  - `VillageBackgroundView.java`: `loadSceneBitmap()` synchronously decodes 1920x1080 JPEG wallpapers on the main thread inside `setScene()`.
- **Status:** **PARTIALLY RESOLVED**

---

### C15. Validation/review claims exceed evidence; regression gates must cover Android behavior
- **Codex Finding:** `build.gradle` disabled lint abort on error. Preference JSON import lacked type/domain checks, volume clamping, and rejected v26 default `pins: null`. Documentation made unsupported 100% parity and PSS claims.
- **Review of Fixes:**
  - `app/build.gradle:23-26`: Restored `abortOnError true` and `checkReleaseBuilds true`.
  - `PreferencesManager.java`: Added domain validation for scenes, music tracks, techniques, and cues; whitelist checking for rotation (`0, 60, 180, 600`) and idle (`0, 300, 900`); volume clamping `[0.0, 1.0]` with NaN/Infinite guards; nullable pins handling (`obj.isNull("pins") -> edit.remove(KEY_PINS)`).
  - `PreferencesManagerTest.java`: Added 7 comprehensive test cases (10 total) covering all edge cases.
- **Remaining Regressions / Gaps:**
  - `README.md` and `REPORT.md` still assert unqualified 100% parity, Small/Medium adaptive decoding, and hypothetical PSS projections without a dated addendum clarifying real-device measurement limits.
- **Status:** **SUBSTANTIALLY RESOLVED**

---

## 3. Summary Scorecard

| ID | Category | Summary | Verification Status | Notes / Remaining Gap |
|---|---|---|---|---|
| **C01** | P1 | Trim memory invalidation | **PARTIALLY RESOLVED** | Critical bug: `TechniqueRenderer` permanently disposed on trim |
| **C02** | P1 | Chakra focus loop while backgrounded | **FULLY RESOLVED** | Window/Activity lifecycle pausing & detach cleanup |
| **C03** | P1 | Runner lifecycle pause/resume | **FULLY RESOLVED** | `onResumeLifecycle` resets flag and pauses physics safely |
| **C04** | P1 | Launch failsafe & watchdog | **FULLY RESOLVED** | 8s watchdog spans prepareAsync + play; generation guards |
| **C05** | P1 | Music prepare starts after onPause | **FULLY RESOLVED** | Explicit `MusicState` enum; fenced `onPrepared` |
| **C06** | P1 | Adaptive dock card widths | **SUBSTANTIALLY RESOLVED** | Dynamic sizing in `DockView`; pickers remain fixed |
| **C07** | P1 | Closed panel resource retention | **PARTIALLY RESOLVED** | Runner still loads both heroes; closed panels held in fields |
| **C08** | P1 | Focus layering & stale targets | **PARTIALLY RESOLVED** | Stale targets cleared; root draw order remains inverted |
| **C09** | P2 | Music loop crossfade parity | **FULLY RESOLVED** | 3.5s loop overlap + 1200ms crossfade; bounded players |
| **C10** | P2 | Raster techniques & Chidori | **FULLY RESOLVED** | Disc skipped for complete art; mesh wave deform; 22-bolt Chidori |
| **C11** | P2 | Sprite paint alpha & frog flip | **FULLY RESOLVED** | Paint alpha reset to 255; flight flip inverted; slide size fixed |
| **C12** | P2 | Background parallax & motion-off fade | **UNRESOLVED** | `VillageBackgroundView` unedited; fade stalls when motion=false |
| **C13** | P2 | Runner Menu pause & touch HUD | **FULLY RESOLVED** | Menu delegated to runner; `RunnerHudLayout` touch bounds |
| **C14** | P2 | Synchronous inventory & bitmap work | **PARTIALLY RESOLVED** | Technique async decode done; `AppManager`/wallpapers sync |
| **C15** | P2 | Build gates & preference validation | **SUBSTANTIALLY RESOLVED** | Lint gates restored; JSON import validated; doc claims remain |

---

## 4. Required Action Items Before Final Acceptance

1. **Fix `TechniqueRenderer.release()` & Lifecycle Handling (C01):**
   - Replace permanent disposal (`isDisposed = true`, `decodeExecutor.shutdownNow()`) with a cache-clearing `clearBitmaps()` method that keeps the executor alive or recreates it upon demand.
   - If trim memory clears active panel child views, `MainActivity.onResume()` must re-invoke `renderScenes()` / `renderTechniques()` if that panel is currently shown.
2. **Fix `VillageBackgroundView` Crossfade Loop & Parallax (C12):**
   - Decouple the one-shot 1.6s crossfade transition progression loop from `isMotionEnabled`, ensuring scene transitions complete even when decorative particles/motion are turned off.
   - Implement clamped viewport parallax offset for background pan.
3. **Correct Root View Layering Order (C08):**
   - Reorder layout hierarchy so that the focus aura is drawn behind app icons in the dock on Home, and ensure modal dialogs do not occlude the focus layer during Library navigation.
4. **Optimize Runner & Panel Bitmap Retention (C07):**
   - Modify `RunnerGameView.loadBitmaps()` to decode only the active hero's motion and slide atlases instead of all four simultaneously.
   - In `MainActivity.closeActivePanel()`, release large bitmap caches of closed picker panels.
5. **Update Documentation (C15):**
   - Add a dated addendum to `README.md` and `REPORT.md` qualifying 100% parity claims, clarifying that Small/Medium adaptive decoding is not implemented, and noting that PSS memory metrics are targets pending physical TV / emulator profiling.
