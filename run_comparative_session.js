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
// THE EXACT SAME UNIFIED ACTION SCENARIO FOR BOTH WEB ETALON AND ANDROID NATIVE
// Covers ALL 10 Navigation Techniques, Dock walking, Library, Scenes, Topbar, Settings, Runner
// ============================================================================
const SCENARIO = [
  // --------------------------------------------------------------------------
  // 1. BASELINE HOME SCREEN (Rasengan on Card 0)
  // --------------------------------------------------------------------------
  { action: null, wait: 1500, label: '01_Home Baseline (Rasengan, Card 0)', capture: '01_home_baseline' },

  // Walk dock with Rasengan from Card 0 to Card 6
  { action: 'RIGHT', wait: 750, label: '02_Rasengan -> Card 1 (SmartTube)', capture: '02_tech01_rasengan_dock' },
  { action: 'RIGHT', wait: 750, label: '03_Rasengan -> Card 2 (Kodi)', capture: null },
  { action: 'RIGHT', wait: 750, label: '04_Rasengan -> Card 3 (LAMPA)', capture: null },
  { action: 'RIGHT', wait: 750, label: '05_Rasengan -> Card 4 (UAKino)', capture: null },
  { action: 'RIGHT', wait: 750, label: '06_Rasengan -> Card 5 (Файли)', capture: null },
  { action: 'RIGHT', wait: 900, label: '07_Rasengan -> Card 6 (Весь архів)', capture: null },
  // Open Techniques panel and select Technique 2: Chidori
  { action: 'UP', wait: 800, label: '08_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '09_Open Techniques Panel (at Rasengan)', capture: '03_panel_at_rasengan' },
  { action: 'RIGHT', wait: 800, label: '10_Select Chidori Card', capture: '04_panel_select_chidori' },
  { action: 'OK', wait: 1500, label: '11_Apply Chidori (Lands at Card 0)', capture: '05_tech02_chidori_applied' },

  // --------------------------------------------------------------------------
  // 2. TECHNIQUE 2: CHIDORI
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '12_Chidori -> Card 1', capture: '06_tech02_chidori_dock' },
  { action: 'RIGHT', wait: 750, label: '13_Chidori -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '14_Chidori -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '15_Chidori -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '16_Chidori -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '17_Chidori -> Card 6', capture: null },
  // Open Techniques panel and select Technique 3: Sharingan
  { action: 'UP', wait: 800, label: '18_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '19_Open Techniques Panel (at Chidori)', capture: null },
  { action: 'RIGHT', wait: 800, label: '20_Select Sharingan Card', capture: '07_panel_select_sharingan' },
  { action: 'OK', wait: 1500, label: '21_Apply Sharingan (Lands at Card 0)', capture: '08_tech03_sharingan_applied' },

  // --------------------------------------------------------------------------
  // 3. TECHNIQUE 3: SHARINGAN
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '22_Sharingan -> Card 1', capture: '09_tech03_sharingan_dock' },
  { action: 'RIGHT', wait: 750, label: '23_Sharingan -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '24_Sharingan -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '25_Sharingan -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '26_Sharingan -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '27_Sharingan -> Card 6', capture: null },
  // Open Techniques panel and select Technique 4: Kurama
  { action: 'UP', wait: 800, label: '28_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '29_Open Techniques Panel (at Sharingan)', capture: null },
  { action: 'DOWN', wait: 600, label: '30_Panel -> Down (Hiraishin)', capture: null },
  { action: 'LEFT', wait: 600, label: '31_Panel -> Left (Summoning)', capture: null },
  { action: 'LEFT', wait: 700, label: '32_Select Kurama Card', capture: '10_panel_select_kurama' },
  { action: 'OK', wait: 1500, label: '33_Apply Kurama (Lands at Card 0)', capture: '11_tech04_kurama_applied' },

  // --------------------------------------------------------------------------
  // 4. TECHNIQUE 4: CHAKRA KURAMA
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '34_Kurama -> Card 1', capture: '12_tech04_kurama_dock' },
  { action: 'RIGHT', wait: 750, label: '35_Kurama -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '36_Kurama -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '37_Kurama -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '38_Kurama -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '39_Kurama -> Card 6', capture: null },
  // Open Techniques panel and select Technique 5: Frog Summoning
  { action: 'UP', wait: 800, label: '40_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '41_Open Techniques Panel (at Kurama)', capture: null },
  { action: 'RIGHT', wait: 800, label: '42_Select Summoning Card', capture: '13_panel_select_summoning' },
  { action: 'OK', wait: 1500, label: '43_Apply Summoning (Lands at Card 0)', capture: '14_tech05_summoning_applied' },

  // --------------------------------------------------------------------------
  // 5. TECHNIQUE 5: FROG SUMMONING (ВИКЛИК ЖАБИ)
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '44_Summoning -> Card 1', capture: '15_tech05_summoning_dock' },
  { action: 'RIGHT', wait: 750, label: '45_Summoning -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '46_Summoning -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '47_Summoning -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '48_Summoning -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '49_Summoning -> Card 6', capture: null },
  // Open Techniques panel and select Technique 6: Hiraishin
  { action: 'UP', wait: 800, label: '50_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '51_Open Techniques Panel (at Summoning)', capture: null },
  { action: 'RIGHT', wait: 800, label: '52_Select Hiraishin Card', capture: '16_panel_select_hiraishin' },
  { action: 'OK', wait: 1500, label: '53_Apply Hiraishin (Lands at Card 0)', capture: '17_tech06_hiraishin_applied' },

  // --------------------------------------------------------------------------
  // 6. TECHNIQUE 6: HIRAISHIN (ХІРАЙШИН)
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '54_Hiraishin -> Card 1', capture: '18_tech06_hiraishin_dock' },
  { action: 'RIGHT', wait: 750, label: '55_Hiraishin -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '56_Hiraishin -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '57_Hiraishin -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '58_Hiraishin -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '59_Hiraishin -> Card 6', capture: null },
  // Open Techniques panel and select Technique 7: Fireball
  { action: 'UP', wait: 800, label: '60_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '61_Open Techniques Panel (at Hiraishin)', capture: null },
  { action: 'DOWN', wait: 600, label: '62_Panel -> Down (Rasenshuriken)', capture: null },
  { action: 'LEFT', wait: 600, label: '63_Panel -> Left (Amaterasu)', capture: null },
  { action: 'LEFT', wait: 700, label: '64_Select Fireball Card', capture: '19_panel_select_fireball' },
  { action: 'OK', wait: 1500, label: '65_Apply Fireball (Lands at Card 0)', capture: '20_tech07_fireball_applied' },

  // --------------------------------------------------------------------------
  // 7. TECHNIQUE 7: FIREBALL (ВОГНЯНА КУЛЯ)
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '66_Fireball -> Card 1', capture: '21_tech07_fireball_dock' },
  { action: 'RIGHT', wait: 750, label: '67_Fireball -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '68_Fireball -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '69_Fireball -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '70_Fireball -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '71_Fireball -> Card 6', capture: null },
  // Open Techniques panel and select Technique 8: Amaterasu
  { action: 'UP', wait: 800, label: '72_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '73_Open Techniques Panel (at Fireball)', capture: null },
  { action: 'RIGHT', wait: 800, label: '74_Select Amaterasu Card', capture: '22_panel_select_amaterasu' },
  { action: 'OK', wait: 1500, label: '75_Apply Amaterasu (Lands at Card 0)', capture: '23_tech08_amaterasu_applied' },

  // --------------------------------------------------------------------------
  // 8. TECHNIQUE 8: AMATERASU (АМАТЕРАСУ)
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '76_Amaterasu -> Card 1', capture: '24_tech08_amaterasu_dock' },
  { action: 'RIGHT', wait: 750, label: '77_Amaterasu -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '78_Amaterasu -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '79_Amaterasu -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '80_Amaterasu -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '81_Amaterasu -> Card 6', capture: null },
  // Open Techniques panel and select Technique 9: Rasenshuriken
  { action: 'UP', wait: 800, label: '82_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '83_Open Techniques Panel (at Amaterasu)', capture: null },
  { action: 'RIGHT', wait: 800, label: '84_Select Rasenshuriken Card', capture: '25_panel_select_rasenshuriken' },
  { action: 'OK', wait: 1500, label: '85_Apply Rasenshuriken (Lands at Card 0)', capture: '26_tech09_rasenshuriken_applied' },

  // --------------------------------------------------------------------------
  // 9. TECHNIQUE 9: RASENSHURIKEN (РАСЕНСЮРИКЕН)
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 750, label: '86_Rasenshuriken -> Card 1', capture: '27_tech09_rasenshuriken_dock' },
  { action: 'RIGHT', wait: 750, label: '87_Rasenshuriken -> Card 2', capture: null },
  { action: 'RIGHT', wait: 750, label: '88_Rasenshuriken -> Card 3', capture: null },
  { action: 'RIGHT', wait: 750, label: '89_Rasenshuriken -> Card 4', capture: null },
  { action: 'RIGHT', wait: 750, label: '90_Rasenshuriken -> Card 5', capture: null },
  { action: 'RIGHT', wait: 900, label: '91_Rasenshuriken -> Card 6', capture: null },
  // Open Techniques panel and select Technique 10: Gamabunta Leap
  { action: 'UP', wait: 800, label: '92_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '93_Open Techniques Panel (at Rasenshuriken)', capture: null },
  { action: 'LEFT', wait: 600, label: '94_Panel -> Left (Amaterasu)', capture: null },
  { action: 'LEFT', wait: 600, label: '95_Panel -> Left (Fireball)', capture: null },
  { action: 'DOWN', wait: 700, label: '96_Select Gamabunta Card', capture: '28_panel_select_gamabunta' },
  { action: 'OK', wait: 1500, label: '97_Apply Gamabunta (Lands at Card 0)', capture: '29_tech10_gamabunta_applied' },

  // --------------------------------------------------------------------------
  // 10. TECHNIQUE 10: GAMABUNTA LEAP (СТРИБОК ГАМАБУНТИ) - PHYSICAL LEAP DEMO
  // --------------------------------------------------------------------------
  { action: null, wait: 1000, label: '98_Gamabunta Resting Perch on Card 0', capture: '30_gamabunta_rest' },
  // Leap across all dock cards to the right
  { action: 'RIGHT', wait: 1100, label: '99_Gamabunta Leap -> Card 1', capture: '31_gamabunta_leap_r1' },
  { action: 'RIGHT', wait: 1100, label: '100_Gamabunta Leap -> Card 2', capture: null },
  { action: 'RIGHT', wait: 1100, label: '101_Gamabunta Leap -> Card 3', capture: null },
  { action: 'RIGHT', wait: 1100, label: '102_Gamabunta Leap -> Card 4', capture: null },
  { action: 'RIGHT', wait: 1100, label: '103_Gamabunta Leap -> Card 5', capture: null },
  { action: 'RIGHT', wait: 1200, label: '104_Gamabunta Leap -> Card 6 (Archive)', capture: '32_gamabunta_leap_r6' },
  // Leap across all dock cards backwards to the left
  { action: 'LEFT', wait: 1100, label: '105_Gamabunta Leap Back -> Card 5', capture: '33_gamabunta_leap_l5' },
  { action: 'LEFT', wait: 1100, label: '106_Gamabunta Leap Back -> Card 4', capture: null },
  { action: 'LEFT', wait: 1100, label: '107_Gamabunta Leap Back -> Card 3', capture: null },
  { action: 'LEFT', wait: 1100, label: '108_Gamabunta Leap Back -> Card 2', capture: null },
  { action: 'LEFT', wait: 1100, label: '109_Gamabunta Leap Back -> Card 1', capture: null },
  { action: 'LEFT', wait: 1200, label: '110_Gamabunta Leap Back -> Card 0', capture: '34_gamabunta_leap_l0' },
  // Return to Technique panel and reset to Rasengan
  { action: 'RIGHT', wait: 200, label: '111_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '111_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '111_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '111_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '111_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 600, label: '111_At Card 6', capture: null },
  { action: 'UP', wait: 800, label: '112_Focus Technique Button', capture: null },
  { action: 'OK', wait: 1200, label: '113_Open Techniques Panel (at Gamabunta)', capture: null },
  { action: 'UP', wait: 500, label: '114_Panel -> Up (Fireball)', capture: null },
  { action: 'UP', wait: 500, label: '115_Panel -> Up (Kurama)', capture: null },
  { action: 'UP', wait: 600, label: '116_Select Rasengan Card', capture: null },
  { action: 'OK', wait: 1500, label: '117_Reset to Rasengan (Lands at Card 0)', capture: null },

  // --------------------------------------------------------------------------
  // 11. ALL APPS LIBRARY MODAL DEMO
  // --------------------------------------------------------------------------
  { action: 'RIGHT', wait: 200, label: '118_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '118_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '118_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '118_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 200, label: '118_Walk to Card 6', capture: null },
  { action: 'RIGHT', wait: 700, label: '118_At Card 6 (Archive)', capture: null },
  { action: 'OK', wait: 1600, label: '119_Open All Apps Library Modal', capture: '35_library_opened' },
  { action: 'DOWN', wait: 700, label: '120_Library Grid -> Down', capture: '36_library_grid_nav' },
  { action: 'RIGHT', wait: 600, label: '121_Library Grid -> Right', capture: null },
  { action: 'LEFT', wait: 600, label: '122_Library Grid -> Left', capture: null },
  { action: 'UP', wait: 600, label: '123_Library Grid -> Up', capture: null },
  { action: 'BACK', wait: 1400, label: '124_Close Library (Returns to Card 6)', capture: '37_library_closed' },

  // --------------------------------------------------------------------------
  // 12. WALLPAPERS / SCENES GALLERY DEMO
  // --------------------------------------------------------------------------
  { action: 'LEFT', wait: 150, label: '125_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '125_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '125_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '125_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '125_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 700, label: '125_At Card 0', capture: null },
  { action: 'UP', wait: 800, label: '126_Focus Scene Choice Button', capture: '38_scene_btn_focused' },
  { action: 'OK', wait: 1600, label: '127_Open Scenes Gallery Modal', capture: '39_scenes_gallery_opened' },
  { action: 'RIGHT', wait: 800, label: '128_Focus Next Scene (Команда №7)', capture: '40_scene_team7_focused' },
  { action: 'OK', wait: 1800, label: '129_Apply Scene Команда №7', capture: '41_scene_team7_applied' },

  // --------------------------------------------------------------------------
  // 13. TOPBAR & SETTINGS MODAL DEMO
  // --------------------------------------------------------------------------
  { action: 'UP', wait: 800, label: '130_Focus Brand Logo in Topbar', capture: '42_brand_logo_focused' },
  { action: 'RIGHT', wait: 600, label: '131_Focus Gamepad Button', capture: '43_gamepad_btn_focused' },
  { action: 'RIGHT', wait: 600, label: '132_Focus Settings Button', capture: '44_settings_btn_focused' },
  { action: 'OK', wait: 1600, label: '133_Open Settings Modal', capture: '45_settings_modal_opened' },
  { action: 'DOWN', wait: 600, label: '134_Settings Nav Down', capture: null },
  { action: 'DOWN', wait: 600, label: '135_Settings Nav Down', capture: '46_settings_options_nav' },
  { action: 'BACK', wait: 1200, label: '136_Close Settings (Returns to Settings Btn)', capture: '47_settings_closed' },

  // --------------------------------------------------------------------------
  // 14. KONOHA RUNNER MINIGAME DEMO ("Шлях шинобі")
  // --------------------------------------------------------------------------
  { action: 'LEFT', wait: 600, label: '137_Focus Gamepad Button', capture: null },
  { action: 'OK', wait: 2400, label: '138_Launch Konoha Runner Minigame', capture: '48_runner_started' },
  { action: 'UP', wait: 1800, label: '139_Runner Jump 1', capture: '49_runner_jump1' },
  { action: 'UP', wait: 1800, label: '140_Runner Jump 2', capture: '50_runner_jump2' },
  { action: 'DOWN', wait: 1300, label: '141_Runner Crouch Slide', capture: '51_runner_crouch' },
  { action: 'BACK', wait: 1600, label: '142_Exit Runner Back to Home', capture: '52_runner_exited' },

  // --------------------------------------------------------------------------
  // 15. FINAL RESTING STATE
  // --------------------------------------------------------------------------
  { action: 'DOWN', wait: 800, label: '143_Down to Dock', capture: null },
  { action: 'LEFT', wait: 150, label: '144_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '144_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '144_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '144_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 150, label: '144_Walk to Card 0', capture: null },
  { action: 'LEFT', wait: 800, label: '144_At Card 0', capture: null },
  { action: null, wait: 2000, label: '145_Final Home Rest State', capture: '53_final_rest_state' }
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
    localStorage.setItem('konoha.preferences.v1', JSON.stringify({
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
  execSync(`adb -s ${ADB_DEVICE} shell "am force-stop com.phlox.tvwebbrowser"`, { stdio: 'inherit' });
  execSync(`adb -s ${ADB_DEVICE} shell "am force-stop org.smarttube.stable"`, { stdio: 'inherit' });
  execSync(`adb -s ${ADB_DEVICE} shell "am force-stop org.xbmc.kodi"`, { stdio: 'inherit' });

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
  await new Promise(r => setTimeout(r, 2500));

  // Ensure focus starts on Card 0 (TV Bro)
  console.log('Resetting focus to Card 0 (TV Bro)...');
  for (let i = 0; i < 7; i++) {
    execSync(`adb -s ${ADB_DEVICE} shell input keyevent 21`);
    await new Promise(r => setTimeout(r, 100));
  }
  await new Promise(r => setTimeout(r, 800));

  // Start background screenrecord (time-limit 0 = unlimited)
  console.log('Starting screenrecord on TV (/sdcard/tv_session.mp4)...');
  execSync(`adb -s ${ADB_DEVICE} shell "rm -f /sdcard/tv_session.mp4"`);
  const recProc = spawn('adb', ['-s', ADB_DEVICE, 'shell', 'screenrecord', '--size', '1366x768', '--bit-rate', '8000000', '--time-limit', '0', '/sdcard/tv_session.mp4']);

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
  await new Promise(r => setTimeout(r, 3000));

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
