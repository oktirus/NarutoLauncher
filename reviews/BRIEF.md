# Independent review — Konoha candidate

User requested a separate Naruto-only launcher while preserving existing Yoru,
all UI elements inventoried, references downloaded, generated icons and backgrounds,
animated Naruto-like focus, dynamic scenery, atmospheric signature SFX; two independent
reviews by Claude and Antigravity through Orca; substantial findings addressed before
the final interactive mockup is delivered.

Scope now: local **interactive prototype**, not APK installation or production readiness.
Preview: http://127.0.0.1:8767/prototype/ (server already running, localhost only).

Read these first:

1. `design/ELEMENT-INVENTORY.md` and `design/PLAN.md`.
2. `README.md`, `research/generation-manifest.json`, `research/asset-manifest.json`.
3. `prototype/index.html`, `prototype/style.css`, `prototype/app.js`.
4. `verification/candidate-home-1920.png`, `candidate-chakra-focus.png`,
   `candidate-library.png`, `candidate-home-960.png`, `candidate-mobile.png`.
5. `design/icon-board.jpg`, `research/reference-board.jpg`.
6. `verification/candidate-checks-*.json`, `verification/offline-checks.json`.

There are 3 generated Konoha panoramas, 10 genuinely transparent illustrated icons,
5 locally synthesized audio assets. No copied anime OST/dialogue. Asset sources and
generation prompts preserved. 122 Yoru files have a byte-level preservation baseline.

Review independently. Do not read the other agent's report or messages. No code edits,
no device/network writes, no git mutations, no public upload. You may write only your
assigned report `reviews/claude-review.md` or `reviews/antigravity-review.md`.

Please assess:

- Visual coherence and atmosphere: one Naruto world, icon consistency/readability,
  scene composition, typography and hierarchy, no generic multi-anime residue.
- Focus animation: chakra ring, moving trail, OK burst; legible selected element,
  keyboard vs mouse behavior, reduced motion, scroll/resize geometry.
- Dynamic background: separate motion layers and scene transitions; performance or
  lifecycle problems with actual code evidence. Do not assume RAM is constrained.
- Sound: activation, mute/volume, ambience lifecycle, distinction of events. Audio
  files decoding is verified; real TV physical output is explicitly unverified.
- Interaction: D-pad reachability, panel/back behavior, search, favorites, empty state,
  persistence, responsive geometry, honesty of Android-only shortcuts.
- Code-level bugs, security and accessibility that matter to this prototype.

Use screenshots visually. If browsing, create your **own isolated** browser session
(e.g. `konoha-claude-review` / `konoha-antigravity-review`), not the coordinator's.
Do not navigate or change the user's existing Orca tab. You may run read-only checks.

Report format: verdict, P1/P2 findings with `path:line`, concrete evidence/reproduction,
impact and minimal recommendation; distinguish confirmed defects from suggestions.
Then list top 3 artistic improvements only if they improve the actual brief. If no
blocking issue, say so. No need for speculative redesign. Report Ukrainian preferred.

Task completion means review/report and worker_done, regardless of whether you find
defects. A check returning non-zero is evidence to report, not permission to change code.
Send findings back to the coordinator with the report path and priorities.
