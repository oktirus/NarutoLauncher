const { chromium } = require('playwright');
const { execSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const WORK_DIR = 'E:\\файли\\Робота\\Network\\NarutoLauncher';
const CHUNKS_DIR = path.join(WORK_DIR, 'chunks');
if (!fs.existsSync(CHUNKS_DIR)) fs.mkdirSync(CHUNKS_DIR, { recursive: true });

const CHROME_PATH = 'C:\\Users\\angel\\AppData\\Local\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe';
const ADB_DEVICE = '192.168.100.26:5555';

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

function safeAdb(cmdStr) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      return execSync(`adb -s ${ADB_DEVICE} ${cmdStr}`, { stdio: 'pipe' });
    } catch (e) {
      if (attempt === 3) throw e;
      console.log(`[ADB Retry ${attempt}] Reconnecting after error...`);
      try { execSync(`adb connect ${ADB_DEVICE}`); } catch (_) {}
      const end = Date.now() + 500; while (Date.now() < end) {}
    }
  }
}

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function recordChunk(chunkId, config) {
  console.log(`\n======================================================`);
  console.log(`🎬 RECORDING CHUNK ${chunkId}: ${config.title}`);
  console.log(`======================================================`);

  const chunkDir = path.join(CHUNKS_DIR, `chunk_${chunkId}`);
  if (!fs.existsSync(chunkDir)) fs.mkdirSync(chunkDir, { recursive: true });

  // --------------------------------------------------------------------------
  // 1. RECORD WEB
  // --------------------------------------------------------------------------
  console.log('[Web] Setting up Web environment...');
  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true
  });
  const context = await browser.newContext({
    viewport: { width: 1366, height: 768 },
    recordVideo: { dir: chunkDir, size: { width: 1366, height: 768 } }
  });
  const pageStartTime = Date.now();
  const page = await context.newPage();

  // Apply preferences
  await page.addInitScript((prefs) => {
    localStorage.clear();
    localStorage.setItem('konoha.preferences.v1', JSON.stringify(prefs));
  }, config.prefs);

  await page.goto('http://localhost:8085/prototype/index.html');
  await sleep(1500);

  // Setup initial state
  if (config.setupWeb) {
    await config.setupWeb(page);
  }

  console.log('[Web] Starting 5.0-second scenario with absolute target timing...');
  // 1.0s lead-in before window begins
  await sleep(1000);

  const tStartWeb = Date.now();
  const webOffset = (tStartWeb - pageStartTime) / 1000;
  console.log(`[Web] Scenario tStartWeb offset in raw recording: ${webOffset.toFixed(3)}s`);
  for (const step of config.actions) {
    const target = tStartWeb + step.targetMs;
    const toWait = target - Date.now();
    if (toWait > 0) await sleep(toWait);
    console.log(`[Web] t=${((Date.now() - tStartWeb)/1000).toFixed(2)}s: ${step.action}`);
    if (step.action) {
      await page.evaluate((act) => window.KonohaRemote(act), step.action);
    }
  }

  // Wait until exactly 5000ms from tStartWeb
  const remainingWeb = (tStartWeb + 5000) - Date.now();
  if (remainingWeb > 0) await sleep(remainingWeb);
  // Add 500ms trailing padding before stopping
  await sleep(500);

  const tEndWeb = Date.now();
  await page.close();
  await context.close();
  await browser.close();

  // Find webm and trim to exact 5.000s
  const webmFiles = fs.readdirSync(chunkDir).filter(f => f.endsWith('.webm'));
  const rawWebm = path.join(chunkDir, webmFiles[0]);
  const webMp4 = path.join(chunkDir, 'web_5s.mp4');

  const rawDur = parseFloat(execSync(`ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "${rawWebm}"`).toString().trim());
  const elapsedFromStart = (tEndWeb - tStartWeb) / 1000;
  const autoTrim = Math.max(0, rawDur - elapsedFromStart);
  console.log(`[Web] Raw video duration: ${rawDur.toFixed(3)}s, scenario elapsed: ${elapsedFromStart.toFixed(3)}s => autoTrim: ${autoTrim.toFixed(3)}s`);

  const webTrim = config.webTrim || autoTrim.toFixed(3);
  execSync(`ffmpeg -y -ss ${webTrim} -t 00:00:05.000 -i "${rawWebm}" -c:v libx264 -pix_fmt yuv420p -r 30 "${webMp4}"`, { stdio: 'pipe' });
  fs.unlinkSync(rawWebm);
  console.log('✓ Web 5s video ready:', webMp4);

  // --------------------------------------------------------------------------
  // 2. RECORD ANDROID TV
  // --------------------------------------------------------------------------
  console.log('[Native] Setting up Android TV...');
  ensureAdbConnected();
  execSync(`adb -s ${ADB_DEVICE} shell "am force-stop ua.konoha.tv.nativeparity"`);
  
  // Set preferences
  const prefsXml = `<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="music">1584791975</string>
    <boolean name="motion" value="true" />
    <int name="idle" value="0" />
    <int name="rotation" value="0" />
    <string name="technique">${config.prefs.technique || 'rasengan'}</string>
    <string name="pins">[&quot;com.phlox.tvwebbrowser&quot;,&quot;org.smarttube.stable&quot;,&quot;org.xbmc.kodi&quot;,&quot;top.rootu.lampa&quot;,&quot;ua.kino&quot;,&quot;com.android.documentsui&quot;]</string>
    <string name="highlightSound">jutsu</string>
    <string name="scene">${config.prefs.scene || 'dusk'}</string>
</map>`;
  fs.writeFileSync(path.join(chunkDir, 'temp_prefs.xml'), prefsXml);
  execSync(`adb -s ${ADB_DEVICE} push "${path.join(chunkDir, 'temp_prefs.xml')}" /data/data/ua.konoha.tv.nativeparity/shared_prefs/konoha_preferences_v1.xml`);
  fs.unlinkSync(path.join(chunkDir, 'temp_prefs.xml'));

  // Start activity
  execSync(`adb -s ${ADB_DEVICE} shell "am start -n ua.konoha.tv.nativeparity/ua.konoha.tv.nativepreview.MainActivity"`);
  await sleep(2500);

  // Setup initial native state (BEFORE screenrecord)
  if (config.setupNative) {
    await config.setupNative();
    await sleep(500);
  }

  // Start screenrecord on device
  console.log('[Native] Starting screenrecord...');
  execSync(`adb -s ${ADB_DEVICE} shell "rm -f /sdcard/chunk_raw.mp4"`);
  const recStartTime = Date.now();
  const recProc = spawn('adb', ['-s', ADB_DEVICE, 'shell', 'screenrecord', '--size', '1366x768', '--bit-rate', '8000000', '--time-limit', '9', '/sdcard/chunk_raw.mp4']);
  
  // 1.0s lead-in for encoder to stabilize
  await sleep(1000);

  console.log('[Native] Starting 5.0-second scenario with absolute target timing (non-blocking)...');
  const tStartNative = Date.now();
  const nativeOffset = (tStartNative - recStartTime) / 1000;
  console.log(`[Native] Scenario tStartNative offset in raw recording: ${nativeOffset.toFixed(3)}s`);
  for (const step of config.actions) {
    const target = tStartNative + step.targetMs;
    const toWait = target - Date.now();
    if (toWait > 0) await sleep(toWait);
    console.log(`[Native] t=${((Date.now() - tStartNative)/1000).toFixed(2)}s: ${step.action}`);
    if (step.actionNative) {
      // NON-BLOCKING dispatch via spawn
      spawn('adb', ['-s', ADB_DEVICE, 'shell', 'input', 'keyevent', String(step.actionNative)]);
    }
  }

  // Wait until exactly 5000ms from tStartNative
  const remainingNative = (tStartNative + 5000) - Date.now();
  if (remainingNative > 0) await sleep(remainingNative);
  // Add 500ms trailing padding
  await sleep(500);

  console.log('[Native] Stopping screenrecord...');
  execSync(`adb -s ${ADB_DEVICE} shell pkill -2 screenrecord`);
  await sleep(1500);

  const rawTvMp4 = path.join(chunkDir, 'tv_raw.mp4');
  execSync(`adb -s ${ADB_DEVICE} pull /sdcard/chunk_raw.mp4 "${rawTvMp4}"`);
  execSync(`adb -s ${ADB_DEVICE} shell rm -f /sdcard/chunk_raw.mp4`);

  const nativeMp4 = path.join(chunkDir, 'native_5s.mp4');
  execSync(`ffmpeg -y -ss 00:00:01.000 -t 00:00:05.000 -i "${rawTvMp4}" -c:v libx264 -pix_fmt yuv420p -r 30 "${nativeMp4}"`, { stdio: 'pipe' });
  fs.unlinkSync(rawTvMp4);
  console.log('✓ Native 5s video ready:', nativeMp4);

  // --------------------------------------------------------------------------
  // 3. STITCH SIDE-BY-SIDE
  // --------------------------------------------------------------------------
  const sideBySideMp4 = path.join(chunkDir, `chunk_${chunkId}_side_by_side.mp4`);
  const filter = [
    `[0:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,drawtext=text='1. WEB [${config.timecode}]':x=30:y=24:fontsize=26:fontcolor=yellow:box=1:boxcolor=black@0.7:boxborderw=8[left]`,
    `[1:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,drawtext=text='2. NATIVE TV [${config.timecode}]':x=30:y=24:fontsize=26:fontcolor=white:box=1:boxcolor=black@0.7:boxborderw=8[right]`,
    `[left][right]hstack=inputs=2[stacked]`
  ].join('; ');

  execSync(`ffmpeg -y -i "${webMp4}" -i "${nativeMp4}" -filter_complex "${filter}" -map "[stacked]" -c:v libx264 -preset fast -crf 19 "${sideBySideMp4}"`, { stdio: 'pipe' });
  console.log('★ Stitched Chunk Video Ready:', sideBySideMp4);

  // Copy to artifacts
  const artMp4 = path.join('C:\\Users\\angel\\.gemini\\antigravity-cli\\brain\\7a75de26-a1ae-4e02-b7c4-36afa28fad09', `chunk_${chunkId}_side_by_side.mp4`);
  fs.copyFileSync(sideBySideMp4, artMp4);

  // --------------------------------------------------------------------------
  // 4. EXTRACT 5 CONTROL FRAMES (at 0.5s, 1.5s, 2.5s, 3.5s, 4.5s)
  // --------------------------------------------------------------------------
  console.log('[Verify] Extracting 5 checkpoint frames...');
  for (let s = 1; s <= 5; s++) {
    const t = (s - 0.5).toFixed(2);
    const framePng = path.join(chunkDir, `frame_s${s}.png`);
    execSync(`ffmpeg -y -ss 00:00:0${t} -i "${sideBySideMp4}" -vframes 1 "${framePng}"`, { stdio: 'pipe' });
  }
  console.log('✓ 5 checkpoint frames extracted.');
  return sideBySideMp4;
}

module.exports = { recordChunk, safeAdb };
