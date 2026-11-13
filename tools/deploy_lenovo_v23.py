"""Explicitly authorized install/foreground test on the verified Home LenovoTV."""
from pathlib import Path
from datetime import datetime,timezone
import json,subprocess,hashlib
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v23/device'
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555';PACKAGE='ua.konoha.tv'
journal=[]
def run(*args):
 result=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True)
 text=result.stdout.decode('utf8',errors='replace').strip();error=result.stderr.decode('utf8',errors='replace').strip()
 journal.append(dict(time_utc=datetime.now(timezone.utc).isoformat(),command=['adb','-s',SERIAL,*args],exit_code=result.returncode,output=text,error=error))
 (OUT/'deployment-journal.json').write_text(json.dumps(journal,ensure_ascii=False,indent=2),encoding='utf8')
 if result.returncode:raise RuntimeError(error or text)
 return text
assert run('get-state')=='device'
model=run('shell','getprop','ro.product.model');device=run('shell','getprop','ro.product.device')
assert model=='LNVNB161216' and device=='x86_64_tv_go',(model,device)
before=dict(serial=SERIAL,model=model,device=device,android=run('shell','getprop','ro.build.version.release'),home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME'),display=run('shell','wm','size'),existing=run('shell','pm','list','packages',PACKAGE))
assert not before['existing'],'Package exists: back up its APK/data before updating'
(OUT/'before.json').write_text(json.dumps(before,ensure_ascii=False,indent=2),encoding='utf8')
apk=ROOT/'releases/Konoha-1.0.0-v23-debug.apk';manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'))
assert hashlib.sha256(apk.read_bytes()).hexdigest().upper()==manifest['sha256']
assert 'Success' in run('install','-r',str(apk))
launch=run('shell','am','start','-W','-n',PACKAGE+'/.MainActivity');assert 'Status: ok' in launch,launch
after=dict(package_path=run('shell','pm','path',PACKAGE),pid=run('shell','pidof',PACKAGE),home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME'),launch=launch)
assert after['home']==before['home'],'Unexpected default HOME change'
(OUT/'after-install.json').write_text(json.dumps(after,ensure_ascii=False,indent=2),encoding='utf8')
screen=subprocess.run([str(ADB),'-s',SERIAL,'exec-out','screencap','-p'],capture_output=True,check=True).stdout
(OUT/'first-launch.png').write_bytes(screen)
print('Installed and started ua.konoha.tv on verified Lenovo; default HOME preserved; PID '+after['pid'])
