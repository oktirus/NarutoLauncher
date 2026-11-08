"""Keep a v11 snapshot and collect actual episode frames for v12 art."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import quote
from PIL import Image, ImageOps, ImageDraw
from bs4 import BeautifulSoup
import requests,json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'revisions/v12';REF=OUT/'references'
PAGES={
 'jiraiya':'https://naruto-official.com/en/anime/naruto2/list/01_445',
 'itachi':'https://naruto-official.com/en/anime/naruto2/list/01_579',
 'tsunade':'https://naruto-official.com/en/anime/naruto1/list/01_323',
 'minato':'https://naruto-official.com/en/anime/naruto2/list/01_612',
 'sai':'https://naruto-official.com/en/anime/naruto2/list/01_481',
}
REF.mkdir(parents=True,exist_ok=True)
if not (OUT/'before').exists():
 (OUT/'before').mkdir();shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def collect(item):
 hero,page=item;response=requests.get(page,timeout=45);response.raise_for_status()
 soup=BeautifulSoup(response.text,'html.parser')
 data=json.loads(soup.select_one('#__NEXT_DATA__').string)
 paths=data['props']['pageProps']['animeData']['anime']['images']
 urls=['https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(p,safe='/') for p in paths]
 if len(urls)<2:raise RuntimeError(f'{hero}: only {len(urls)} episode frames')
 rows=[]
 for n,url in enumerate(urls[:4],1):
  p=REF/f'{hero}-{n}.jpg';r=requests.get(url,timeout=45);r.raise_for_status();p.write_bytes(r.content)
  im=Image.open(p);im.verify();rows.append({'hero':hero,'page':page,'url':url,'file':p.name,'sha256':digest(p),'kind':'actual official anime episode frame'})
 return rows
with ThreadPoolExecutor(max_workers=5) as pool:rows=[r for group in pool.map(collect,PAGES.items()) for r in group]
old=json.loads((ROOT/'revisions/v11/references/sources.json').read_text(encoding='utf8'))
for r in old:
 if r['hero']=='gaara':
  p=REF/r['file'];shutil.copy2(ROOT/'revisions/v11/references'/r['file'],p);rows.append({**r,'kind':'actual official anime episode frame'})
for hero in [*PAGES,'gaara']:
 group=[r for r in rows if r['hero']==hero];board=Image.new('RGB',(1000,620),'#d8d0bc');draw=ImageDraw.Draw(board)
 for n,r in enumerate(group):
  x,y=n%2*500,n//2*310;im=ImageOps.contain(Image.open(REF/r['file']).convert('RGB'),(490,278));board.paste(im,(x+(500-im.width)//2,y+25));draw.text((x+12,y+6),f'{hero} / actual anime frame {n+1}',fill='#172c34')
 board.save(REF/f'{hero}-board.jpg',quality=94)
(REF/'sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
print(f'Saved {len(rows)} actual episode frames, six boards and exact v11 backup')
