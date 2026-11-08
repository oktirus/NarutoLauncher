"""Save primary-source technique references separately from runtime assets."""
from pathlib import Path
from urllib.parse import urljoin
import hashlib,json,requests
from bs4 import BeautifulSoup
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v2/references';rows=[]
for name,url in {
 'rasengan':'https://naruto-official.com/en/news/01_1802',
 'chidori':'https://naruto-official.com/en/news/01_1715',
 'hiraishin':'https://naruto-official.com/en/news/01_1709',
}.items():
 r=requests.get(url,timeout=30);r.raise_for_status();page=OUT/(name+'-official.html');page.write_text(r.text,encoding='utf-8')
 images=[]
 for image in BeautifulSoup(r.text,'html.parser').select('img'):
  src=image.get('src','')
  if 'cmsapi-frontend.naruto-official.com' in src:
   dest=OUT/(name+'-official-'+str(len(images)+1)+'.jpg');data=requests.get(src,timeout=30).content;dest.write_bytes(data);images.append({'file':dest.relative_to(ROOT).as_posix(),'url':src,'sha256':hashlib.sha256(data).hexdigest()})
   if len(images)>=2:break
 rows.append({'technique':name,'page':url,'local_page':page.relative_to(ROOT).as_posix(),'images':images})
url='https://tamashiiweb.com/images/item/item_0000014904_HKDgJ4EA_300.pdf';r=requests.get(url,timeout=30);r.raise_for_status();dest=OUT/'rasenshuriken-bandai-reference.pdf';dest.write_bytes(r.content)
rows.append({'technique':'rasenshuriken','page':url,'file':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(r.content).hexdigest(),'role':'Bandai / Tamashii Nations Naruto Uzumaki Sage Mode accessory diagram'})
try:
 import fitz
 doc=fitz.open(dest);doc[0].get_pixmap(matrix=fitz.Matrix(1.5,1.5)).save(str(OUT/'rasenshuriken-bandai-reference.png'))
except ImportError:pass
(ROOT/'revisions/v2/technique-references.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
print('Saved',len(rows),'primary-source reference pages/manuals.')
