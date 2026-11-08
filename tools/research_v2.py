"""Traceable reference and short public audio preview collection for revision 2."""
from pathlib import Path
from urllib.parse import urljoin
import hashlib,json,re,requests
from bs4 import BeautifulSoup
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'revisions/v2'; REF=OUT/'references'; AUDIO=OUT/'audio'
for p in (REF,AUDIO):p.mkdir(parents=True,exist_ok=True)
session=requests.Session();session.headers['User-Agent']='Mozilla/5.0'
records=[]
def fetch(url,path):
 r=session.get(url,timeout=45);r.raise_for_status();path.write_bytes(r.content)
 records.append({'url':url,'file':str(path.relative_to(ROOT)).replace('\\','/'),'sha256':hashlib.sha256(r.content).hexdigest()})
 return r
images={
 'anime-four-faces.jpg':'https://m.media-amazon.com/images/M/MV5BMzU0ODE1MDcwNF5BMl5BanBnXkFtZTgwODA3NzI0MjE%40._V1_FMjpg_UX1000_.jpg',
 'anime-village.jpg':'https://www.ligagame.tv/images/ligagame_visahat/resized/Konohagakure-naruto_3da0f.jpeg',
 'anime-front.jpg':'https://statics.viralizalo.com/virs/2016/02/VIR_130176_11015_naruto.jpg',
}
for name,url in images.items():
 try:fetch(url,REF/name)
 except Exception as e:print(name,str(e))
park='https://en.awajiisland.pasonagroup.co.jp/hokage-rock-discovering-narutos-legacy-on-awaji-island/'
try:
 html=fetch(park,REF/'official-park.html').text;soup=BeautifulSoup(html,'html.parser')
 urls=[]
 for image in soup.select('img'):
  src=image.get('data-src') or image.get('src','')
  if any(s in (src+' '+image.get('alt','')).lower() for s in ('hokage','naruto','rock')) and src.startswith('http'):urls.append(src)
 for i,url in enumerate(dict.fromkeys(urls)):
  if i>=4:break
  fetch(url,REF/f'official-park-{i+1}.jpg')
except Exception as e:print('park',str(e))
sounds={
 'rasengan':'https://www.myinstants.com/en/instant/naruto-rasengan-shippuden-23365/',
 'chidori':'https://www.myinstants.com/en/instant/chidori-sound-4995/',
 'sharingan':'https://www.myinstants.com/en/instant/sharingan-99233/',
}
for key,url in sounds.items():
 try:
  html=fetch(url,AUDIO/(key+'.html')).text
  paths=re.findall(r'[\"\'](/media/sounds/[^\"\']+\.mp3)',html)
  if paths:fetch(urljoin(url,paths[0]),AUDIO/(key+'.mp3'))
 except Exception as e:print(key,str(e))
try:
 data=session.get('https://itunes.apple.com/lookup',params={'id':'1584791969','entity':'song','country':'us'},timeout=30).json()
 (AUDIO/'apple-album.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
 print('ALBUM',[(r.get('trackId'),r.get('trackName'),r.get('collectionId')) for r in data.get('results',[])])
except Exception as e:print('apple',str(e))
(OUT/'download-manifest.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
print('Downloaded',len(records),'references / source pages / audio files')
