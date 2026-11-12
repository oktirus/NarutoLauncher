const { recordChunk } = require('./run_chunk.js');

(async () => {
  const config = {
    title: 'Select Chidori + Apply to Dock Navigation',
    timecode: '10s - 15s',
    prefs: {
      scene: 'dusk',
      technique: 'rasengan',
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
        const activeCard = document.querySelector('.technique-card[aria-pressed="true"]') || document.querySelector('.technique-card');
        if (activeCard) activeCard.focus();
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
      await new Promise(r => setTimeout(r, 100));
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 20');
      await new Promise(r => setTimeout(r, 100));
      // 3. 7x Left to Card 0
      for (let i = 0; i < 7; i++) {
        execSync('adb -s 192.168.100.26:5555 shell input keyevent 21');
        await new Promise(r => setTimeout(r, 80));
      }
      await new Promise(r => setTimeout(r, 150));
      // 4. 6x Right to Card 6 (Archive)
      for (let i = 0; i < 6; i++) {
        execSync('adb -s 192.168.100.26:5555 shell input keyevent 22');
        await new Promise(r => setTimeout(r, 120));
      }
      await new Promise(r => setTimeout(r, 200));
      // 5. Up to techniqueBtn
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 19');
      await new Promise(r => setTimeout(r, 200));
      // 6. Enter to open modal
      execSync('adb -s 192.168.100.26:5555 shell input keyevent 23');
      await new Promise(r => setTimeout(r, 700));
    },
    actions: [
      { targetMs: 1000, action: 'ArrowRight', actionNative: 22 }, // at 1.0s -> Chidori
      { targetMs: 2000, action: 'Enter', actionNative: 23 },      // at 2.0s -> Apply Chidori (lands on Card 0)
      { targetMs: 3000, action: 'ArrowRight', actionNative: 22 }, // at 3.0s -> Card 1
      { targetMs: 4000, action: 'ArrowRight', actionNative: 22 }  // at 4.0s -> Card 2
    ]
  };

  const video = await recordChunk('03', config);
  console.log('Resulting video:', video);
})();
