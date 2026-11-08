"""Download Naruto reference material, preserving source pages and asset provenance."""
import concurrent.futures, hashlib, html, json, re, urllib.request
from pathlib import Path
from urllib.parse import urljoin, urlparse
ROOT=Path(__file__).resolve().parents[1]
SOURCES={
 'naruto-official':'https://naruto-official.com/en',
 'anime':'https://naruto-official.com/en/anime',
 'rasengan':'https://naruto-official.com/en/special/rasengan',
 'chakra-database':'https://naruto-official.com/en/news/01_1802?nolangsuggestion=1',
 'storm':'https://www.bandainamcoent.com/games/naruto-x-boruto-ultimate-ninja-storm-connections',
}
def fetch(url):
 req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (local Naruto design references)'})
 with urllib.request.urlopen(req,timeout=25) as res:return res.read(),res.headers.get('Content-Type','')
def collect(item):
 name,url=item;folder=ROOT/'research/references'/name;folder.mkdir(parents=True,exist_ok=True)
 row={'name':name,'url':url,'usage':'Local reference only; original copyright retained. Generated runtime assets are separate.','assets':[],'errors':[]}
 try:
  body,_=fetch(url);(folder/'page.html').write_bytes(body);page=body.decode('utf-8',errors='replace')
  matches=re.findall(r'(?:src|content|data-src)=[\"\']([^\"\']+\.(?:jpg|jpeg|png|webp)(?:\?[^\"\']*)?)[\"\']',page,re.I)
  matches=list(dict.fromkeys(urljoin(url,html.unescape(x)) for x in matches))
  matches.sort(key=lambda x:(not bool(re.search('og|main|visual|rasengan|chakra',x,re.I)),len(x)))
  (folder/'candidates.json').write_text(json.dumps(matches,ensure_ascii=False,indent=2),encoding='utf-8')
  for i,asset in enumerate(matches[:6]):
   try:
    data,mime=fetch(asset)
    if not mime.startswith('image/') or len(data)<12000:continue
    dest=folder/(f'{i:02d}-'+Path(urlparse(asset).path).name);dest.write_bytes(data)
    row['assets'].append({'file':dest.relative_to(ROOT).as_posix(),'url':asset,'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)})
   except Exception as ex:row['errors'].append(str(ex))
 except Exception as ex:row['errors'].append(str(ex))
 print(name,len(row['assets']),'references',flush=True);return row
if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:rows=list(pool.map(collect,SOURCES.items()))
 (ROOT/'research/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
