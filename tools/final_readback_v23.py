from pathlib import Path
import subprocess,json,hashlib,re
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v23/device';ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555'
def run(*args):return subprocess.check_output([str(ADB),'-s',SERIAL,*args]).decode('utf8',errors='replace').strip()
path=run('shell','pm','path','ua.konoha.tv').removeprefix('package:')
installed=run('shell','sha256sum',path).split()[0]
manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'))
assert installed.upper()==manifest['sha256']
home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
assert home==json.loads((OUT/'before.json').read_text(encoding='utf8'))['home']
focus=next(line.strip() for line in run('shell','dumpsys','window').splitlines() if 'mCurrentFocus=' in line)
assert 'ua.konoha.tv/ua.konoha.tv.MainActivity' in focus,focus
power=[line.strip() for line in run('shell','dumpsys','power').splitlines() if 'mWakefulness=' in line or 'mIsPowered=' in line]
final=dict(time_utc=datetime.now(timezone.utc).isoformat(),serial=SERIAL,model=run('shell','getprop','ro.product.model'),pid=run('shell','pidof','ua.konoha.tv'),package='ua.konoha.tv',installed_apk_sha256=installed,apk_matches=True,default_home=home,default_home_unchanged=True,focus=focus,power=power,updates=['Installed initial live-test APK','Backed up installed initial APK as before-native-fix.apk','Updated native default favorites and real icon fallback','Backed up second APK as before-decode-fix.apk','Updated bounded decode retry and failed-cache eviction'],backups={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in OUT.glob('before-*.apk')})
(OUT/'final-readback.json').write_text(json.dumps(final,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(final,ensure_ascii=False,indent=2))
