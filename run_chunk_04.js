const { recordChunk } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Chidori Dock Walk: Kodi -> Archive',
    timecode: '15s - 20s',
    prefs: {
      scene: 'dusk',
      technique: 'chidori',
      audio: false,
      motion: true,
      rotation: 0,
      idle: 0,
      pins: ['preview.tvbro', 'preview.smarttube', 'preview.kodi', 'preview.lampa', 'preview.uakino', 'preview.xplore']
    },
    setupWeb: async (page) => {
      await page.evaluate(() => {
        const cards = document.querySelectorAll('.app-card');
        if (cards[2]) cards[2].focus();
      });
      await new Promise(r => setTimeout(r, 250));
    },
    setupNative: async () => {
      const { execSync } = require('child_process');
      // 1. Close any modal
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 4');
      await new Promise(r => setTimeout(r, 400));
      // 2. Down twice to ensure focus is on dock
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 20');
      await new Promise(r => setTimeout(r, 200));
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 20');
      await new Promise(r => setTimeout(r, 200));
      // 3. 7x Left to Card 0
      for (let i = 0; i < 7; i++) {
        execSync('adb -s 192.168.100.26:5555 shell input keyevent 21');
        await new Promise(r => setTimeout(r, 180));
      }
      await new Promise(r => setTimeout(r, 300));
      // 4. 2x Right to Card 2 (Kodi)
      for (let i = 0; i < 2; i++) {
        execSync('adb -s 192.168.100.26:5555 shell input keyevent 22');
        await new Promise(r => setTimeout(r, 250));
      }
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowRight', actionNative: 22 }, // at 1.0s -> Card 3 (Lampa)
      { targetMs: 2000, action: 'ArrowRight', actionNative: 22 }, // at 2.0s -> Card 4 (UAKino)
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Card 5 (X-plore / Файли)
      { targetMs: 4000, action: 'ArrowRight', actionNative: 22 }  // at 4.0s -> Card 6 (Весь архів)
    ]
  };

  const video = await recordChunk('04', config);
  console.log('Resulting video:', video);
})();
