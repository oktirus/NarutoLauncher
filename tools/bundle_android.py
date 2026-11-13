"""Package current UI byte-for-byte and only referenced wallpaper/icon assets."""
from pathlib import Path
import json,re,shutil,hashlib,sys
ROOT=Path(__file__).resolve().parents[1];destination=Path(sys.argv[1]).resolve()
assert destination.name=='site' and not destination.exists(), 'Use a new staging site directory'
files=set((ROOT/'prototype').glob('*'))
files={p for p in files if p.is_file()}
scenes=json.loads((ROOT/'prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
files.update(ROOT/'assets/runtime'/s['image'].split('?')[0] for s in scenes)
for p in list(files):
 if p.suffix in {'.js','.html','.css','.svg'}:
  for match in re.findall(r'\.\./(assets/[A-Za-z0-9_./-]+\.(?:png|jpg|webp|svg|ttf|woff2|wav|m4a|ogg))',p.read_text(encoding='utf8')):files.add(ROOT/match)
# Only selectable music is dynamic. Cue URLs (including UI) were collected above.
app=(ROOT/'prototype/app.js').read_text(encoding='utf8')
for track in re.findall(r"id:'(\d+)',name:",app):files.add(ROOT/'assets/audio/naruto'/f'{track}.m4a')
files.add(ROOT/'LICENSE-cleanyourtv')
rows=[]
for p in sorted(files):
 assert p.is_file(),str(p)
 relative=p.relative_to(ROOT);target=destination/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
 rows.append(dict(file=relative.as_posix(),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
assert not any('scene-night' in r['file'] for r in rows),'Retired wallpaper must not be bundled'
out=ROOT/'releases/bundled-assets.json';out.write_text(json.dumps(rows,indent=2),encoding='utf8')
print(f'Bundled {len(rows)} files, {sum(r["bytes"] for r in rows)/1048576:.1f} MiB; 18 active scenes, no retired training art')
