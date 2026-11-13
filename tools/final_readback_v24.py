from pathlib import Path
from datetime import datetime,timezone
import json,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v24/device';ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555'
def run(*args):return subprocess.check_output([str(ADB),'-s',SERIAL,*args],timeout=30).decode('utf8',errors='replace').strip()
assert run('shell','getprop','ro.product.model')=='LNVNB161216'
path=run('shell','pm','path','ua.konoha.tv').removeprefix('package:');hash=run('shell','sha256sum',path).split()[0]
manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));assert hash.upper()==manifest['sha256'] and manifest['versionCode']==24
home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME');assert home.splitlines()[-1]=='ua.konoha.tv/.MainActivity'
focus=[l.strip() for l in run('shell','dumpsys','window').splitlines() if 'mCurrentFocus=' in l];assert any('ua.konoha.tv/ua.konoha.tv.MainActivity' in l for l in focus)
power=run('shell','dumpsys','power');standby=json.loads(run('shell','cat','/data/local/tmp/lenovo-standby/status.json'));assert not standby['displayOff'] and 'mWakefulness=Awake' in power
result=dict(time_utc=datetime.now(timezone.utc).isoformat(),serial=SERIAL,version=24,installed_apk_sha256=hash,home=home,focus=focus,pid=run('shell','pidof','ua.konoha.tv'),power=[l.strip() for l in power.splitlines() if 'mWakefulness=' in l or 'mIsPowered=' in l],displayOff=standby['displayOff'])
(OUT/'final.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(result,ensure_ascii=False))
