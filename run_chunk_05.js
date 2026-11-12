const { recordChunk } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Select Sharingan + Apply to Dock',
    timecode: '20s - 25s',
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
        if (cards[6]) cards[6].focus();
      });
      await new Promise(r => setTimeout(r, 250));
    },
    setupNative: async () => {
      const { execSync } = require('child_process');
      // 1. Close any modal
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 4');
      await new Promise(r => setTimeout(r, 300));
      // 2. Down twice to ensure focus is on dock
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 20');
      await new Promise(r => setTimeout(r, 200));
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 20');
      await new Promise(r => setTimeout(r, 200));
      // 3. 7x Left to Card 0
      for (let i = 0; i < 7; i++) {
        execSync('adb -s 192.168.100.26:5555 shell input keyevent 21');
        await new Promise(r => setTimeout(r, 150));
      }
      await new Promise(r => setTimeout(r, 300));
      // 4. 6x Right to Card 6 (Весь архів)
      for (let i = 0; i < 6; i++) {
        execSync('adb -s 192.168.100.26:5555 shell input keyevent 22');
        await new Promise(r => setTimeout(r, 180));
      }
      await new Promise(r => setTimeout(r, 400));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowUp', actionNative: 19 },    // at 1.0s -> techniqueBtn
      { targetMs: 2000, action: 'Enter', actionNative: 23 },      // at 2.0s -> Open Modal (Chidori focused)
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Sharingan
      { targetMs: 4000, action: 'Enter', actionNative: 23 }       // at 4.0s -> Apply Sharingan (lands on Card 0)
    ]
  };

  const video = await recordChunk('05', config);
  console.log('Resulting video:', video);
})();
