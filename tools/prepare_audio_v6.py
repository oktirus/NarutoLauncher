"""Add common hand-seal readiness and ordinary Sharingan activation; preserve v5."""
from pathlib import Path
import array, hashlib, json, math, subprocess, wave
from analyze_audio_v5 import metrics, envelope
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'revisions/v6/references/naruto-jutsu-source.mp3'
raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-af','loudnorm=I=-22:LRA=11:TP=-2','-ar','44100','-ac','1','-f','f32le','-'])
samples=array.array('f');samples.frombytes(raw);rate=44100
env=envelope(samples,rate,.01);threshold=max(env)*.025
start=max(0,next((i for i,x in enumerate(env) if x>threshold),0)-2)*.01
samples=samples[round(start*rate):]
for i in range(min(353,len(samples))):samples[i]*=math.sin(i/352*math.pi/2)**2
tail=min(round(.25*rate),len(samples))
for i in range(tail):samples[len(samples)-tail+i]*=math.cos(i/(tail-1)*math.pi/2)**2
dest=ROOT/'assets/audio/naruto/v6-jutsu-focus.wav'
pcm=array.array('h',(round(max(-1,min(1,v))*32767) for v in samples))
with wave.open(str(dest),'wb') as out:out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes(pcm.tobytes())
quality=metrics(array.array('f',(x/32768 for x in pcm)),rate)
assert quality['leading_silence']<=.08 and quality['tail_ratio_50ms']<=.1, quality
digest=hashlib.sha256(dest.read_bytes()).hexdigest()
script=ROOT/'prototype/audio-sources.js'
data=json.loads(script.read_text(encoding='utf-8').split('window.KonohaCueSources=')[1].strip().rstrip(';'))
data['jutsu']={'focus':{'url':'../assets/audio/naruto/v6-jutsu-focus.wav?v='+digest[:12],'gain':.42}}
# The former confirm says Mangekyo; ordinary three-tomoe uses the non-verbal activation.
data['sharingan']['confirm']={**data['sharingan']['focus'],'gain':.7}
script.write_text("'use strict';\n/* Common selectable focus cue; technique-specific confirmation. */\nwindow.KonohaCueSources="+json.dumps(data,indent=2)+';\n',encoding='utf-8')
manifest={'jutsu':{'source':'revisions/v6/references/naruto-jutsu-source.mp3','page':'https://www.myinstants.com/en/instant/naruto-jutsu-sound-39884/','download':'https://www.myinstants.com/media/sounds/katon_aoszcAp.mp3','source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'file':dest.relative_to(ROOT).as_posix(),'sha256':digest,'leading_trim':start,'release':.25,'quality':quality},'focus_options':['jutsu','amaterasu','rasenshuriken','hiraishin','summoning','sharingan'],'sharingan_confirm':{'url':data['sharingan']['confirm']['url'],'reason':'ordinary Sharingan, non-verbal activation; removes Mangekyo phrase; v5 file preserved'}}
(ROOT/'revisions/v6/audio-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(quality))
