"""Apply the user's explicit choice of default HOME on the verified Lenovo."""
from pathlib import Path
from datetime import datetime,timezone
import json,subprocess,time
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'verification/lenovo-reinstall-20261109'
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555'
journal=[]
def run(*args):
 r=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True,timeout=30)
 journal.append(dict(time_utc=datetime.now(timezone.utc).isoformat(),command=['adb','-s',SERIAL,*args],exit_code=r.returncode,output=r.stdout.decode('utf8',errors='replace').strip(),error=r.stderr.decode('utf8',errors='replace').strip()))
 (OUT/'home-actions.json').write_text(json.dumps(journal,ensure_ascii=False,indent=2),encoding='utf8')
 if r.returncode:raise RuntimeError('ADB failed: '+str(args))
 return journal[-1]['output']
def home():return run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
assert run('shell','getprop','ro.product.model')=='LNVNB161216'
assert run('shell','getprop','ro.product.device')=='x86_64_tv_go'
before=home();backup=OUT/'home-before.json'
if not backup.exists():backup.write_text(json.dumps(dict(home=before,rollback='adb -s '+SERIAL+' shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity'),indent=2),encoding='utf8')
result=run('shell','cmd','package','set-home-activity','--user','0','ua.konoha.tv/.MainActivity')
after=home();assert after.splitlines()[-1]=='ua.konoha.tv/.MainActivity',after
run('shell','am','start','-W','-a','android.settings.SETTINGS')
run('shell','input','keyevent','3');time.sleep(2)
window=run('shell','dumpsys','window');focus=next(l.strip() for l in window.splitlines() if 'mCurrentFocus=' in l)
assert 'ua.konoha.tv/ua.konoha.tv.MainActivity' in focus,focus
readback=dict(before=before,after=after,set_home_result=result,home_key_foreground=focus,home_key_test_passed=True,reboot_tested=False)
(OUT/'home-after.json').write_text(json.dumps(readback,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(readback,ensure_ascii=False,indent=2))
