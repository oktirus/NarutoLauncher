const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const framesDir = 'E:\\файли\\Робота\\Network\\NarutoLauncher\\compare_frames';
const artDir = 'C:\\Users\\angel\\.gemini\\antigravity-cli\\brain\\7a75de26-a1ae-4e02-b7c4-36afa28fad09';

const keySteps = [
  '01_home_baseline',
  '04_library_modal_opened',
  '08_scenes_gallery_opened',
  '10_scene_team7_applied',
  '12_techniques_panel_opened',
  '15_dock_chidori_active',
  '20_gamabunta_leap_left',
  '23_settings_panel_opened',
  '28_runner_jump1'
];

for (const step of keySteps) {
  const webP = path.join(framesDir, `web_${step}.png`);
  const natP = path.join(framesDir, `native_${step}.png`);
  const outP = path.join(artDir, `comp_${step}.png`);

  if (fs.existsSync(webP) && fs.existsSync(natP)) {
    console.log(`Building composite for ${step}`);
    const filter = `[0:v]scale=683:384[w]; [1:v]scale=683:384[n]; [w][n]hstack=inputs=2[stacked]; [stacked]drawtext=text='Web Etalon':x=20:y=20:fontsize=20:fontcolor=yellow:box=1:boxcolor=black@0.6, drawtext=text='Android Native TV':x=703:y=20:fontsize=20:fontcolor=white:box=1:boxcolor=black@0.6`;
    execSync(`ffmpeg -y -i "${webP}" -i "${natP}" -filter_complex "${filter}" -frames:v 1 "${outP}"`);
  }
}
console.log('All composite comparison frames built!');
