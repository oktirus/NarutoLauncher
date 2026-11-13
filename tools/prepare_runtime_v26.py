"""Resize delivery copies only; preserve every approved source illustration."""
from pathlib import Path
import json,hashlib
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'assets/runtime/v26';OUT.mkdir(exist_ok=True)
def read(name,global_name):return json.loads((ROOT/'revisions/v26/before/prototype'/name).read_text(encoding='utf8').split(global_name+'=')[1].strip().rstrip(';'))
def resize(url,name,size,quality=92):
 source=ROOT/url.split('?')[0].removeprefix('../');target=OUT/name
 with Image.open(source) as im:
  im=im.resize(size,Image.Resampling.LANCZOS)
  if target.suffix=='.jpg':im=im.convert('RGB');im.save(target,quality=quality,subsampling=0,optimize=True)
  else:im.save(target,format='WEBP',quality=quality,method=6)
 return '../'+target.relative_to(ROOT).as_posix()+'?v='+hashlib.sha256(target.read_bytes()).hexdigest()[:12]
data=dict(scenes={},icons={},effects={},game={})
for s in read('scenes.js','window.KonohaScenes'):
 src='../assets/runtime/'+s['image'];data['scenes'][s['id']]=dict(full=src,small=resize(src,'scene-'+s['id']+'-1600.jpg',(1600,900)),thumb=resize(src,'thumb-'+s['id']+'.webp',(480,270),86))
for kind,url in read('icons.js','window.KonohaIconSources').items():
 data['icons'][kind]=dict(full=url,small=resize(url,'icon-'+kind+'-256.webp',(256,256)),medium=resize(url,'icon-'+kind+'-384.webp',(384,384)))
for name,url in read('effects.js','window.KonohaEffectSources').items():
 if name=='chidori':continue # Entirely procedural, no raster plate.
 data['effects'][name]=dict(full=url,small=resize(url,'effect-'+name+'-384.webp',(384,384)))
for hero,s in read('runner-art.js','window.KonohaRunnerSources').items():
 data['game'][hero]=dict(motion=resize(s['url'],'game-'+hero+'-motion.webp',(1024,512)),slide=resize(s['slide']['url'],'game-'+hero+'-slide.webp',(512,512)))
text="'use strict';\nwindow.KonohaRuntimeAssets="+json.dumps(data,ensure_ascii=False,indent=2)+";\n"
text+="const iconPixels=181*Math.min(devicePixelRatio||1,2);\nfor(const [id,variants] of Object.entries(window.KonohaRuntimeAssets.icons))window.KonohaIconSources[id]=iconPixels<=256?variants.small:iconPixels<=384?variants.medium:variants.full;\n"
(ROOT/'prototype/runtime-assets.js').write_text(text,encoding='utf8')
print('Runtime variants:',len(list(OUT.iterdir())),'files; approved originals preserved')
