const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Apply Gamabunta (Summoning) + Dock Leaps (TV Bro -> LAMPA)',
    timecode: '50s - 55s',
    webTrim: '00:00:02.580',
    prefs: {
      scene: 'dusk',
      technique: 'hiraishin',
      audio: false,
      motion: true,
      rotation: 0,
      idle: 0,
      pins: ['preview.tvbro', 'preview.smarttube', 'preview.kodi', 'preview.lampa', 'preview.uakino', 'preview.xplore']
    },
    setupWeb: async (page) => {
      await page.evaluate(() => {
        const btn = document.querySelector('.technique-shortcut');
        if (btn) btn.click();
      });
      await new Promise(r => setTimeout(r, 250));
      await page.evaluate(() => {
        const card = document.querySelector('.technique-card[data-technique="gamabunta"]');
        if (card) card.focus();
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
      // 6. Enter to open modal (Hiraishin focused by default)
      safeAdb('shell input keyevent 23');
      await new Promise(r => setTimeout(r, 700));
      // 7. Down twice to Gamabunta
      safeAdb('shell input keyevent 20');
      await new Promise(r => setTimeout(r, 250));
      safeAdb('shell input keyevent 20');
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'Enter', actionNative: 23 },      // at 1.0s -> Apply Gamabunta (modal closes, lands on Card 0 TV Bro)
      { targetMs: 2000, action: 'ArrowRight', actionNative: 22 }, // at 2.0s -> Card 1 (SmartTube) - Gamabunta leaps!
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Card 2 (Kodi) - Gamabunta leaps!
      { targetMs: 4000, action: 'ArrowRight', actionNative: 22 }  // at 4.0s -> Card 3 (LAMPA) - Gamabunta leaps!
    ]
  };

  const video = await recordChunk('11', config);
  console.log('Resulting video:', video);
})();
