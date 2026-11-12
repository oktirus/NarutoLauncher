const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Gamabunta Leaps to Archive -> Open All Apps Library',
    timecode: '55s - 60s',
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
        const cards = document.querySelectorAll('.app-card');
        if (cards[3]) cards[3].focus();
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
      // 4. 3x Right to Card 3 (LAMPA)
      for (let i = 0; i < 3; i++) {
        safeAdb('shell input keyevent 22');
        await new Promise(r => setTimeout(r, 180));
      }
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowRight', actionNative: 22 }, // at 1.0s -> Card 4 (UAKino) - Gamabunta leaps!
      { targetMs: 2000, action: 'ArrowRight', actionNative: 22 }, // at 2.0s -> Card 5 (Files / X-plore) - Gamabunta leaps!
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Card 6 (Весь архів) - Gamabunta leaps!
      { targetMs: 4000, action: 'Enter', actionNative: 23 }       // at 4.0s -> Open All Apps Library!
    ]
  };

  const video = await recordChunk('12', config);
  console.log('Resulting video:', video);
})();
