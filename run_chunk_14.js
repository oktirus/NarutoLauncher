const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Topbar Settings -> Open Settings Panel -> Down -> Escape (Close)',
    timecode: '65s - 70s',
    webTrim: '00:00:02.500',
    prefs: {
      scene: 'dusk',
      technique: 'gamabunta',
      audio: false,
      motion: true,
      rotation: 0,
      idle: 0,
      pins: ['preview.tvbro', 'preview.smarttube', 'preview.kodi', 'preview.lampa', 'preview.uakino', 'preview.xplore']
    },
    setupWeb: async (page) => {
      await page.evaluate(() => {
        const btn = document.querySelector('[data-action="settings"]');
        if (btn) btn.focus();
      });
      await new Promise(r => setTimeout(r, 250));
    },
    setupNative: async () => {
      // 1. Close any modal
      safeAdb('shell input keyevent 4');
      await new Promise(r => setTimeout(r, 300));
      // 2. Down twice to ensure focus is on dock
      safeAdb('shell input keyevent 20');
      await new Promise(r => setTimeout(r, 200));
      safeAdb('shell input keyevent 20');
      await new Promise(r => setTimeout(r, 200));
      // 3. 7x Left to Card 0 (clamped)
      for (let i = 0; i < 7; i++) {
        safeAdb('shell input keyevent 21');
        await new Promise(r => setTimeout(r, 150));
      }
      await new Promise(r => setTimeout(r, 300));
      // 4. 6x Right to Card 6 (Весь архів)
      for (let i = 0; i < 6; i++) {
        safeAdb('shell input keyevent 22');
        await new Promise(r => setTimeout(r, 180));
      }
      await new Promise(r => setTimeout(r, 300));
      // 5. Up to techniqueBtn
      safeAdb('shell input keyevent 19');
      await new Promise(r => setTimeout(r, 250));
      // 6. Up to gameBtn
      safeAdb('shell input keyevent 19');
      await new Promise(r => setTimeout(r, 250));
      // 7. Right to settingsBtn
      safeAdb('shell input keyevent 22');
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'Enter', actionNative: 23 },      // at 1.0s -> Open Settings
      { targetMs: 2000, action: 'ArrowDown', actionNative: 20 },  // at 2.0s -> Down through settings
      { targetMs: 3000, action: 'ArrowDown', actionNative: 20 },  // at 3.0s -> Down through settings
      { targetMs: 4000, action: 'Escape', actionNative: 4 }       // at 4.0s -> Close Settings (returns to topbar)
    ]
  };

  const video = await recordChunk('14', config);
  console.log('Resulting video:', video);
})();
