const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Select Hiraishin (ArrowDown in Modal) + Dock Walk to Kodi',
    timecode: '35s - 40s',
    webTrim: '00:00:02.580',
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
        const btn = document.querySelector('.technique-shortcut');
        if (btn) btn.click();
      });
      await new Promise(r => setTimeout(r, 250));
      await page.evaluate(() => {
        const cards = document.querySelectorAll('.technique-card');
        if (cards[2]) cards[2].focus();
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
      // 6. Enter to open modal (Sharingan focused by default since technique is sharingan)
      safeAdb('shell input keyevent 23');
      await new Promise(r => setTimeout(r, 700));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowDown', actionNative: 20 },  // at 1.0s -> Hiraishin (Хірайшин)
      { targetMs: 2000, action: 'Enter', actionNative: 23 },      // at 2.0s -> Apply Hiraishin (lands on Card 0)
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Card 1 (SmartTube)
      { targetMs: 4000, action: 'ArrowRight', actionNative: 22 }  // at 4.0s -> Card 2 (Kodi)
    ]
  };

  const video = await recordChunk('08', config);
  console.log('Resulting video:', video);
})();
