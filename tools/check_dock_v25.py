"""Exercise the real Chromium layout at seven favorite counts and six widths."""
from pathlib import Path
import subprocess,json,base64
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v25/verification'
CLI=Path.home()/'AppData/Roaming/npm/node_modules/agent-browser/bin/agent-browser-win32-x64.exe'
def browser(*args):
 r=subprocess.run([str(CLI),'--session','konoha-v25','--json',*args],capture_output=True,encoding='utf8',check=True)
 result=json.loads(r.stdout);assert result['success'],result
 return result['data']
script='''(()=>{const saved=prefs.pins,rows=[];try{for(let count=0;count<=6;count++){prefs.pins=apps.slice(0,count).map(a=>a.package);renderDock();const dock=$('#dock'),s=getComputedStyle(dock),r=dock.getBoundingClientRect(),cards=[...dock.children],rects=cards.map(c=>c.getBoundingClientRect());const fits=dock.scrollWidth<=dock.clientWidth+1;rows.push({viewport:[innerWidth,innerHeight],favorites:count,cards:cards.length,tracks:s.gridTemplateColumns.split(' ').length,dockWidth:r.width,scrollWidth:dock.scrollWidth,fits,lastRight:rects.at(-1).right,expectedRight:r.right-parseFloat(s.paddingRight),minCard:Math.min(...rects.map(r=>r.width)),sameRow:rects.every(a=>Math.abs(a.top-rects[0].top)<1)});}}finally{prefs.pins=saved;renderDock();}return rows})()'''
rows=[]
for w,h in [(1920,1080),(1280,720),(963,541),(800,600),(640,480),(390,844)]:
 browser('set','viewport',str(w),str(h));rows.extend(browser('eval','-b',base64.b64encode(script.encode()).decode())['result'])
for row in rows:
 assert row['cards']==row['favorites']+1==row['tracks'],row
 assert row['sameRow'],row
 if row['fits']:assert abs(row['lastRight']-row['expectedRight'])<1,row
 if row['viewport'][0]<=900:assert row['minCard']>=107.9,row
browser('set','viewport','963','541')
browser('eval',"prefs.pins=apps.slice(0,5).map(a=>a.package);renderDock();document.querySelector('#dock .app-card').focus();'ready'")
for _ in range(5):browser('press','ArrowRight')
assert browser('eval',"document.activeElement.dataset.action")['result']=='library'
browser('screenshot',str(OUT/'dock-963.png'))
browser('set','viewport','390','844');browser('screenshot',str(OUT/'dock-390.png'),'--full')
(OUT/'dock-layout.json').write_text(json.dumps(dict(cases=rows,passed=len(rows),dpad_archive=True),indent=2),encoding='utf8')
print(f'{len(rows)}/{len(rows)} layouts passed; D-pad reaches archive')
