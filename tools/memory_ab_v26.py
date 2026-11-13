"""Matched Lenovo v25/v26 exercise; transient UI only, no persisted preference changes."""
from pathlib import Path
from datetime import datetime,timezone
import subprocess,json,re,time,statistics,base64,sys
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v26/device';ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';CLI=Path.home()/'AppData/Roaming/npm/node_modules/agent-browser/bin/agent-browser-win32-x64.exe';SERIAL='192.168.100.26:5555';phase=sys.argv[1];session='konoha-ab26-'+phase
def adb(*args):return subprocess.check_output([str(ADB),'-s',SERIAL,*args],timeout=90).decode('utf8',errors='replace').strip()
def browser(*args):
 x=subprocess.run([str(CLI),'--session',session,'--json',*args],capture_output=True,encoding='utf8',timeout=45,check=True);r=json.loads(x.stdout);assert r['success'],r;return r['data']
def js(s):return browser('eval','-b',base64.b64encode(s.encode()).decode())['result']
def sample(label):
 procs=[]
 for block in re.split(r'(?m)(?=^\s+\*APP\* UID )',adb('shell','dumpsys','activity','processes'))[1:]:
  p=re.search(r'ProcessRecord\{\S+ (\d+):([^\s}]+)',block);q=re.search(r'packageList=\{([^}]+)\}',block)
  if p and q and 'ua.konoha.tv' in [v.strip() for v in q[1].split(',')]:procs.append((p[1],p[2]))
 assert len(procs)>=2,procs
 values=[]
 for i in range(3):
  rows=[]
  for pid,name in procs:
   raw=adb('shell','dumpsys','meminfo',pid);(OUT/f'{phase}-{label}-{i}-{pid}.txt').write_text(raw,encoding='utf8');m=re.search(r'TOTAL PSS:\s+(\d+)\s+TOTAL RSS:\s+(\d+)\s+TOTAL SWAP PSS:\s+(\d+)',raw);assert m;rows.append(dict(pid=pid,name=name,pss_kib=int(m[1]),rss_kib=int(m[2]),swap_kib=int(m[3])))
  values.append(dict(processes=rows,pss_mib=sum(r['pss_kib'] for r in rows)/1024));time.sleep(1)
 result=dict(label=label,pss_mib=statistics.mean(v['pss_mib'] for v in values),samples=values,runtime=js('({modal,canvases:[...document.querySelectorAll("canvas")].map(c=>({id:c.id,width:c.width,height:c.height})),audio:audioEngine.snapshot(),sceneCache:decodedScenes.size})'))
 data['states'].append(result);save();print(phase,label,round(result['pss_mib'],2),'MiB',flush=True)
def save():(OUT/f'memory-{phase}.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf8')
assert adb('shell','getprop','ro.product.model')=='LNVNB161216'
assert adb('shell','getprop','ro.product.device')=='x86_64_tv_go'
path=adb('shell','pm','path','ua.konoha.tv').removeprefix('package:');sha=adb('shell','sha256sum',path).split()[0]
expected=json.loads((ROOT/('revisions/v26/before/releases/build-manifest.json' if phase=='v25' else 'releases/build-manifest.json')).read_text(encoding='utf-8-sig'));assert sha.upper()==expected['sha256']
status=json.loads(adb('shell','cat','/data/local/tmp/lenovo-standby/status.json'))
if status['displayOff']:adb('shell','sh','-c','"echo wake > /data/local/tmp/lenovo-standby/command"')
adb('shell','am','force-stop','ua.konoha.tv');adb('shell','am','start','-W','-n','ua.konoha.tv/.MainActivity');print(phase,'warming 45s',flush=True);time.sleep(45)
pid=adb('shell','pidof','ua.konoha.tv');port=adb('forward','tcp:0','localabstract:webview_devtools_remote_'+pid)
data=dict(phase=phase,sha256=sha,time_utc=datetime.now(timezone.utc).isoformat(),warmup_seconds=45,states=[])
try:
 browser('--cdp',port,'get','url');data['prefs']=js('window.ab26Original={...prefs};({prefs:{...prefs},persisted:localStorage.getItem("konoha.preferences.v1")})');save();sample('home')
 for panel in ['scenes','techniques','game']:
  js(f'openPanel("{panel}");true');time.sleep(5);sample(panel+'-open');js('closePanel();true');time.sleep(10);sample(panel+'-closed')
 js('prefs.audio=true;prefs.ambience=true;prefs.volume=0;prefs.musicVolume=0;unlockAudio().then(()=>true)')
 for track in ['1584791975','1584792236','1584792240']:
  js(f'prefs.music="{track}";audioEngine.configure({{audio:true,ambience:true,paused:false,volume:0,musicVolume:0,music:prefs.music}}).then(()=>true)');time.sleep(2)
 sample('three-tracks')
finally:
 try:
  data['restored']=js('Object.assign(prefs,window.ab26Original);ambience();({prefs:{...prefs},persisted:localStorage.getItem("konoha.preferences.v1"),modal})');assert data['restored']['prefs']==data['prefs']['prefs'] and data['restored']['persisted']==data['prefs']['persisted'];save()
 finally:
  browser('close');adb('forward','--remove','tcp:'+port)
print(phase,'done; settings restored',flush=True)
