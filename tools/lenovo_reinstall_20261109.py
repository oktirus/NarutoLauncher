"""Scoped audit, encrypted backup and authorized reinstall of ua.konoha.tv."""
from pathlib import Path
from datetime import datetime,timezone
import subprocess,json,hashlib,importlib.util,sys,tarfile,io,time
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'verification/lenovo-reinstall-20261109'
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555';PKG='ua.konoha.tv'
OUT.mkdir(exist_ok=True);JOURNAL=OUT/'actions.json'
journal=json.loads(JOURNAL.read_text(encoding='utf8')) if JOURNAL.exists() else []
def record(args,result):
 journal.append(dict(time_utc=datetime.now(timezone.utc).isoformat(),command=['adb','-s',SERIAL,*args],exit_code=result.returncode,output=result.stdout.decode('utf8',errors='replace').strip(),error=result.stderr.decode('utf8',errors='replace').strip()))
 JOURNAL.write_text(json.dumps(journal,ensure_ascii=False,indent=2),encoding='utf8')
def run(*args,optional=False):
 result=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True,timeout=120);record(args,result)
 if result.returncode and not optional:raise RuntimeError('ADB failed: '+str(args))
 return result.stdout.decode('utf8',errors='replace').strip()
def blob(*args):
 result=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True,timeout=120)
 if result.returncode:raise RuntimeError('Binary ADB operation failed; private output omitted')
 return result.stdout
def save(name,value):(OUT/name).write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf8')
def sha(data):return hashlib.sha256(data).hexdigest()
assert run('shell','getprop','ro.product.model')=='LNVNB161216'
assert run('shell','getprop','ro.product.device')=='x86_64_tv_go'
mode=sys.argv[1] if len(sys.argv)>1 else 'audit'
if mode=='audit':
 data=dict(model='LNVNB161216',serial=SERIAL,boot_id=run('shell','cat','/proc/sys/kernel/random/boot_id'),uptime=run('shell','cat','/proc/uptime'),boot_reason=run('shell','getprop','sys.boot.reason'),package_path=run('shell','pm','path',PKG),home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME'),standby=json.loads(run('shell','cat','/data/local/tmp/lenovo-standby/status.json')),standby_log=run('shell','cat','/data/local/tmp/lenovo-standby/daemon.log'),lid_behavior=run('shell','settings','get','global','lid_behavior'),stay_on=run('shell','settings','get','global','stay_on_while_plugged_in'),package_exit_history=run('shell','dumpsys','activity','exit-info',PKG),pstore=run('shell','ls','-l','/sys/fs/pstore'))
 power=run('shell','dumpsys','power');data['power']=[l.strip() for l in power.splitlines() if any(k in l for k in ['mWakefulness=','mIsPowered=','mStayOn=','mLastSleepTime=','mLastWakeTime='])]
 window=run('shell','dumpsys','window');data['window']=[l.strip() for l in window.splitlines() if any(k in l for k in ['mCurrentFocus=','mLidState=','mAwake='])]
 save('before.json',data)
 print(json.dumps({k:data[k] for k in ['home','standby','standby_log','power','package_exit_history']},ensure_ascii=False,indent=2))
elif mode=='backup':
 dst=OUT/'backup';dst.mkdir(exist_ok=True)
 assert not (dst/'app-data.dpapi').exists(),'Do not replace the existing backup'
 path=run('shell','pm','path',PKG).removeprefix('package:')
 apk=blob('exec-out','cat',path);(dst/'installed.apk').write_bytes(apk)
 # Stop only this app so the WebView database backup is consistent.
 run('shell','am','force-stop',PKG)
 raw=blob('exec-out','run-as',PKG,'tar','-czf','-','--exclude=cache','--exclude=code_cache','.')
 with tarfile.open(fileobj=io.BytesIO(raw),mode='r:gz') as archive:
  files=[m.name for m in archive.getmembers() if m.isfile()]
 modulePath=ROOT.parent/'Lenovo_IdeaPad_120S-11IAP_LineageOS_TV/tools/recovery/recovery.py'
 spec=importlib.util.spec_from_file_location('lenovo_recovery',modulePath);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 encrypted=module.protected(raw);assert module.protected(encrypted,decrypt=True)==raw
 (dst/'app-data.dpapi').write_bytes(encrypted)
 save('backup/manifest.json',dict(package=PKG,apk_sha256=sha(apk),data_plain_sha256=sha(raw),encrypted_sha256=sha(encrypted),data_files=len(files),protection='Windows DPAPI CurrentUser; original PC/account required',raw_data_written_to_disk=False))
 print('APK and DPAPI-encrypted app data saved; '+str(len(files))+' files verified')
elif mode=='wake':
 before=json.loads(run('shell','cat','/data/local/tmp/lenovo-standby/status.json'))
 run('shell','input','keyevent','224');time.sleep(2)
 normal=json.loads(run('shell','cat','/data/local/tmp/lenovo-standby/status.json'))
 run('shell','echo wake > /data/local/tmp/lenovo-standby/command');time.sleep(3)
 control=json.loads(run('shell','cat','/data/local/tmp/lenovo-standby/status.json'))
 save('wake-test.json',dict(before=before,after_android_wakeup=normal,after_standby_control=control))
 assert not control['displayOff'],'Display did not wake'
 print('DisplayOff: before='+str(before['displayOff'])+', Android wake='+str(normal['displayOff'])+', helper wake='+str(control['displayOff']))
elif mode=='reinstall':
 assert (OUT/'backup/manifest.json').exists(),'Backup required'
 manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'));apk=ROOT/'releases'/manifest['file']
 assert sha(apk.read_bytes()).upper()==manifest['sha256']
 assert 'Success' in run('install','-r',str(apk))
 output=run('shell','am','start','-W','-n',PKG+'/.MainActivity');assert 'Status: ok' in output
 path=run('shell','pm','path',PKG).removeprefix('package:');installed=run('shell','sha256sum',path).split()[0]
 assert installed.upper()==manifest['sha256']
 save('reinstall.json',dict(package=PKG,sha256=installed,installed_apk_matches=True,launch=output,pid=run('shell','pidof',PKG),data_cleared=False))
 print('Reinstalled and launched; installed APK SHA256 matches; app data preserved')
elif mode=='final':
 standby=json.loads(run('shell','cat','/data/local/tmp/lenovo-standby/status.json'))
 power=run('shell','dumpsys','power');window=run('shell','dumpsys','window')
 path=run('shell','pm','path',PKG).removeprefix('package:');installed=run('shell','sha256sum',path).split()[0]
 manifest=json.loads((ROOT/'releases/build-manifest.json').read_text(encoding='utf-8-sig'))
 home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
 focus=next(l.strip() for l in window.splitlines() if 'mCurrentFocus=' in l)
 data=dict(time_utc=datetime.now(timezone.utc).isoformat(),serial=SERIAL,pid=run('shell','pidof',PKG),standby=standby,home=home,focus=focus,power=[l.strip() for l in power.splitlines() if any(k in l for k in ['mWakefulness=','mIsPowered=','mStayOn='])],installed_apk_sha256=installed,apk_matches=installed.upper()==manifest['sha256'])
 assert data['apk_matches'] and not standby['displayOff'] and 'ua.konoha.tv/ua.konoha.tv.MainActivity' in focus
 assert 'mWakefulness=Awake' in power
 save('final.json',data);print(json.dumps(data,ensure_ascii=False,indent=2))
else:raise ValueError(mode)
