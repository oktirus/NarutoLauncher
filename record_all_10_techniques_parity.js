const { chromium } = require('playwright');
const { execSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const WORK_DIR = __dirname;
const ART_DIR = process.env.KONOHA_ARTIFACT_DIR || path.join(WORK_DIR, 'verification', 'comparisons');
const CHROME_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined;
const ADB_DEVICE = '192.168.100.26:5555';

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function ensureAdbConnected() {
  try {
    const out = execSync('adb devices').toString();
    if (!out.includes(ADB_DEVICE) || out.includes('offline')) {
      console.log(`[Native] Reconnecting to ${ADB_DEVICE}...`);
      execSync(`adb connect ${ADB_DEVICE}`);
    }
  } catch (e) {
    console.error('Error ensuring ADB connection:', e);
  }
}

// ----------------------------------------------------------------------------
// FULL 10-TECHNIQUE SCENARIO SEQUENCE
// ----------------------------------------------------------------------------
// 10 Techniques in order:
// 0: rasengan
// 1: chidori
// 2: sharingan
// 3: kurama
// 4: summoning
// 5: hiraishin
// 6: fireball
// 7: amaterasu
// 8: rasenshuriken
// 9: gamabunta
//
// Key mappings:
// ArrowRight: 22
// ArrowLeft: 21
// ArrowUp: 19
// ArrowDown: 20
// Enter: 23
// Escape: 4
// ----------------------------------------------------------------------------

function buildScenario() {
  const actions = [];
  let t = 800; // start after 0.8s lead-in

  // Helper to add an action
  function add(action, actionNative, waitMs, note) {
    actions.push({ targetMs: t, action, actionNative, note });
    t += waitMs;
  }

  // Helper for dock traverse: from Card 0 to Card 6
  function dockTraverse(techName) {
    for (let c = 1; c <= 6; c++) {
      add('ArrowRight', 22, 500, `Dock -> Card ${c} (${techName})`);
    }
    // Pause at Card 6
    t += 300;
  }

  // Open modal from Card 6
  function openTechniquesModal() {
    add('ArrowUp', 19, 400, 'Up to technique shortcut button');
    add('Enter', 23, 700, 'Open techniques modal');
  }

  // --- TECH 0: RASENGAN (Initial on dock) ---
  // Already active on Card 0
  dockTraverse('01_Rasengan');

  // --- TECH 1: CHIDORI ---
  openTechniquesModal();
  add('ArrowRight', 22, 400, 'Modal: Right to Chidori');
  add('Enter', 23, 700, 'Modal: Select Chidori -> applies & focuses Card 0');
  dockTraverse('02_Chidori');

  // --- TECH 2: SHARINGAN ---
  openTechniquesModal();
  add('ArrowRight', 22, 400, 'Modal: Right to Sharingan');
  add('Enter', 23, 700, 'Modal: Select Sharingan -> applies & focuses Card 0');
  dockTraverse('03_Sharingan');

  // --- TECH 3: KURAMA ---
  openTechniquesModal();
  add('ArrowLeft', 21, 300, 'Modal: Left to Chidori');
  add('ArrowLeft', 21, 300, 'Modal: Left to Rasengan');
  add('ArrowDown', 20, 400, 'Modal: Down to Kurama');
  add('Enter', 23, 700, 'Modal: Select Kurama -> applies & focuses Card 0');
  dockTraverse('04_Kurama');

  // --- TECH 4: SUMMONING (Виклик жаби) ---
  openTechniquesModal();
  add('ArrowRight', 22, 400, 'Modal: Right to Summoning');
  add('Enter', 23, 700, 'Modal: Select Summoning -> applies & focuses Card 0');
  dockTraverse('05_Summoning');

  // --- TECH 5: HIRAISHIN (Хірайшин) ---
  openTechniquesModal();
  add('ArrowRight', 22, 400, 'Modal: Right to Hiraishin');
  add('Enter', 23, 700, 'Modal: Select Hiraishin -> applies & focuses Card 0');
  dockTraverse('06_Hiraishin');

  // --- TECH 6: FIREBALL (Вогняна куля) ---
  openTechniquesModal();
  add('ArrowLeft', 21, 300, 'Modal: Left to Summoning');
  add('ArrowLeft', 21, 300, 'Modal: Left to Kurama');
  add('ArrowDown', 20, 400, 'Modal: Down to Fireball');
  add('Enter', 23, 700, 'Modal: Select Fireball -> applies & focuses Card 0');
  dockTraverse('07_Fireball');

  // --- TECH 7: AMATERASU (Аматерасу) ---
  openTechniquesModal();
  add('ArrowRight', 22, 400, 'Modal: Right to Amaterasu');
  add('Enter', 23, 700, 'Modal: Select Amaterasu -> applies & focuses Card 0');
  dockTraverse('08_Amaterasu');

  // --- TECH 8: RASENSHURIKEN (Расенсюрикен) ---
  openTechniquesModal();
  add('ArrowRight', 22, 400, 'Modal: Right to Rasenshuriken');
  add('Enter', 23, 700, 'Modal: Select Rasenshuriken -> applies & focuses Card 0');
  dockTraverse('09_Rasenshuriken');

  // --- TECH 9: GAMABUNTA (Стрибок Гамабунти) ---
  openTechniquesModal();
  add('ArrowLeft', 21, 300, 'Modal: Left to Amaterasu');
  add('ArrowLeft', 21, 300, 'Modal: Left to Fireball');
  add('ArrowDown', 20, 400, 'Modal: Down to Gamabunta');
  add('Enter', 23, 800, 'Modal: Select Gamabunta -> applies & focuses Card 0');
  dockTraverse('10_Gamabunta');

  // --- SETTINGS DRAWER VERIFICATION ---
  add('ArrowUp', 19, 400, 'Up to technique shortcut button');
  add('ArrowUp', 19, 400, 'Up to topbar');
  add('ArrowRight', 22, 400, 'Right to Settings button');
  add('Enter', 23, 1500, 'Open Settings panel ("Шлях ніндзя")');
  add('ArrowDown', 20, 600, 'Settings: Down to Highlight sound');
  add('ArrowDown', 20, 600, 'Settings: Down to Living world toggle');
  add('ArrowDown', 20, 600, 'Settings: Down to Jutsu sounds toggle');
  add('Escape', 4, 1000, 'Back out of Settings');

  const totalDurationMs = t + 1000;
  return { actions, totalDurationMs };
}

async function run() {
  console.log('===============================================================');
  console.log('🚀 RECORDING MASTER 10-TECHNIQUE FULL LAUNCHER COMPARISON');
  console.log('===============================================================');

  const { actions, totalDurationMs } = buildScenario();
  const totalSeconds = Math.ceil(totalDurationMs / 1000);
  console.log(`Total scenario steps: ${actions.length}, Duration: ${totalSeconds}s (${totalDurationMs}ms)`);

  const outDir = path.join(WORK_DIR, 'compare_10_tech');
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

  const webRawDir = path.join(outDir, 'web_raw');
  if (!fs.existsSync(webRawDir)) fs.mkdirSync(webRawDir, { recursive: true });

  // --------------------------------------------------------------------------
  // 1. RECORD WEB
  // --------------------------------------------------------------------------
  console.log('\n[Web] Launching Chrome Playwright...');
  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true
  });
  const context = await browser.newContext({
    viewport: { width: 1366, height: 768 },
    recordVideo: { dir: webRawDir, size: { width: 1366, height: 768 } }
  });
  const pageStartTime = Date.now();
  const page = await context.newPage();

  // Initial preferences: rasengan, dusk, matching pins
  await page.addInitScript(() => {
    localStorage.clear();
    localStorage.setItem('konoha.preferences.v1', JSON.stringify({
      scene: 'dusk',
      technique: 'rasengan',
      audio: false,
      motion: true,
      pins: ['preview.tvbro', 'preview.smarttube', 'preview.kodi', 'preview.lampa', 'preview.uakino', 'preview.xplore']
    }));
  });

  await page.goto('http://localhost:8085/prototype/index.html');
  await sleep(1500);

  // Set initial focus to Card 0
  await page.evaluate(() => {
    const card0 = document.querySelector('#dock .app-card');
    if (card0) card0.focus();
  });

  console.log(`[Web] Starting ${totalSeconds}s execution...`);
  await sleep(1000);
  const tStartWeb = Date.now();

  for (const step of actions) {
    const target = tStartWeb + step.targetMs;
    const toWait = target - Date.now();
    if (toWait > 0) await sleep(toWait);
    await page.evaluate((act) => window.KonohaRemote(act), step.action);
  }

  const remainingWeb = (tStartWeb + totalDurationMs) - Date.now();
  if (remainingWeb > 0) await sleep(remainingWeb);
  await sleep(500);

  const tEndWeb = Date.now();
  await page.close();
  await context.close();
  await browser.close();

  const webmFiles = fs.readdirSync(webRawDir).filter(f => f.endsWith('.webm'));
  const rawWebm = path.join(webRawDir, webmFiles[0]);
  const webMp4 = path.join(outDir, 'web_full.mp4');

  const rawDur = parseFloat(execSync(`ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "${rawWebm}"`).toString().trim());
  const elapsedFromStart = (tEndWeb - tStartWeb) / 1000;
  const autoTrim = Math.max(0, rawDur - elapsedFromStart);
  console.log(`[Web] Raw duration: ${rawDur.toFixed(3)}s, scenario elapsed: ${elapsedFromStart.toFixed(3)}s => trim: ${autoTrim.toFixed(3)}s`);

  execSync(`ffmpeg -y -ss ${autoTrim.toFixed(3)} -t 00:0${Math.floor(totalSeconds/60)}:${String(totalSeconds%60).padStart(2, '0')}.000 -i "${rawWebm}" -c:v libx264 -pix_fmt yuv420p -r 30 "${webMp4}"`, { stdio: 'pipe' });
  console.log('✓ Web full video ready:', webMp4);

  // --------------------------------------------------------------------------
  // 2. RECORD ANDROID TV
  // --------------------------------------------------------------------------
  console.log('\n[Native] Preparing Android TV...');
  ensureAdbConnected();
  execSync(`adb -s ${ADB_DEVICE} shell "am force-stop ua.konoha.tv.nativeparity"`);

  // Initial preferences: rasengan, dusk, matching pins
  const prefsXml = `<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="music">1584791975</string>
    <boolean name="motion" value="true" />
    <int name="idle" value="0" />
    <int name="rotation" value="0" />
    <string name="technique">rasengan</string>
    <string name="pins">[&quot;com.phlox.tvwebbrowser&quot;,&quot;org.smarttube.stable&quot;,&quot;org.xbmc.kodi&quot;,&quot;top.rootu.lampa&quot;,&quot;ua.kino&quot;,&quot;com.android.documentsui&quot;]</string>
    <string name="highlightSound">jutsu</string>
    <string name="scene">dusk</string>
</map>`;
  fs.writeFileSync(path.join(outDir, 'temp_prefs.xml'), prefsXml);
  execSync(`adb -s ${ADB_DEVICE} push "${path.join(outDir, 'temp_prefs.xml')}" /data/data/ua.konoha.tv.nativeparity/shared_prefs/konoha_preferences_v1.xml`);
  fs.unlinkSync(path.join(outDir, 'temp_prefs.xml'));

  // Start activity
  execSync(`adb -s ${ADB_DEVICE} shell "am start -n ua.konoha.tv.nativeparity/ua.konoha.tv.nativepreview.MainActivity"`);
  await sleep(2500);

  // Ensure focus is on Card 0
  for (let i = 0; i < 7; i++) {
    execSync(`adb -s ${ADB_DEVICE} shell "input keyevent 21"`);
  }
  await sleep(500);

  console.log(`[Native] Starting screenrecord for ${totalSeconds}s...`);
  execSync(`adb -s ${ADB_DEVICE} shell "rm -f /sdcard/full_raw.mp4"`);
  const recProc = spawn('adb', ['-s', ADB_DEVICE, 'shell', 'screenrecord', '--size', '1366x768', '--bit-rate', '8000000', '--time-limit', String(totalSeconds + 10), '/sdcard/full_raw.mp4']);

  // 1.0s lead-in for encoder
  await sleep(1000);
  const tStartNative = Date.now();

  for (const step of actions) {
    const target = tStartNative + step.targetMs;
    const toWait = target - Date.now();
    if (toWait > 0) await sleep(toWait);
    spawn('adb', ['-s', ADB_DEVICE, 'shell', 'input', 'keyevent', String(step.actionNative)]);
  }

  const remainingNative = (tStartNative + totalDurationMs) - Date.now();
  if (remainingNative > 0) await sleep(remainingNative);
  await sleep(500);

  console.log('[Native] Stopping screenrecord...');
  execSync(`adb -s ${ADB_DEVICE} shell pkill -2 screenrecord`);
  await sleep(2000);

  const rawTvMp4 = path.join(outDir, 'tv_raw.mp4');
  execSync(`adb -s ${ADB_DEVICE} pull /sdcard/full_raw.mp4 "${rawTvMp4}"`);
  execSync(`adb -s ${ADB_DEVICE} shell rm -f /sdcard/full_raw.mp4`);

  const nativeMp4 = path.join(outDir, 'native_full.mp4');
  execSync(`ffmpeg -y -ss 00:00:01.000 -t 00:0${Math.floor(totalSeconds/60)}:${String(totalSeconds%60).padStart(2, '0')}.000 -i "${rawTvMp4}" -c:v libx264 -pix_fmt yuv420p -r 30 "${nativeMp4}"`, { stdio: 'pipe' });
  fs.unlinkSync(rawTvMp4);
  console.log('✓ Native full video ready:', nativeMp4);

  // --------------------------------------------------------------------------
  // 3. STITCH SIDE-BY-SIDE
  // --------------------------------------------------------------------------
  console.log('\n[Stitch] Creating side-by-side split screen video...');
  const sideBySideMp4 = path.join(outDir, 'compare_all_10_techniques_side_by_side.mp4');
  const filter = [
    `[0:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,drawtext=text='1. ЕТАЛОН WEB (Prototype)':x=30:y=24:fontsize=26:fontcolor=yellow:box=1:boxcolor=black@0.7:boxborderw=8[left]`,
    `[1:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,drawtext=text='2. ANDROID NATIVE TV (Lenovo TV 14)':x=30:y=24:fontsize=26:fontcolor=white:box=1:boxcolor=black@0.7:boxborderw=8[right]`,
    `[left][right]hstack=inputs=2[stacked]`
  ].join('; ');

  execSync(`ffmpeg -y -i "${webMp4}" -i "${nativeMp4}" -filter_complex "${filter}" -map "[stacked]" -c:v libx264 -preset fast -crf 19 "${sideBySideMp4}"`, { stdio: 'pipe' });
  console.log('★ Stitched Master Video Ready:', sideBySideMp4);

  // Copy to brain artifacts
  const artMp4 = path.join(ART_DIR, 'compare_all_10_techniques_side_by_side.mp4');
  fs.copyFileSync(sideBySideMp4, artMp4);
  console.log('★ Copied to brain artifacts:', artMp4);

  // --------------------------------------------------------------------------
  // 4. EXTRACT CHECKPOINT FRAMES FOR EACH TECHNIQUE & SETTINGS
  // --------------------------------------------------------------------------
  console.log('\n[Frames] Extracting checkpoint frames for all 10 techniques + settings...');
  const checkpoints = [
    { name: '01_rasengan', time: '00:00:02.500' },
    { name: '02_chidori', time: '00:00:08.500' },
    { name: '03_sharingan', time: '00:00:14.500' },
    { name: '04_kurama', time: '00:00:21.000' },
    { name: '05_summoning', time: '00:00:27.500' },
    { name: '06_hiraishin', time: '00:00:34.000' },
    { name: '07_fireball', time: '00:00:41.000' },
    { name: '08_amaterasu', time: '00:00:47.500' },
    { name: '09_rasenshuriken', time: '00:00:54.000' },
    { name: '10_gamabunta_leap', time: '00:01:01.000' },
    { name: '11_settings_drawer', time: '00:01:08.000' }
  ];

  for (const cp of checkpoints) {
    const framePath = path.join(ART_DIR, `parity_frame_${cp.name}.png`);
    execSync(`ffmpeg -y -ss ${cp.time} -i "${sideBySideMp4}" -vframes 1 "${framePath}"`, { stdio: 'pipe' });
    console.log(`✓ Checkpoint frame saved: parity_frame_${cp.name}.png (${cp.time})`);
  }

  console.log('\n🎉 ALL DONE! Master video and 11 checkpoint frames ready for independent review.');
}

run().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
