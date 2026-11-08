"""Collect source pages and complete downloadable Naruto cues for revision 4."""
import concurrent.futures, hashlib, json, sys, subprocess, re, html
from pathlib import Path
from urllib.parse import urljoin
import requests
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]; OUT=ROOT/'revisions/v4'
HEADERS={'User-Agent':'Mozilla/5.0'}
def get(url):
    response=requests.get(url,headers=HEADERS,timeout=40);response.raise_for_status();return response
def search(term):
    try:page=get('https://www.myinstants.com/en/search/?name='+term)
    except requests.HTTPError as error:return {'term':term,'items':[],'status':error.response.status_code}
    soup=BeautifulSoup(page.text,'html.parser')
    return {'term':term,'items':[{'name':a.get_text(strip=True),'page':urljoin(page.url,a['href'])} for a in soup.select('a.instant-link')][:16]}
def inspect(url):
    response=get(url);soup=BeautifulSoup(response.text,'html.parser')
    return {'page':url,'final':response.url,'title':soup.title.get_text(),'download':[urljoin(response.url,a['href']) for a in soup.select('a[href]') if '.mp3' in a['href']],'buttons':[b.get('onclick') for b in soup.select('button[onclick]')][-4:]}
def portrait():
    rows=json.loads((OUT/'references/gaara-official-images.json').read_text(encoding='utf-8'))
    urls=[r['src'] for r in rows if r['src'].startswith('https://cmsapi')]
    selected=[urls[0],urls[17],urls[27]]
    manifest=[]
    for i,url in enumerate(selected):
        data=get(url).content;dest=OUT/'references'/f'gaara-official-{i+1}.png';dest.write_bytes(data)
        manifest.append({'page':'https://naruto-official.com/en/news/01_1688','url':url,'file':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(data).hexdigest(),'kind':'official manga character/story reference'})
    (OUT/'references/gaara-manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print('Downloaded',len(manifest),'official Gaara references')
def download_candidates():
    urls={
      'rasengan-a':'naruto-rasengan-shippuden-23365','rasengan-b':'naruto-rasengan-by-maicon','rasengan-c':'naruto-rasengan-shippuden-4671',
      'amaterasu-a':'amaterasu','amaterasu-b':'sasukes-amaterasu-from-boruto-77503','rasenshuriken':'rasenshuriken-11185',
      'summoning':'kuchiosenojutsu-90227','fireball':'katon-goukakyuu-no-jutsu','fireball-b':'katon-gaukakyu-no-13451',
      'kurama':'kyubbi-roar-33064','hiraishin':'teleport-anime-36324','hiraishin-b':'minato-namikaze-1556','gamabunta':'summoning-88828',
    }
    def run(item):
        tag,slug=item
        try:
            row=inspect('https://www.myinstants.com/en/instant/'+slug+'/');url=row['download'][0]
            data=get(url).content;dest=OUT/'audio'/(tag+'-source.mp3');dest.write_bytes(data)
            probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','json',str(dest)],text=True))
            row.update({'id':tag,'url':url,'source':dest.relative_to(ROOT).as_posix(),'duration':float(probe['format']['duration']),'sha256':hashlib.sha256(data).hexdigest()})
            return row
        except Exception as error:return {'id':tag,'error':str(error)}
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:rows=list(pool.map(run,urls.items()))
    (OUT/'audio/candidates.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps([{k:r.get(k) for k in ['id','title','url','duration','error']} for r in rows],ensure_ascii=False))
def precise_sources():
    rows=[]
    page='https://www.voicy.network/sounds/wuvtwiCN20ufGTqqXsrXeQ-amaterasu-itachi'
    text=(OUT/'audio/voicy-page.html').read_text(encoding='utf-8')
    for match in list(re.finditer(r'"source":"([^\"]+\.mp3)"',text))[:1]:
        url='https://files.voicy.network/public'+match.group(1)
        try:
            data=get(url).content;dest=OUT/'audio/amaterasu-itachi-source.mp3';dest.write_bytes(data)
            subprocess.run(['ffprobe','-v','error',str(dest)],check=True)
            rows.append({'id':'amaterasu-itachi','page':page,'url':url,'source':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(data).hexdigest()});break
        except Exception as error:print(type(error).__name__)
    text=(OUT/'audio/hiraishin-zedge.html').read_text(encoding='utf-8');soup=BeautifulSoup(text,'html.parser')
    url=next(m['content'] for m in soup.select('meta[content]') if 'hiraishin_sound_in.mp3' in m['content'])
    data=get(url).content;dest=OUT/'audio/hiraishin-in-source.mp3';dest.write_bytes(data)
    subprocess.run(['ffprobe','-v','error',str(dest)],check=True)
    rows.append({'id':'hiraishin-in','page':'https://www.zedge.net/ringtones/57ab9f40-dee8-3e9a-9926-f37277a20bc1','url':url,'source':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(data).hexdigest()})
    for row in rows:row['duration']=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(ROOT/row['source'])],text=True))
    (OUT/'audio/precise-sources.json').write_text(json.dumps(rows,indent=2),encoding='utf-8');print(json.dumps([{k:r[k] for k in ['id','duration']} for r in rows]))
def voice_sources():
    candidates={'rasenshuriken-voice':'https://www.zedge.net/notification-sounds/01e15488-dbb8-3a99-b3b0-a522b699b2de',
                'rasenshuriken-voice-b':'https://www.zedge.net/notification-sounds/b46a2c55-1e19-4da7-9855-763d5116300c',
                'sharingan-voice':'https://www.myinstants.com/en/instant/itachi-sharingan-naruto-43808/',
                'chidori-voice':'https://www.myinstants.com/en/instant/chidorii-43852/',
                'hiraishin-out':'https://www.zedge.net/notification-sounds/5c442c52-9389-3753-8a4e-b667e00b35dc'}
    rows=[]
    for tag,page in candidates.items():
        response=get(page);soup=BeautifulSoup(response.text,'html.parser')
        if 'zedge' in page:url=next(m['content'] for m in soup.select('meta[content]') if m.get('property')=='og:audio')
        else:url=inspect(page)['download'][0]
        data=get(url).content;dest=OUT/'audio'/(tag+'-source.mp3');dest.write_bytes(data)
        duration=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(dest)],text=True))
        rows.append({'id':tag,'page':page,'url':url,'source':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(data).hexdigest(),'duration':duration})
    (OUT/'audio/voice-sources.json').write_text(json.dumps(rows,indent=2),encoding='utf-8');print(json.dumps([{k:r[k] for k in ['id','duration']} for r in rows]))
if __name__=='__main__' and '--inspect' in sys.argv:
    urls=['https://www.myinstants.com/en/instant/amaterasu/','https://www.myinstants.com/en/instant/rasenshuriken-11185/','https://www.myinstants.com/en/instant/naruto-rasengan-shippuden-23365/']
    print(json.dumps(list(concurrent.futures.ThreadPoolExecutor(max_workers=3).map(inspect,urls)),ensure_ascii=False))
elif __name__=='__main__' and '--portrait' in sys.argv:portrait()
elif __name__=='__main__' and '--download' in sys.argv:download_candidates()
elif __name__=='__main__' and '--precise' in sys.argv:precise_sources()
elif __name__=='__main__' and '--voices' in sys.argv:voice_sources()
elif __name__=='__main__' and '--extra' in sys.argv:
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:rows=list(pool.map(search,['kyuubi','minato','summoning','naruto roar','flying thunder','gamabunta naruto']))
    (OUT/'audio/extra-search.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(rows,ensure_ascii=False))
elif __name__=='__main__':
    terms=['rasengan','amaterasu','rasenshuriken','hiraishin','kuchiyose','katon','kurama','gamabunta','sharingan','chidori']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:rows=list(pool.map(search,terms))
    (OUT/'audio/search-results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(rows,ensure_ascii=False))
