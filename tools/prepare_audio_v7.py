"""Use only the final activation after the hand signs, preserving all older cues."""
from pathlib import Path
import array,hashlib,json,math,subprocess,wave
from analyze_audio_v5 import metrics
ROOT=Path(__file__).resolve().parents[1];source=ROOT/'revisions/v7/references/hand-signs-source.mp3'
start=1.48
raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-af',f'atrim=start={start},asetpts=PTS-STARTPTS,loudnorm=I=-22:LRA=11:TP=-2','-ar','44100','-ac','1','-f','f32le','-'])
samples=array.array('f');samples.frombytes(raw);rate=44100;attack=round(.004*rate);release=round(.065*rate)
for i in range(attack):samples[i]*=math.sin(i/(attack-1)*math.pi/2)**2
for i in range(release):samples[-release+i]*=math.cos(i/(release-1)*math.pi/2)**2
pcm=array.array('h',(round(max(-1,min(1,v))*32767) for v in samples));dest=ROOT/'assets/audio/naruto/v7-seal-complete-focus.wav'
with wave.open(str(dest),'wb')as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(rate);f.writeframes(pcm.tobytes())
quality=metrics(array.array('f',(x/32768 for x in pcm)),rate);assert quality['leading_silence']<=.03 and quality['tail_ratio_50ms']<=.1,quality
digest=hashlib.sha256(dest.read_bytes()).hexdigest();path=ROOT/'prototype/audio-sources.js';data=json.loads(path.read_text(encoding='utf-8').split('window.KonohaCueSources=')[1].strip().rstrip(';'));data['jutsu']['focus']={'url':'../'+dest.relative_to(ROOT).as_posix()+'?v='+digest[:12],'gain':.42};path.write_text("'use strict';\n/* Completed hand seal: final activation, not the preceding signs. */\nwindow.KonohaCueSources="+json.dumps(data,indent=2)+';\n',encoding='utf-8')
report={'id':'jutsu','label':'Завершення печатки','source':source.relative_to(ROOT).as_posix(),'source_page':'https://www.myinstants.com/en/instant/naruto-hand-signs-40193/','download':'https://www.myinstants.com/media/sounds/ytmp3free_BaQB7oM.mp3','source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'window_start':start,'window_end':'end of source, including natural decay','attack':.004,'release':.065,'file':dest.relative_to(ROOT).as_posix(),'sha256':digest,'quality':quality}
(ROOT/'revisions/v7/audio-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(quality))
