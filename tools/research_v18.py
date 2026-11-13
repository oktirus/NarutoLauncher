from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
import hashlib,json,shutil,urllib.request,re
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v18';OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
 shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
 (OUT/'before-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for group in ['prototype','assets'] for p in (ROOT/group).rglob('*') if p.is_file()},indent=2),encoding='utf8')
for folder in ['references','verification']:(OUT/folder).mkdir(exist_ok=True)
for folder in ['assets/generated/v18','assets/runtime/v18']:(ROOT/folder).mkdir(exist_ok=True)
rows=[]
def get(name,url,page,role):
 p=OUT/'references'/name;p.write_bytes(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'}),timeout=35).read());Image.open(p).verify();rows.append(dict(file=p.relative_to(ROOT).as_posix(),image=url,page=page,role=role,sha256=hashlib.sha256(p.read_bytes()).hexdigest()));print(name,flush=True)
items=[('training-team.jpg','https://pbs.twimg.com/media/F9UtvP-WYAA3Uo5.jpg','https://x.com/lonely_heheh/status/1717333932897657308','Actual anime coordinated formation, inspect hand attachments; not generated art'),('training-waterfall.jpg','https://image.animationdigitalnetwork.com/license/naruto/tv2/web/eps73_1920x1080.jpg','https://animationdigitalnetwork.com/video/265-naruto-shippuden/3875-episode-73','Licensed anime episode still; wind training setting'),('training-blades.jpg','https://cdn.alfabetajuega.com/alfabetajuega/2023/02/image-2023-02-12t182835.695.jpg','https://alfabetajuega.com/naruto/esta-es-la-explicacion-al-rasenshuriken-de-naruto','Actual anime jutsu shape; extra decoy figures not formation count'),('ramen-eating.jpg','https://media.thepopverse.com/media/naruto-eating-ramen-zenhw0tqcg4enec4ea3j54uzam.jpg','https://www.thepopverse.com/tv-naruto-shippuden-the-official-cookbook-announced-viz-media-fall-2025','Anime eating pose, inspect exact age/clothes'),('ramen-counter.jpg','https://a.storyblok.com/f/178900/720x540/166436f6ab/0f91bbaa0e2921611e339cf5cebd08ea1642644441_main.png/m/filters%3Aquality%2895%29format%28webp%29','https://www.crunchyroll.com/news/features/2022/1/20/feature-which-word-would-these-7-anime-characters-play-first-in-wordle','Licensed anime still: counter, food, eating pose; Part I outfit is not Shippuden')]
for args in items:get(*args)
for tag,slug in [('ramen','01_450'),('minato','01_612')]:
 page='https://naruto-official.com/en/anime/naruto2/list/'+slug;markup=urllib.request.urlopen(page,timeout=35).read().decode();paths=list(dict.fromkeys(re.findall(r'"(/naruto/import/images/[^"<>]+\.(?:jpg|png))"',markup)))[:4]
 for i,path in enumerate(paths):get(f'{tag}-official-{i+1}.jpg','https://cmsapi-frontend.naruto-official.com/site/api/naruto/Image/get?path='+quote(path,safe=''),page,'Official episode frame; inspect before use')
for source,name,role in [(ROOT/'revisions/v16/references/minato-board.jpg','minato-v16-board.jpg','Existing actual anime design board'),(ROOT/'revisions/v17/references/cel-density-approved.jpg','cel-density-approved.jpg','Approved generated cel density only, not anime source')]:
 p=OUT/'references'/name;shutil.copy2(source,p);rows.append(dict(file=p.relative_to(ROOT).as_posix(),copied_from=source.relative_to(ROOT).as_posix(),role=role,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
for tag in ['training','ramen','minato']:
 group=[r for r in rows if Path(r['file']).name.startswith(tag) and 'board' not in r['file']];cols=min(4,len(group));board=Image.new('RGB',(500*cols,320*((len(group)+cols-1)//cols)),'#142d38');d=ImageDraw.Draw(board)
 for i,r in enumerate(group):
  im=ImageOps.contain(Image.open(ROOT/r['file']).convert('RGB'),(490,280));x=i%cols*500;y=i//cols*320;board.paste(im,(x+(500-im.width)//2,y+(280-im.height)//2));d.text((x+8,y+294),Path(r['file']).name,fill='white')
 board.save(OUT/f'references/{tag}-board.jpg',quality=95)
(OUT/'references/sources.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8');print('v17 frozen; v18 references saved')
