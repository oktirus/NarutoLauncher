const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Close Library -> Up to Technique -> Up to Game -> Right to Settings',
    timecode: '60s - 65s',
    webTrim: '00:00:02.580',
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
        const cards = document.querySelectorAll('.app-card');
        if (cards[6]) {
          cards[6].focus();
          cards[6].click();
        }
      });
      await new Promise(r => setTimeout(r, 400));
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
      // 5. Enter to open Library
      safeAdb('shell input keyevent 23');
      await new Promise(r => setTimeout(r, 700));
    },
    actions: [
      { targetMs: 1000, action: 'Escape', actionNative: 4 },      // at 1.0s -> Close Library (returns to Card 6)
      { targetMs: 2000, action: 'ArrowUp', actionNative: 19 },    // at 2.0s -> Technique shortcut button
      { targetMs: 3000, action: 'ArrowUp', actionNative: 19 },    // at 3.0s -> Gamepad button in Topbar
      { targetMs: 4000, action: 'ArrowRight', actionNative: 22 }  // at 4.0s -> Settings button in Topbar
    ]
  };

  const video = await recordChunk('13', config);
  console.log('Resulting video:', video);
})();
