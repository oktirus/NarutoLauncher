"""Revision-2 asset provenance, decodability and old-scene preservation checks."""
from pathlib import Path
import hashlib,json,subprocess,wave
ROOT=Path(__file__).resolve().parents[1];checks=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def check(name,ok):
 checks.append({'name':name,'passed':bool(ok)})
 if not ok:raise RuntimeError(name)
manifest=json.loads((ROOT/'revisions/v2/audio-manifest.json').read_text(encoding='utf-8'))
for row in manifest['cues']:
 p=ROOT/row['file'];check('cue hash '+p.name,sha(p)==row['sha256']);check('source preserved '+p.name,sha(ROOT/row['source_file'])==row['source_sha256'])
 with wave.open(str(p),'rb') as wav:check('short WAV '+p.name,wav.getframerate()==44100 and .15<wav.getnframes()/44100<.8)
for row in manifest['music']:
 p=ROOT/row['file'];check('official preview unmodified '+row['name'],sha(p)==row['sha256'] and p.read_bytes()==(ROOT/'revisions/v2/audio'/(row['id']+'.m4a')).read_bytes());check('30-second preview '+row['name'],29<row['duration']<31)
old_hashes={'dawn':'7c54c216c2e2f87e2ff9eb13a70b0c3eda75c3dd8e0ffc99846e43649c9a5799','dusk':'46fbca0d7f7c2a3535eb518838a4619e83891f349f6d1f7a2a09d25ca5c90876','night':'cb5e7dc23c2807620c8b121495fa14dd5c1efd9ad96e6e09b2fc819f53085fb7'}
for scene,digest in old_hashes.items():check('v1 '+scene+' backed up',sha(ROOT/'revisions/v2/before/assets'/('konoha-'+scene+'.png'))==digest)
prompts=json.loads((ROOT/'revisions/v2/generation-manifest.json').read_text(encoding='utf-8'));check('3 new edit prompts recorded',len(prompts)==3 and all(row.get('prompt') for row in prompts))
source_hashes={str(p.relative_to(ROOT)).replace('\\','/'):sha(p) for p in (ROOT/'prototype').iterdir() if p.is_file()}
(ROOT/'revisions/v2/verification/source-hashes.json').write_text(json.dumps(source_hashes,indent=2),encoding='utf-8')
(ROOT/'revisions/v2/verification/offline-checks.json').write_text(json.dumps({'passed':len(checks),'total':len(checks),'checks':checks},indent=2),encoding='utf-8')
print('PASS:',len(checks),'revision-2 audio/provenance/backup checks')
