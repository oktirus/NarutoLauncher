from pathlib import Path
from PIL import Image,ImageOps
import hashlib,json,shutil,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v23'
GEN=Path(r'C:\Users\angel\.codex\generated_images\01a1b5d6-c1c9-7f20-9b97-1f0cd10f4a77')
files={'scene-team7-candidate.png':'exec-51cd8152-8888-4689-a2ee-e0eced9fc0e9.png','scene-team7-framing-candidate.png':'exec-ffe19d52-2e85-4f9e-a315-c078c281340b.png','scene-team7.png':'exec-a536cdf1-d258-47d8-8c01-01c8c5ddf795.png'}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
for file,source in files.items():
 dst=ROOT/'assets/generated/v23'/file
 if not dst.exists():shutil.copy2(GEN/source,dst)
src=ROOT/'assets/generated/v23/scene-team7.png';target=ROOT/'assets/runtime/v23/scene-team7.jpg'
ImageOps.fit(Image.open(src).convert('RGB'),(1920,1080),method=Image.Resampling.LANCZOS).save(target,quality=95,subsampling=0)
scenes=json.loads((OUT/'before/prototype/scenes.js').read_text(encoding='utf8').split('window.KonohaScenes=')[1].strip().rstrip(';'))
scene=next(s for s in scenes if s['id']=='night')
scene.update(id='team7',name='Команда №7',caption='Перше спільне фото',mood='Наруто, Сакура, Саске й Какаші',title='Разом<br><em>сильніші.</em>',description='Класичне спільне фото команди №7.',image='v23/scene-team7.jpg?v='+sha(target)[:12],mobilePosition='79% center',cardPosition='77% 34%',particles='leaf',color='#b5cc99',palette=['#b8cc8a','#91b1a0'])
(ROOT/'prototype/scenes.js').write_text("'use strict';\nwindow.KonohaScenes="+json.dumps(scenes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
quotes=json.loads((OUT/'before/prototype/quotes.js').read_text(encoding='utf8').split('window.KonohaQuotes=')[1].strip().rstrip(';'))
quotes['team7']=quotes.pop('night')
(ROOT/'prototype/quotes.js').write_text("'use strict';\nwindow.KonohaQuotes="+json.dumps(quotes,ensure_ascii=False,indent=2)+';\n',encoding='utf8',newline='\r\n')
app=(OUT/'before/prototype/app.js').read_text(encoding='utf8')
needle="if(!scenes.some(s=>s.id===prefs.scene))prefs.scene='dusk';"
assert app.count(needle)==1
app=app.replace(needle,"if(prefs.scene==='night')prefs.scene='team7'; // Replace retired training scene in saved preferences.\n"+needle)
needle="if(prefs.pins===null){prefs.pins=apps.slice(0,6).map(a=>a.package);persist();}"
assert app.count(needle)==1
app=app.replace(needle,"if(prefs.pins===null){const themed=native?apps.filter(a=>a.kind).sort((a,b)=>examples.findIndex(x=>x.kind===a.kind)-examples.findIndex(x=>x.kind===b.kind)):[];prefs.pins=(themed.length?themed:apps).slice(0,6).map(a=>a.package);persist();}")
needle="`<span class=\"app-fallback\">${escapeHTML(app.name.slice(0,2))}</span>`"
assert app.count(needle)==1
app=app.replace(needle,"native&&app.icon?`<img src=\"${escapeHTML(app.icon)}\" alt=\"\" draggable=\"false\">`:"+needle)
needle="if(!decodedScenes.has(scene.id)){const image=new Image();image.src='../assets/runtime/'+scene.image;decodedScenes.set(scene.id,image.decode());}"
assert app.count(needle)==1
app=app.replace(needle,"if(!decodedScenes.has(scene.id)){const load=()=>{const image=new Image();image.src='../assets/runtime/'+scene.image;return image.decode();};const pending=load().catch(()=>load()).catch(error=>{if(decodedScenes.get(scene.id)===pending)decodedScenes.delete(scene.id);throw error;});decodedScenes.set(scene.id,pending);}")
(ROOT/'prototype/app.js').write_text(app,encoding='utf8',newline='\r\n')
html=(OUT/'before/prototype/index.html').read_text(encoding='utf8')
for file in ['scenes.js','quotes.js','app.js']:
 html,n=re.subn(re.escape(file)+r'\?v=[a-zA-Z0-9]+',file+'?v='+sha(ROOT/'prototype'/file)[:12],html);assert n==1
(ROOT/'prototype/index.html').write_text(html,encoding='utf8',newline='\r\n')
(OUT/'asset-manifest.json').write_text(json.dumps(dict(id='team7',source=src.relative_to(ROOT).as_posix(),source_path=str(GEN/files[src.name]),source_sha256=sha(src),file=target.relative_to(ROOT).as_posix(),sha256=sha(target),size=[1920,1080],source_size=list(Image.open(src).size)),indent=2),encoding='utf8')
print('Training removed from registry; Team 7 integrated, saved night selection migrates to team7')
