from pathlib import Path
import json,hashlib,re,subprocess,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v24';checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
 checks.append(dict(name=name,passed=bool(ok)))
 assert ok,name
changed={'prototype/app.js','prototype/audio.js','prototype/index.html','android/app/build.gradle'}
for file,hash in json.loads((OUT/'before-hashes.json').read_text()).items():
 if file not in changed:check('preserved '+file,sha(ROOT/file)==hash)
for file in ['app.js','audio.js']:check('syntax '+file,subprocess.run(['node','--check',str(ROOT/'prototype'/file)],capture_output=True).returncode==0)
html=(ROOT/'prototype/index.html').read_text(encoding='utf8');expected=(OUT/'before/prototype/index.html').read_text(encoding='utf8')
for file in ['app.js','audio.js']:expected=re.sub(re.escape(file)+r'\?v=[a-zA-Z0-9]+',file+'?v='+sha(ROOT/'prototype'/file)[:12],expected)
check('HTML only two content hashes',html==expected)
manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));check('version 24',manifest['versionCode']==24)
apk=ROOT/'releases'/manifest['file'];check('APK hash',sha(apk).upper()==manifest['sha256'])
with zipfile.ZipFile(apk) as archive:
 for row in json.loads((ROOT/'releases/bundled-assets.json').read_text()):
  check('current bundle '+row['file'],sha(ROOT/row['file'])==row['sha256'])
  check('APK bytes '+row['file'],hashlib.sha256(archive.read('assets/site/'+row['file'])).hexdigest()==row['sha256'])
lint=ET.parse(OUT/'verification/android-lint.xml').getroot();check('lint no errors',not any(n.get('severity') in {'Error','Fatal'} for n in lint.findall('issue')))
(OUT/'verification/offline.json').write_text(json.dumps(checks,indent=2),encoding='utf8');print(f'{len(checks)}/{len(checks)} passed')
