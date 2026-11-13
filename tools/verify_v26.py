from pathlib import Path
import json,hashlib,re,subprocess,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v26';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
 checks.append(dict(name=name,passed=bool(ok)));assert ok,name
changed={'prototype/app.js','prototype/audio.js','prototype/runner.js','prototype/technique-art.js','prototype/index.html','android/app/build.gradle','android/app/src/main/java/ua/konoha/tv/MainActivity.java'}
for file,h in json.loads((OUT/'before-hashes.json').read_text()).items():
 if file not in changed:check('preserved '+file,sha(ROOT/file)==h)
for file in ['app.js','audio.js','runner.js','technique-art.js','runtime-assets.js']:check('syntax '+file,subprocess.run(['node','--check',str(ROOT/'prototype'/file)],capture_output=True).returncode==0)
html=(ROOT/'prototype/index.html').read_text()
for file,h in re.findall(r'(?:src|href)="([^"?]+)\?v=([a-f0-9]{12})"',html):check('content hash '+file,sha(ROOT/'prototype'/file)[:12]==h)
check('runtime asset script before effects',html.index('runtime-assets.js')<html.index('technique-art.js'))
manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));check('version 26',manifest['versionCode']==26)
apk=ROOT/'releases'/manifest['file'];check('APK hash',sha(apk).upper()==manifest['sha256'])
with zipfile.ZipFile(apk) as archive:
 for row in json.loads((ROOT/'releases/bundled-assets.json').read_text()):
  check('current bundle '+row['file'],sha(ROOT/row['file'])==row['sha256']);check('APK bytes '+row['file'],hashlib.sha256(archive.read('assets/site/'+row['file'])).hexdigest()==row['sha256'])
lint=ET.parse(OUT/'verification/android-lint.xml').getroot();check('lint no errors',not any(n.get('severity') in {'Error','Fatal'} for n in lint.findall('issue')))
for name,count in [('browser-resources',40),('browser-edges',11),('browser-launch',61),('browser-navigation',6)]:
 x=json.loads((OUT/f'verification/{name}.json').read_text(encoding='utf-8-sig'));x=x['data']['result'];x=x.get('result',x);check(name,x['passed']==count and all(c['passed'] for c in x['checks']))
physics=json.loads((OUT/'verification/runner-physics.json').read_text());check('65 game physics checks',physics['passed']==65 and all(c['passed'] for c in physics['checks']))
dock=json.loads((OUT/'verification/dock-layout.json').read_text());check('42 dock layouts and D-pad',dock['passed']==42 and dock['dpad_archive'])
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
