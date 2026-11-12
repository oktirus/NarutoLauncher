const { recordChunk, safeAdb } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Archive -> Open Modal -> Navigate to Gamabunta',
    timecode: '45s - 50s',
    webTrim: '00:00:02.500',
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
        const cards = document.querySelectorAll('.app-card');
        if (cards[6]) cards[6].focus();
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
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowUp', actionNative: 19 },    // at 1.0s -> technique shortcut button
      { targetMs: 2000, action: 'Enter', actionNative: 23 },      // at 2.0s -> Open Modal (hiraishin focused)
      { targetMs: 3000, action: 'ArrowDown', actionNative: 20 },  // at 3.0s -> rasenshuriken
      { targetMs: 4000, action: 'ArrowDown', actionNative: 20 }   // at 4.0s -> gamabunta
    ]
  };

  const video = await recordChunk('10', config);
  console.log('Resulting video:', video);
})();
