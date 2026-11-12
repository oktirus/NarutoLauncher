const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Sharingan Dock Walk: TV Bro -> UAKino',
    timecode: '25s - 30s',
    prefs: {
      scene: 'dusk',
      technique: 'sharingan',
      audio: false,
      motion: true,
      rotation: 0,
      idle: 0,
      pins: ['preview.tvbro', 'preview.smarttube', 'preview.kodi', 'preview.lampa', 'preview.uakino', 'preview.xplore']
    },
    setupWeb: async (page) => {
      await page.evaluate(() => {
        const cards = document.querySelectorAll('.app-card');
        if (cards[0]) cards[0].focus();
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
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowRight', actionNative: 22 }, // at 1.0s -> Card 1 (SmartTube)
      { targetMs: 2000, action: 'ArrowRight', actionNative: 22 }, // at 2.0s -> Card 2 (Kodi)
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Card 3 (LAMPA)
      { targetMs: 4000, action: 'ArrowRight', actionNative: 22 }  // at 4.0s -> Card 4 (UAKino)
    ]
  };

  const video = await recordChunk('06', config);
  console.log('Resulting video:', video);
})();
