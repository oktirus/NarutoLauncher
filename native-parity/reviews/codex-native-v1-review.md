# Codex independent review — Native v1: CHANGES REQUIRED

2026-11-09. Reviewed the candidate reported by Antigravity as complete, after the independent Antigravity review. **Not accepted as feature-parity complete or ready for Lenovo deployment.** Product code was not edited by this review. Findings below refer to the frozen candidate, not any future fixes.

Candidate APK SHA256: `4e3a340ee866b0b5c4dc5e676fc2c81364894943cba0a0a41be9cb69ce3964fd`.

## Evidence and limits

- Read all native production Java classes, build/manifest configuration, tests, supplied review/resolution/report and relevant v26 implementation. Used verify-and-stop skill; orchestration is used to return fixes to the author.
- Current 128 app/src files match the staging directory recorded in the build manifest byte for byte. All 98 bundled assets match the APK, and APK SHA matches the release manifest. See `codex-v1-evidence/candidate-evidence.json`.
- Supplied JUnit XML: 21 tests, zero failures/errors. Supplied lint XML: 38 warnings, zero errors/fatals. These tests cover catalog/preferences/pure physics, **not actual Views, MediaPlayer, Canvas paint state or Activity lifecycle**. Repeated full build would not resolve those coverage gaps, so it was not performed.
- `python -X utf8 NarutoLauncher/tools/verify_v26.py`: **780/780 PASS**. Initial invocation without `-X utf8` failed from Windows cp1251 decoding; UTF-8 invocation passed. This is not a v26 product regression.
- Compiled the **unchanged** AudioController on JVM with deterministic Android API doubles and ran focused control-flow observations. Missing watchdog and delayed prepared-start after pause reproduced; normal completion/duplicate guard/muted immediate launch also checked. Script, generated doubles and results: `codex-v1-evidence/audio_probe.py`, `audio-probe.json`. This is control-flow evidence, **not real Android playback or measured timing**.
- Android emulator executable and local system images are absent. No physical-device/ADB interaction, no install, no HOME/settings/power changes, no actual native PSS or screenshot measurements.

## P1 — must fix

### C01. Trim invalidates live ImageViews and permanently removes technique art

`MainActivity.java:693`, `panels/TechniquesPanel.java:192`, `panels/ScenesPanel.java:168`, `views/TechniqueRenderer.java:42,65` (all Java paths below relative to app/src/main/java/ua/konoha/tv/nativepreview).

onTrimMemory calls release at every level >= RUNNING_LOW, including UI_HIDDEN. Panels retain child ImageViews referencing Bitmaps which release() recycles without detaching/clearing the drawables. Returning to an open picker therefore leaves Views attempting to draw recycled bitmap pixels (crash/invalid rendering risk). TechniqueRenderer clears all plates, but preloadBitmaps is called only in its constructor, so normal later drawing never restores those plates. Reproduction path: picker open -> background/trim -> return; alternatively trim on Home -> change among raster techniques.

Fix: explicit bitmap ownership, clear ImageView references before recycling, preserve visible resources or rebuild them on resume, lazy ensure/reload for the selected technique. Add tests for trim while each picker is visible, resume, and repeated trim. Android Bitmap contract: https://developer.android.com/reference/android/graphics/Bitmap#recycle(). No crash was executed on a device in this review.

### C02. Chakra frame loop keeps running while Activity is backgrounded

`views/ChakraFocusView.java:24,83`, `MainActivity.java:679`.

Runnable checks targetView, motion and **View.getVisibility()**, which remain set when the Activity loses visibility. MainActivity.onPause pauses background/game but does not pause this View or clear its target. There is also no onDetachedFromWindow callback removal. This retains scheduled work and keeps updating the focus surface while other apps are running, contrary to the memory/power purpose of this migration.

Fix: lifecycle/window-visibility-aware pause/resume and detach cleanup; verify no scheduled frames while paused, under non-library panels or ambient, and proper restoration on Home.

### C03. Runner cannot continue after Activity pause/resume

`views/RunnerGameView.java:32,149,154`, `MainActivity.java:658,679`.

onPauseLifecycle sets isPausedByLifecycle=true. MainActivity.onResume never calls the runner's onResumeLifecycle; it is called only when opening the game again. With the game still active, pressing OK starts/reposts its tick, but tick exits because the lifecycle flag is still true. Reproduction: start game -> another Activity/background -> return -> OK. Reopening the panel is an accidental recovery, not parity.

Fix: restore active game lifecycle on Activity resume without auto-starting a paused run. Verify physics stays paused until input and then advances once, without duplicate tick chains.

### C04. Launch failsafe is removed before cue playback even starts

`audio/AudioController.java:209,212,250,280`.

launchWithTechnique posts failsafeRunnable, immediately calls cleanupConfirmPlayer, and that method removes/nulls failsafeRunnable. Result: no watchdog exists throughout the cue. A missing completion/error event can leave isLaunching forever true. Synchronous MediaPlayer.prepare also blocks the main thread, preventing Back/watchdog handling during preparation.

JVM probe on unchanged source: `watchdogs_after_launch=0`. Fix cleanup ordering, asynchronous preparation, generation/cancellation guards and a watchdog spanning prepare+play. Add stalled prepare/play, cancel/retry, error, duplicate OK, completion, and muted immediate-path tests. Do not weaken full-cue completion behavior.

### C05. Music preparation can start audio after onPause

`audio/AudioController.java:137,144`, `MainActivity.java:679`.

Music registers unconditional `setOnPreparedListener(MediaPlayer::start)`. If Activity pauses before preparation finishes, pauseMusic does not cancel the preparation or fence its listener. Later completion starts music while hidden. updateMusic/pauseMusic also query isPlaying without an explicit preparation state; this is not a valid state-management design for asynchronous preparation/error handling.

JVM callback-order probe reproduces `music_starts_after_pause_during_prepare=true`. Fix with explicit lifecycle/player states, identity/generation checks, error listener and unconditional safe release paths; fence delayed callbacks after stop/replacement/pause. Official state model: https://developer.android.com/reference/android/media/MediaPlayer.

### C06. Adaptive dock regression: cards have constant width regardless of item count

`views/DockView.java:89,150`.

Both app and archive cards are fixed130dp plus12dp margins. setFillViewport expands the container but never distributes card widths. Few favorites leave most of a wide screen empty; seven cards can unnecessarily overflow a compact screen. This directly regresses the v25/v26 actual-count/full-width fix requested by the user. Other fixed-column picker/library layouts also lack width-dependent sizing.

Fix measured available width + actual count, minimum readable size and overflow only when needed. Validate 0..6 favorites plus archive, representative963/1366/1920 physical sizes with density accounted for, compact width, long labels and D-pad reachability. Geometry assertions and screenshots required; fixed130dp cannot be described as adaptive parity.

### C07. Closed panels and game still retain their full resources

`MainActivity.java:426`, `views/RunnerGameView.java:68,159`, `views/TechniqueRenderer.java:40`, `panels/ScenesPanel.java:154`, `panels/TechniquesPanel.java:174`.

closeActivePanel only removes the panel from its parent; Activity fields retain the panel trees, thumbnail maps, preview maps and runner. Game loads **both heroes' four atlases** at construction and releases only on Activity destruction, not close/background. Renderer eagerly loads all ten effect bitmaps on startup. This recreates exactly the retention/eager-loading behavior v26 was optimized to remove. Approximate RGBA pixel budgets from bundled dimensions: scene thumbnails8.90MiB, all game images8.64MiB, all effect plates5.62MiB; these are raw budgets, **not measured PSS savings**.

Fix explicit panel disposal/reconstruction or view-safe close release, active-hero-only loading, active-technique-only loading and bounded icon/resource ownership. On background release appropriate large resources and restore them on demand. Add repeated open/close, hero/technique switch, trim/resume resource-count tests.

### C08. Focus effect is layered above dock artwork and below the opaque library; target can be stale

`MainActivity.java:155,163,170,431,668`, `views/ChakraFocusView.java:45,95`, `views/DockView.java:66`, `panels/LibraryPanel.java:230`.

Root drawing order is mainShell -> ChakraFocusView -> modalContainer. On Home the technique/filled core paints over icons, losing the approved behind-icon composition. In library, its nearly opaque modal obscures the entire technique layer. onResume re-renders/removes all dock cards without preserving target identity; lastFocusedView/targetView may reference a detached old card. Focus listeners update only on gaining card focus, so moving to header/pin/search controls can leave aura over an app that will not receive OK. Unpin with filtered view also removes the target without restoring a surviving focus.

Fix an explicit active focus model, draw order that keeps technique behind app art within Home AND library, clear non-app targets, recompute coordinates after layout/scroll, restore by package/action identity after rebuild. Verify visual selection and actual OK target always match, including filtered unpin, settings roundtrip and return from launched app.

## P2 — parity and verification gaps that also need resolution

### C09. Music loop/crossfade parity is missing

`audio/AudioController.java:102-140`: single MediaPlayer, setLooping(true); switching calls stopMusic before creating the replacement. No overlap/fade scheduling exists. README claims smooth crossfade, whereas v26 audio.js schedules overlapping music loops. Implement bounded native crossfade for loop seam and track changes with timing tests; keep long tracks streamed. Document precisely what remains speaker-unverified.

### C10. Raster techniques are visually simplified and old filled-disc defect was reintroduced

`views/TechniqueRenderer.java:103-184,186-220`, versus `prototype/technique-art.js:5,59-75,169-180`.

Native draws a generic radial-gradient disc under every technique, including the five complete paintings for which v26 deliberately skips core/accents. Fireball/Amaterasu/Kurama plates are static except a tiny global scale pulse; the approved flame/tail deformation is absent. Procedural Chidori also drops the fork/glow structure. Preserve current approved visual logic using native Canvas/shaders/mesh as needed, not a generic placeholder. Compare static and multiple-time frames against v26; do not claim identical fidelity from presence of10 IDs alone.

### C11. Sprite paint state makes game heroes and Gamabunta focus translucent; frog flight flip differs

`views/RunnerGameView.java:266-289`: shared paint alpha is set60 for shadow then reused for the hero bitmap without restoring255, so hero draws at about24% alpha. `views/TechniqueRenderer.java:311-341`: shadow sets alpha40/80, then frog bitmap inherits it. Native also uses the same horizontal flip for idle/jump although v26 explicitly uses `airborne ? -direction : direction` because the two approved sprites face opposite ways. Runner uses0.95 baseline instead of runtime249/256 and ignores the distinct Gamabunta slide drawSize.

Fix paint save/reset ownership, correct opposite sprite orientations, baseline and slide metadata. Assert alpha/flip/source rects and compare screenshots for both heroes/poses and both navigation directions. These regressions correspond to defects the user previously asked to fix.

### C12. Background parallax is absent; motion-off scene changes do not get a fade frame loop

`views/VillageBackgroundView.java:46,73,121,162,257`: drawScaledBitmap always centers the image; no pointer/D-pad/time parallax is implemented. Crossfade progresses only when onDraw is called, but the animationRunnable is gated by isMotionEnabled. Choose another scene while motion=false and there is no scheduled transition progression/commit; it waits for unrelated redraws. Separate one-shot transition frames from continuous decorative motion and restore approved clamped parallax when enabled.

### C13. Runner's Menu pause is intercepted by the Activity; touch controls lost

`MainActivity.java:647`, `views/RunnerGameView.java:170-207`: Activity consumes MENU to open settings before the View can handle it as pause, although the game HUD advertises Menu pause. Full Canvas screen also lacks equivalent visible touch controls for hero selection, pause and close that exist in v26. Route active-game actions first, preserve Back cancellation precedence and provide accessible controls without reducing the requested full-width playfield.

### C14. Expensive inventory and bitmap work remains synchronous on the main thread

`apps/AppManager.java:30-70`, `MainActivity.java:94,668`, `views/VillageBackgroundView.java:73,99`, picker render/load methods. All package icons are loaded on every onResume, and scenes/gallery images decode directly in click/layout paths. The first Antigravity audit already flagged main-thread work, but the implementation completion message says all P2s resolved. Use bounded asynchronous loads with lifecycle/version guards and placeholder/error handling; avoid duplicate inventory scans on startup. Verify stale results cannot resurrect closed panels or replaced scenes.

### C15. Validation/review claims exceed evidence; regression gates must cover Android behavior

`app/build.gradle:26-29` disables lint abort on errors and release lint checks. Restore meaningful gates (explicit justified suppressions if needed). Current lint has no errors, so this is a future guard issue, not a claim that today's lint failed.

Supplied independent audit task really exists: `task_5d801521bd30`, dispatch `ctx_409078fa2746`, distinct Antigravity worker. However its report names **D01F...** pre-fix APK, ends with proposed fixes, and there is no supplied independent post-fix re-review for **4E3A...**. CANDIDATE-FREEZE includes a self-hash and older report hashes, so prefer a machine-readable manifest excluding itself and later review outputs. Re-freeze final sources/APK and obtain a real follow-up independent review.

Correct README/REPORT unsupported100% parity, small/medium adaptive decode, all-resource cleanup and crossfade assertions. Independent audit predicts55..85MiB and210..240MiB savings using299.9MiB as v26 baseline; those predictions are not supported by a native measurement, and matched final v26 Home was243.36MiB. Remove/qualify stale projections with a dated addendum preserving review history. APK/bundle bytes do not establish mmap/PSS reduction or60FPS.

Preference import also bypasses volume clamping and accepts arbitrary IDs; current default pins=null is a valid v26 state but obj.getJSONArray("pins") rejects JSON null, so a valid untouched v26 prefs object cannot import. Validate domains/types, nullable pins and record migration boundaries; test malformed/partial/default input and ensure failure is atomic. Never claim private v26 localStorage automatically migrates to a different app ID.

## Return-to-author acceptance

1. Resolve C01-C15 within native/, preserve v26 and its780/780 gate. Preserve this report and evidence unchanged; add resolution/evidence files.
2. Read development skills and reread affected call sites before each change. Add meaningful tests around the reproduced faults; compilation/catalog/physics tests alone are insufficient. The evidence probe intentionally records OLD defects and is not a passing product regression test; add a separate corrected test/harness rather than overwriting this historical evidence.
3. Build/lint/tests must match final sources and APK. Need actual UI execution/screenshots on a **local emulator**, if feasible to provision using local SDK tools. No physical Lenovo/ADB targets. If emulator execution is unavailable, explicitly retain this visual/lifecycle acceptance gap rather than claiming100% parity/ready.
4. Obtain independent Antigravity re-review against final exact hashes after material fixes, then return to Codex for final verification. No deployment/commit/push is authorized by this review.
