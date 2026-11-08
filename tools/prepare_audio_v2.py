"""Preserve public source clips; derive short, level-matched local UI excerpts."""
from pathlib import Path
import requests,re,json,hashlib,subprocess,shutil
from bs4 import BeautifulSoup
from urllib.parse import urlparse
ROOT=Path(__file__).resolve().parents[1];SRC=ROOT/'revisions/v2/audio';OUT=ROOT/'assets/audio/naruto';OUT.mkdir(exist_ok=True)
sources={
 'rasengan':('https://soundfactory.ai/rasengan-sound-effect/embed/',1.2),
 'chidori':('https://www.myinstants.com/en/instant/chidori-sound-4995/',2.3),
 'sharingan':('https://www.myinstants.com/en/instant/sharingan-sfx-35806/',.1),
 'clone':('https://www.myinstants.com/en/instant/smoke-clone-naruto-59408/',0),
 'fireball':('https://www.myinstants.com/en/instant/fireball-jutsu-12782/',3.5),
 'amaterasu':('https://www.myinstants.com/en/instant/amaterasu/',.3),
}
rows=[]
for name,(url,start) in sources.items():
 r=requests.get(url,timeout=30);r.raise_for_status();(SRC/(name+'-source.html')).write_text(r.text,encoding='utf-8')
 if 'soundfactory.ai' in url:
  audio_url=BeautifulSoup(r.text,'html.parser').select_one('video[src]')['src']
 else:
  paths=re.findall(r'[\"\'](/media/sounds/[^\"\']+\.mp3)',r.text)
  if not paths:raise RuntimeError('Missing public clip '+name)
  audio_url='https://www.myinstants.com'+paths[0]
 source=SRC/(name+'-source'+Path(urlparse(audio_url).path).suffix);source.write_bytes(requests.get(audio_url,timeout=30).content)
 duration=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(source)],text=True).strip())
 start=min(start,max(0,duration-.85))
 for cue,length in [('focus',.24),('confirm',.7)]:
  dest=OUT/(name+'-'+cue+'.wav')
  # dynaudnorm matches wildly different public upload levels; fades avoid cut clicks.
  subprocess.run(['ffmpeg','-y','-v','error','-ss',str(start),'-i',str(source),'-t',str(length),'-af',f'dynaudnorm=f=50:g=5:p=0.7,afade=t=in:d=0.008,afade=t=out:st={length-.045}:d=0.045','-ar','44100','-ac','1',str(dest)],check=True)
  rows.append({'file':dest.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'source_page':url,'source_audio':audio_url,'source_file':source.relative_to(ROOT).as_posix(),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'source_duration':duration,'excerpt_start':start,'excerpt_duration':length,'provenance':'Public upload on SoundFactory or Myinstants; not studio-verified'})
music=[]
for row in json.loads((SRC/'music-previews.json').read_text(encoding='utf-8')):
 dest=OUT/(str(row['trackId'])+'.m4a');shutil.copy2(SRC/(str(row['trackId'])+'.m4a'),dest)
 music.append({'id':str(row['trackId']),'name':row['trackName'],'artist':row['artistName'],'file':dest.relative_to(ROOT).as_posix(),'source':row['trackViewUrl'],'preview_url':row['previewUrl'],'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'duration':float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(dest)],text=True).strip()),'type':'Official Apple Music OST preview; original unmodified excerpt, not full track'})
(ROOT/'revisions/v2/audio-manifest.json').write_text(json.dumps({'cues':rows,'music':music},ensure_ascii=False,indent=2),encoding='utf-8')
print('Prepared',len(rows),'short anime cues and',len(music),'official OST previews.')
