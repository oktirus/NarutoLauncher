"""Live WebView static chooser and Android pause/resume proof, preserving prefs."""
from pathlib import Path
import subprocess,json,base64,time
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v26/device';ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';CLI=Path.home()/'AppData/Roaming/npm/node_modules/agent-browser/bin/agent-browser-win32-x64.exe';SERIAL='192.168.100.26:5555';SESSION='konoha-native26'
def adb(*args):return subprocess.check_output([str(ADB),'-s',SERIAL,*args],timeout=40).decode('utf8',errors='replace').strip()
def browser(*args):
 r=subprocess.run([str(CLI),'--session',SESSION,'--json',*args],capture_output=True,encoding='utf8',timeout=45,check=True);x=json.loads(r.stdout);assert x['success'],x;return x['data']
def js(s):return browser('eval','-b',base64.b64encode(s.encode()).decode())['result']
def png(name):
 x=subprocess.check_output([str(ADB),'-s',SERIAL,'exec-out','screencap','-p'],timeout=30);(OUT/name).write_bytes(x[x.index(b'\x89PNG\r\n\x1a\n'):])
assert adb('shell','getprop','ro.product.model')=='LNVNB161216'
pid=adb('shell','pidof','ua.konoha.tv');port=adb('forward','tcp:0','localabstract:webview_devtools_remote_'+pid);result={}
try:
 browser('--cdp',port,'get','url');result['before']=js('({prefs:{...prefs},storage:localStorage.getItem("konoha.preferences.v1")})')
 result['static']=js('''(async()=>{openPanel('techniques');await new Promise(r=>setTimeout(r,800));const a=[...document.querySelectorAll('[data-preview]')].map(c=>c.toDataURL());await new Promise(r=>setTimeout(r,500));return{count:a.length,identical:[...document.querySelectorAll('[data-preview]')].every((c,i)=>c.toDataURL()===a[i]),frame,art:techniques.artStatus()};})()''');assert result['static']['count']==10 and result['static']['identical'] and result['static']['frame']==0;png('techniques-live.png')
 js('closePanel();true');result['closed']=js('({children:document.querySelector("#panel-content").childElementCount,art:techniques.artStatus()})');assert result['closed']['children']==0 and len(result['closed']['art'])==2
 result['archiveFocus']=js('(async()=>{openPanel("library");await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));document.querySelector("#library-grid .app-card").focus();return {selected:document.activeElement.matches(".app-card"),package:document.activeElement.dataset.package};})()');assert result['archiveFocus']['selected']
 assert 'Status: ok' in adb('shell','am','start','-W','-a','android.settings.SETTINGS');time.sleep(2)
 result['background']=js('({paused,hidden:document.hidden,canvases:[...document.querySelectorAll("canvas")].map(c=>[c.width,c.height]),audio:audioEngine.context?.state,cache:audioEngine.cache.size})');assert result['background']['paused'] and all(w==0 and h==0 for w,h in result['background']['canvases']);assert result['background']['cache']==0
 adb('shell','input','keyevent','3');time.sleep(2)
 result['after']=js('({prefs:{...prefs},storage:localStorage.getItem("konoha.preferences.v1"),paused,hidden:document.hidden,canvas:[canvas.width,canvas.height],sceneCache:decodedScenes.size,modal,frame,focusCenter,focusTarget:focusTarget?.dataset.package,active:document.activeElement.outerHTML.slice(0,300)})');assert result['after']['prefs']==result['before']['prefs'] and result['after']['storage']==result['before']['storage'];assert not result['after']['paused'] and not result['after']['hidden'] and result['after']['canvas'][0]>0;assert result['after']['modal']=='library' and result['after']['frame']>0;js('closePanel();true');time.sleep(.3);png('home-live.png')
 (OUT/'smoke.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8');print('Native static chooser, cleanup, Settings/Home resume and preference preservation PASS')
finally:
 (OUT/'smoke-last-attempt.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
 try:js('if(modal)closePanel();true');adb('shell','input','keyevent','3')
 finally:browser('close');adb('forward','--remove','tcp:'+port)
