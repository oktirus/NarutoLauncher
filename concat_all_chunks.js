const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const WORK_DIR = 'E:\\файли\\Робота\\Network\\NarutoLauncher';
const CHUNKS_DIR = path.join(WORK_DIR, 'chunks');
const ART_DIR = 'C:\\Users\\angel\\.gemini\\antigravity-cli\\brain\\7a75de26-a1ae-4e02-b7c4-36afa28fad09';

const chunkFiles = [];
for (let i = 1; i <= 15; i++) {
  const id = String(i).padStart(2, '0');
  const mp4 = path.join(CHUNKS_DIR, `chunk_${id}`, `chunk_${id}_side_by_side.mp4`);
  if (!fs.existsSync(mp4)) {
    console.error(`Missing chunk: ${mp4}`);
    process.exit(1);
  }
  chunkFiles.push(`file '${mp4.replace(/\\/g, '/')}'`);
}

const listPath = path.join(CHUNKS_DIR, 'concat_list.txt');
fs.writeFileSync(listPath, chunkFiles.join('\n'));
console.log('Concat list written with 15 chunks.');

const finalMp4 = path.join(WORK_DIR, 'compare_full_launcher_side_by_side_v2.mp4');
console.log('Concatenating into full 75-second video...');
execSync(`ffmpeg -y -f concat -safe 0 -i "${listPath}" -c copy "${finalMp4}"`, { stdio: 'inherit' });
console.log('★ Full Video Ready:', finalMp4);

const artFinalMp4 = path.join(ART_DIR, 'compare_full_launcher_side_by_side_v2.mp4');
fs.copyFileSync(finalMp4, artFinalMp4);
console.log('★ Copied to brain artifacts:', artFinalMp4);
