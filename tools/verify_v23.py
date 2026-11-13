from pathlib import Path
import json,hashlib,re,subprocess,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v23';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
 checks.append(dict(name=name,passed=bool(ok)))
 assert ok,name
def registry(p,globalName):return json.loads(p.read_text(encoding='utf8').split('window.'+globalName+'=')[1].strip().rstrip(';'))
for file,hash in json.loads((OUT/'before-hashes.json').read_text()).items():
 if file not in {'prototype/index.html','prototype/scenes.js','prototype/quotes.js','prototype/app.js'}:check('preserved '+file,sha(ROOT/file)==hash)
before=registry(OUT/'before/prototype/scenes.js','KonohaScenes');after=registry(ROOT/'prototype/scenes.js','KonohaScenes')
check('18 scenes',len(after)==18)
check('training removed',not any(s['id']=='night' or 'Тренування Расен' in s['name'] for s in after))
check('team7 replaces old slot',[s['id'] for s in after]==['team7' if s['id']=='night' else s['id'] for s in before])
for old in before:
 if old['id']!='night':check('exact other scene '+old['id'],old==next(s for s in after if s['id']==old['id']))
quotes=registry(ROOT/'prototype/quotes.js','KonohaQuotes');oldQuotes=registry(OUT/'before/prototype/quotes.js','KonohaQuotes')
check('quote rekey only',{('team7' if k=='night' else k):v for k,v in oldQuotes.items()}==quotes)
for file in ['app.js','scenes.js','quotes.js']:check('syntax '+file,subprocess.run(['node','--check',str(ROOT/'prototype'/file)],capture_output=True).returncode==0)
manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));apk=ROOT/'releases'/manifest['file']
check('APK manifest SHA256',sha(apk).upper()==manifest['sha256'])
with zipfile.ZipFile(apk) as archive:
 for row in json.loads((ROOT/'releases/bundled-assets.json').read_text()):
  check('bundle current '+row['file'],sha(ROOT/row['file'])==row['sha256'])
  check('APK exact bytes '+row['file'],hashlib.sha256(archive.read('assets/site/'+row['file'])).hexdigest()==row['sha256'])
 check('no training wallpaper in APK',not any('scene-night' in name for name in archive.namelist()))
lint=ET.parse(OUT/'verification/android-lint.xml').getroot();check('lint no errors',not any(n.get('severity') in {'Error','Fatal'} for n in lint.findall('issue')))
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
