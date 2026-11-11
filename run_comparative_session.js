const { chromium } = require('playwright');
const { execSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const WORK_DIR = 'E:\\файли\\Робота\\Network\\NarutoLauncher';
const FRAMES_DIR = path.join(WORK_DIR, 'compare_frames');
if (!fs.existsSync(FRAMES_DIR)) fs.mkdirSync(FRAMES_DIR, { recursive: true });

const CHROME_PATH = 'C:\\Users\\angel\\AppData\\Local\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe';
const ADB_DEVICE = '192.168.100.26:5555';

// Unified remote keys definition
const KEY_MAP_WEB = {
  'UP': 'ArrowUp',
  'DOWN': 'ArrowDown',
  'LEFT': 'ArrowLeft',
  'RIGHT': 'ArrowRight',
  'OK': 'Enter',
  'BACK': 'Escape',
  'MENU': 'ContextMenu'
};

const KEY_MAP_ADB = {
  'UP': 19,
  'DOWN': 20,
  'LEFT': 21,
  'RIGHT': 22,
  'OK': 23,
  'BACK': 4,
  'MENU': 82
};

// ============================================================================
// THE EXACT SAME ACTION SCENARIO FOR BOTH WEB ETALON AND ANDROID NATIVE
// ============================================================================
const SCENARIO = [
  // 1. Initial baseline home screen (Card 0: TV Bro)
  { action: null, wait: 1500, label: '01_Home Screen Baseline (Card 0)', capture: '01_home_baseline' },

  // 2. Dock navigation (cards 0 through 6)
  { action: 'RIGHT', wait: 700, label: '02_Dock -> Card 1 (SmartTube)', capture: '02_dock_card1' },
  { action: 'RIGHT', wait: 700, label: '03_Dock -> Card 2 (Kodi)', capture: null },
  { action: 'RIGHT', wait: 700, label: '04_Dock -> Card 3 (LAMPA)', capture: null },
  { action: 'RIGHT', wait: 700, label: '05_Dock -> Card 4 (UAKino)', capture: null },
  { action: 'RIGHT', wait: 700, label: '06_Dock -> Card 5 (Файли)', capture: null },
  { action: 'RIGHT', wait: 1000, label: '07_Dock -> Card 6 (Весь архів)', capture: '03_dock_archive_card' },

  // 3. Open All Apps Library modal
  { action: 'OK', wait: 1600, label: '08_Open All Apps Library', capture: '04_library_modal_opened' },
  { action: 'DOWN', wait: 700, label: '09_Library Grid -> Down', capture: '05_library_grid_nav' },
  { action: 'RIGHT', wait: 600, label: '10_Library Grid -> Right', capture: null },
  { action: 'LEFT', wait: 600, label: '11_Library Grid -> Left', capture: null },
  { action: 'UP', wait: 600, label: '12_Library Grid -> Up', capture: null },
  { action: 'BACK', wait: 1400, label: '13_Close Library -> Return to Dock Card 6', capture: '06_library_closed_back_dock' },

  // 4. Return to Card 0, navigate UP to Hero Scene selector
  { action: 'LEFT', wait: 150, label: '14_Back to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '14_Back to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '14_Back to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '14_Back to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '14_Back to Card 0', capture: null },
  { action: 'LEFT', wait: 800, label: '14_At Card 0 (TV Bro)', capture: null },
  { action: 'UP', wait: 900, label: '15_Hero Scene Button Focused', capture: '07_hero_scene_btn_focused' },

  // 5. Open Scenes Panel and select "Команда №7"
  { action: 'OK', wait: 1600, label: '16_Open Scenes Gallery Modal', capture: '08_scenes_gallery_opened' },
  { action: 'RIGHT', wait: 800, label: '17_Focus Next Scene (Команда №7)', capture: '09_scene_team7_focused' },
  { action: 'OK', wait: 1800, label: '18_Select & Apply Scene Команда №7', capture: '10_scene_team7_applied' },

  // 6. Navigate to Technique Shortcut Button
  { action: 'DOWN', wait: 700, label: '19_Return Down to Dock', capture: null },
  { action: 'RIGHT', wait: 150, label: '20_Navigate to Right Edge', capture: null },
  { action: 'RIGHT', wait: 150, label: '20_Navigate to Right Edge', capture: null },
  { action: 'RIGHT', wait: 150, label: '20_Navigate to Right Edge', capture: null },
  { action: 'RIGHT', wait: 150, label: '20_Navigate to Right Edge', capture: null },
  { action: 'RIGHT', wait: 150, label: '20_Navigate to Right Edge', capture: null },
  { action: 'RIGHT', wait: 700, label: '20_At Card 6', capture: null },
  { action: 'UP', wait: 900, label: '21_Technique Shortcut Button Focused', capture: '11_technique_btn_focused' },

  // 7. Open Techniques Panel and select "Чідорі" (Chidori)
  { action: 'OK', wait: 1600, label: '22_Open Techniques Panel Modal', capture: '12_techniques_panel_opened' },
  { action: 'RIGHT', wait: 800, label: '23_Focus Chidori Technique', capture: '13_technique_chidori_focused' },
  { action: 'OK', wait: 1800, label: '24_Select & Apply Chidori', capture: '14_technique_chidori_applied' },

  // 8. Demonstrate Chidori Technique in Action across Dock
  { action: 'DOWN', wait: 800, label: '25_Down to Dock with Chidori', capture: '15_dock_chidori_active' },
  { action: 'LEFT', wait: 800, label: '26_Chidori Move Left -> Card 5', capture: null },
  { action: 'LEFT', wait: 800, label: '27_Chidori Move Left -> Card 4', capture: '16_dock_chidori_moving' },
  { action: 'LEFT', wait: 800, label: '28_Chidori Move Left -> Card 3', capture: null },

  // 9. Switch Technique to "Гамабунта" (Frog Jump)
  { action: 'RIGHT', wait: 150, label: '29_Back to Right Edge', capture: null },
  { action: 'RIGHT', wait: 150, label: '29_Back to Right Edge', capture: null },
  { action: 'RIGHT', wait: 600, label: '29_At Card 6', capture: null },
  { action: 'UP', wait: 800, label: '30_Technique Button Focused', capture: null },
  { action: 'OK', wait: 1600, label: '31_Open Techniques Panel', capture: null },
  // From chidori (0,1), navigate to gamabunta (row 3, col 0): DOWN, DOWN, DOWN, LEFT
  { action: 'DOWN', wait: 400, label: '32_Techniques Down', capture: null },
  { action: 'DOWN', wait: 400, label: '33_Techniques Down', capture: null },
  { action: 'DOWN', wait: 500, label: '34_Techniques Down to Gamabunta', capture: null },
  { action: 'LEFT', wait: 500, label: '35_Select Gamabunta', capture: '17_technique_gamabunta_focused' },
  { action: 'OK', wait: 1800, label: '36_Apply Gamabunta Technique', capture: '18_technique_gamabunta_applied' },

  // 10. Demonstrate Gamabunta Leaping across Dock
  { action: 'DOWN', wait: 800, label: '37_Down to Dock with Gamabunta', capture: '19_gamabunta_dock_idle' },
  { action: 'LEFT', wait: 1200, label: '38_Gamabunta Physical Leap Left', capture: '20_gamabunta_leap_left' },
  { action: 'LEFT', wait: 1200, label: '39_Gamabunta Physical Leap Left', capture: null },
  { action: 'RIGHT', wait: 1200, label: '40_Gamabunta Physical Leap Right', capture: '21_gamabunta_leap_right' },

  // 11. Navigate to Topbar: from Card 0 -> UP to Hero -> UP to Topbar
  { action: 'LEFT', wait: 150, label: '41_To Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '41_To Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '41_To Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '41_To Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '41_To Card 0', capture: null },
  { action: 'LEFT', wait: 700, label: '41_At Card 0', capture: null },
  { action: 'UP', wait: 600, label: '42_To Hero Scene Button', capture: null },
  { action: 'UP', wait: 700, label: '43_To Topbar (Game Button)', capture: '22_gamepad_btn_focused' },
  { action: 'RIGHT', wait: 600, label: '44_To Topbar (Settings Button)', capture: '23_settings_btn_focused' },
  { action: 'OK', wait: 1600, label: '45_Open Settings Panel Modal', capture: '24_settings_panel_opened' },
  { action: 'DOWN', wait: 600, label: '46_Settings Nav Down', capture: null },
  { action: 'DOWN', wait: 600, label: '47_Settings Nav Down', capture: '25_settings_options_nav' },
  { action: 'BACK', wait: 1200, label: '48_Close Settings -> Return to Topbar', capture: '26_settings_closed' },

  // 12. Open Konoha Runner Minigame
  { action: 'LEFT', wait: 600, label: '49_Focus Gamepad Button', capture: null },
  { action: 'OK', wait: 2400, label: '50_Launch Konoha Runner Minigame', capture: '27_runner_game_started' },
  // Play Runner actions
  { action: 'UP', wait: 1800, label: '51_Runner Jump 1', capture: '28_runner_jump1' },
  { action: 'UP', wait: 1800, label: '52_Runner Jump 2', capture: '29_runner_jump2' },
  { action: 'DOWN', wait: 1300, label: '53_Runner Crouch Slide', capture: '30_runner_crouch' },
  { action: 'BACK', wait: 1600, label: '54_Exit Runner Game back to Home', capture: '31_runner_exited_home' },

  // 13. Final Return to Home Base
  { action: 'DOWN', wait: 800, label: '55_Down to Dock', capture: null },
  { action: 'LEFT', wait: 150, label: '56_Left to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '56_Left to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '56_Left to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '56_Left to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '56_Left to Card 0', capture: null },
  { action: 'LEFT', wait: 700, label: '56_At Card 0', capture: null },
  { action: null, wait: 2000, label: '57_Final Home Rest State', capture: '32_final_resting_state' }
];

// ============================================================================
// WEB ETALON EXECUTOR (PLAYWRIGHT)
// ============================================================================
async function runWebSession() {
  console.log('\n[1/3] Starting Web Etalon (Playwright Chromium) recording...');
  const videoDir = path.join(WORK_DIR, 'web_video_tmp');
  if (!fs.existsSync(videoDir)) fs.mkdirSync(videoDir, { recursive: true });

  const browser = await chromium.launch({
    executablePath: CHROME_PATH,
    headless: true
  });

  const context = await browser.newContext({
    viewport: { width: 1366, height: 768 },
    recordVideo: { dir: videoDir, size: { width: 1366, height: 768 } }
  });

  const page = await context.newPage();

  // Reset localStorage to clean state
  await page.addInitScript(() => {
    localStorage.clear();
    localStorage.setItem('konoha_prefs', JSON.stringify({
      scene: 'dusk',
      technique: 'rasengan',
      audio: false,
      motion: true,
      rotation: 0,
      idle: 0,
      pins: [
        'preview.tvbro',
        'preview.smarttube',
        'preview.kodi',
        'preview.lampa',
        'preview.uakino',
        'preview.xplore'
      ]
    }));
  });

  await page.goto('http://localhost:8085/prototype/index.html');
  await page.waitForTimeout(2000);

  // Focus Card 0
  await page.evaluate(() => {
    const card0 = document.querySelector('#dock .app-card');
    if (card0) card0.focus();
  });
  await page.waitForTimeout(500);

  // Execute unified scenario
  for (let i = 0; i < SCENARIO.length; i++) {
    const step = SCENARIO[i];
    console.log(`[Web] Step ${i + 1}/${SCENARIO.length}: ${step.label}`);

    if (step.action) {
      const webKey = KEY_MAP_WEB[step.action];
      if (webKey) {
        await page.evaluate((k) => window.KonohaRemote(k), webKey);
      }
    }

    if (step.capture) {
      const framePath = path.join(FRAMES_DIR, `web_${step.capture}.png`);
      await page.screenshot({ path: framePath });
    }

    await page.waitForTimeout(step.wait);
  }

  // Gracefully close video recording
  await page.close();
  await context.close();
  await browser.close();

  // Find recorded WebM and convert to MP4
  const videoFiles = fs.readdirSync(videoDir).filter(f => f.endsWith('.webm'));
  if (videoFiles.length > 0) {
    const rawWebm = path.join(videoDir, videoFiles[0]);
    const finalMp4 = path.join(WORK_DIR, 'proto_session.mp4');
    console.log(`Converting ${rawWebm} -> ${finalMp4}...`);
    execSync(`ffmpeg -y -i "${rawWebm}" -c:v libx264 -pix_fmt yuv420p -r 30 "${finalMp4}"`, { stdio: 'inherit' });
    fs.rmSync(videoDir, { recursive: true, force: true });
    console.log('✓ Web Etalon MP4 ready:', finalMp4);
  }
}

// ============================================================================
// ANDROID TV EXECUTOR (ADB)
// ============================================================================
async function runAndroidSession() {
  console.log('\n[2/3] Starting Android TV (Lenovo TV 14) recording...');

  // Reset Android TV preferences to dusk + rasengan
  console.log('Configuring initial preferences on Android TV...');
  execSync(`adb -s ${ADB_DEVICE} shell "am force-stop ua.konoha.tv.nativeparity"`, { stdio: 'inherit' });
  
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
  fs.writeFileSync(path.join(WORK_DIR, 'temp_prefs.xml'), prefsXml);
  execSync(`adb -s ${ADB_DEVICE} push "${path.join(WORK_DIR, 'temp_prefs.xml')}" /data/data/ua.konoha.tv.nativeparity/shared_prefs/konoha_preferences_v1.xml`, { stdio: 'inherit' });
  fs.unlinkSync(path.join(WORK_DIR, 'temp_prefs.xml'));

  // Launch Launcher Activity
  execSync(`adb -s ${ADB_DEVICE} shell "am start -n ua.konoha.tv.nativeparity/ua.konoha.tv.nativepreview.MainActivity"`, { stdio: 'inherit' });
  await new Promise(r => setTimeout(r, 2000));

  // Ensure focus starts on Card 0 (TV Bro)
  console.log('Resetting focus to Card 0 (TV Bro)...');
  for (let i = 0; i < 7; i++) {
    execSync(`adb -s ${ADB_DEVICE} shell input keyevent 21`);
    await new Promise(r => setTimeout(r, 80));
  }
  await new Promise(r => setTimeout(r, 500));

  // Start background screenrecord
  console.log('Starting screenrecord on TV (/sdcard/tv_session.mp4)...');
  execSync(`adb -s ${ADB_DEVICE} shell "rm -f /sdcard/tv_session.mp4"`);
  const recProc = spawn('adb', ['-s', ADB_DEVICE, 'shell', 'screenrecord', '--size', '1366x768', '--bit-rate', '8000000', '--time-limit', '180', '/sdcard/tv_session.mp4']);

  await new Promise(r => setTimeout(r, 1500));

  // Execute EXACT SAME scenario
  for (let i = 0; i < SCENARIO.length; i++) {
    const step = SCENARIO[i];
    console.log(`[Android] Step ${i + 1}/${SCENARIO.length}: ${step.label}`);

    if (step.action) {
      const keyCode = KEY_MAP_ADB[step.action];
      if (keyCode) {
        execSync(`adb -s ${ADB_DEVICE} shell input keyevent ${keyCode}`);
      }
    }

    if (step.capture) {
      const localFrame = path.join(FRAMES_DIR, `native_${step.capture}.png`);
      execSync(`adb -s ${ADB_DEVICE} shell screencap -p /sdcard/tmp_cap.png`);
      execSync(`adb -s ${ADB_DEVICE} pull /sdcard/tmp_cap.png "${localFrame}"`);
    }

    await new Promise(r => setTimeout(r, step.wait));
  }

  // Gracefully stop screenrecord
  console.log('Stopping TV screenrecord gracefully (SIGINT)...');
  execSync(`adb -s ${ADB_DEVICE} shell pkill -2 screenrecord`);
  await new Promise(r => setTimeout(r, 2000));

  const nativeMp4 = path.join(WORK_DIR, 'native_session.mp4');
  console.log(`Pulling /sdcard/tv_session.mp4 -> ${nativeMp4}...`);
  execSync(`adb -s ${ADB_DEVICE} pull /sdcard/tv_session.mp4 "${nativeMp4}"`, { stdio: 'inherit' });
  execSync(`adb -s ${ADB_DEVICE} shell rm -f /sdcard/tv_session.mp4 /sdcard/tmp_cap.png`);
  console.log('✓ Android Native MP4 ready:', nativeMp4);
}

// ============================================================================
// STITCH SIDE-BY-SIDE VIDEO (FFMPEG)
// ============================================================================
function stitchSideBySide() {
  console.log('\n[3/3] Stitching Side-by-Side Comparison Video with FFmpeg...');
  const protoMp4 = path.join(WORK_DIR, 'proto_session.mp4');
  const nativeMp4 = path.join(WORK_DIR, 'native_session.mp4');
  const outputMp4 = path.join(WORK_DIR, 'compare_full_launcher_side_by_side.mp4');

  // Filter complex:
  // Scale both to exactly 1280x720 (for high compatibility 2560x720 16:9 double-wide)
  // Stack horizontally
  // Draw clear banners on each half
  const filter = [
    "[0:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,drawtext=text='1. ЕТАЛОН WEB (Prototype)':x=30:y=24:fontsize=26:fontcolor=yellow:box=1:boxcolor=black@0.7:boxborderw=8[left]",
    "[1:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,drawtext=text='2. ANDROID NATIVE TV (Lenovo)':x=30:y=24:fontsize=26:fontcolor=white:box=1:boxcolor=black@0.7:boxborderw=8[right]",
    "[left][right]hstack=inputs=2[stacked]"
  ].join('; ');

  const cmd = `ffmpeg -y -i "${protoMp4}" -i "${nativeMp4}" -filter_complex "${filter}" -map "[stacked]" -c:v libx264 -preset fast -crf 19 "${outputMp4}"`;
  console.log('Running:', cmd);
  execSync(cmd, { stdio: 'inherit' });

  // Copy output to artifacts
  const artMp4 = path.join('C:\\Users\\angel\\.gemini\\antigravity-cli\\brain\\7a75de26-a1ae-4e02-b7c4-36afa28fad09', 'compare_full_launcher_side_by_side.mp4');
  fs.copyFileSync(outputMp4, artMp4);

  console.log('\n======================================================');
  console.log('★ SUCCESS! Comparative Video Created & Copied to Artifacts:');
  console.log(outputMp4);
  console.log(artMp4);
  console.log('======================================================');
}

// Main execution coordinator
(async () => {
  try {
    await runWebSession();
    await runAndroidSession();
    stitchSideBySide();
  } catch (err) {
    console.error('Fatal execution error:', err);
    process.exit(1);
  }
})();
