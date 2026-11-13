"""Replace the just-installed v26 candidate; v25 APK+DPAPI data backup stays intact."""
from pathlib import Path
from datetime import datetime,timezone
import subprocess,json,hashlib
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v26/device';ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555';log=[]
def run(*args):
 r=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True,encoding='utf8',timeout=120);log.append(dict(time_utc=datetime.now(timezone.utc).isoformat(),command=['adb','-s',SERIAL,*args],exit=r.returncode,output=r.stdout,error=r.stderr));(OUT/'final-update.json').write_text(json.dumps(log,ensure_ascii=False,indent=2),encoding='utf8');assert r.returncode==0,r.stderr;return r.stdout.strip()
assert run('shell','getprop','ro.product.model')=='LNVNB161216';assert run('shell','getprop','ro.product.device')=='x86_64_tv_go'
home_args=('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME');before=run(*home_args);assert before.endswith('ua.konoha.tv/.MainActivity')
path=run('shell','pm','path','ua.konoha.tv').removeprefix('package:');expected=json.loads((OUT/'candidate-build-manifest.json').read_text(encoding='utf-8-sig'));assert run('shell','sha256sum',path).split()[0].upper()==expected['sha256']
assert (OUT/'backup/v25.apk').is_file() and (OUT/'backup/app-data.dpapi').is_file()
m=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));apk=ROOT/'releases'/m['file'];assert m['versionCode']==26 and hashlib.sha256(apk.read_bytes()).hexdigest().upper()==m['sha256']
assert 'Success' in run('install','-r',str(apk));assert 'Status: ok' in run('shell','am','start','-W','-n','ua.konoha.tv/.MainActivity')
assert run(*home_args)==before;path=run('shell','pm','path','ua.konoha.tv').removeprefix('package:');assert run('shell','sha256sum',path).split()[0].upper()==m['sha256'];print('Final v26 installed, APK SHA verified, HOME preserved')
