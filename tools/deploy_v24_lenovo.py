"""Back up and update the explicitly authorized Lenovo target without clearing data."""
from pathlib import Path
from datetime import datetime,timezone
import json,subprocess,hashlib,importlib.util
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v24/device';ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555';PKG='ua.konoha.tv'
log=[]
def sha(data):return hashlib.sha256(data).hexdigest()
def run(*args):
 r=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True,timeout=120)
 log.append(dict(time_utc=datetime.now(timezone.utc).isoformat(),command=['adb','-s',SERIAL,*args],exit_code=r.returncode,output=r.stdout.decode('utf8',errors='replace').strip(),error=r.stderr.decode('utf8',errors='replace').strip()))
 (OUT/'deployment.json').write_text(json.dumps(log,ensure_ascii=False,indent=2),encoding='utf8')
 if r.returncode:raise RuntimeError('ADB operation failed: '+str(args))
 return log[-1]['output']
def blob(*args):return subprocess.check_output([str(ADB),'-s',SERIAL,*args],timeout=120)
def home():return run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
assert run('shell','getprop','ro.product.model')=='LNVNB161216'
assert run('shell','getprop','ro.product.device')=='x86_64_tv_go'
before=home();assert before.splitlines()[-1]=='ua.konoha.tv/.MainActivity'
backup=OUT/'backup';backup.mkdir(exist_ok=False)
path=run('shell','pm','path',PKG).removeprefix('package:');old=blob('exec-out','cat',path)
expected=json.loads((ROOT/'revisions/v24/before/build-manifest.json').read_text(encoding='utf-8-sig'))
assert sha(old).upper()==expected['sha256'],'Installed version differs from expected v23; inspect before updating'
(backup/'v23.apk').write_bytes(old);run('shell','am','force-stop',PKG)
raw=blob('exec-out','run-as',PKG,'tar','-czf','-','--exclude=cache','--exclude=code_cache','.')
spec=importlib.util.spec_from_file_location('recovery',ROOT.parent/'Lenovo_IdeaPad_120S-11IAP_LineageOS_TV/tools/recovery/recovery.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
encrypted=module.protected(raw);assert module.protected(encrypted,decrypt=True)==raw
(backup/'app-data.dpapi').write_bytes(encrypted)
(backup/'manifest.json').write_text(json.dumps(dict(apk_sha256=sha(old),data_sha256=sha(raw),encrypted_sha256=sha(encrypted),protection='Windows DPAPI CurrentUser',plaintext_saved=False),indent=2),encoding='utf8')
manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));assert manifest['versionCode']==24
apk=ROOT/'releases'/manifest['file'];assert sha(apk.read_bytes()).upper()==manifest['sha256']
assert 'Success' in run('install','-r',str(apk))
launch=run('shell','am','start','-W','-n',PKG+'/.MainActivity');assert 'Status: ok' in launch
path=run('shell','pm','path',PKG).removeprefix('package:');installed=run('shell','sha256sum',path).split()[0];assert installed.upper()==manifest['sha256']
after=home();assert before==after
result=dict(package=PKG,version=24,pid=run('shell','pidof',PKG),sha256=installed,home=after,home_unchanged=True,data_cleared=False,launch=launch)
(OUT/'installed.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(result,ensure_ascii=False))
