"""Original shinobi-themed UI sound design, synthesized locally (no anime samples)."""
import array, hashlib, json, math, random, wave
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; OUT=ROOT/'assets/audio'; OUT.mkdir(parents=True,exist_ok=True)
SR=44100; RNG=random.Random(7281)
def write(name, duration, function, loop_overlap=0):
 values=[]; state=0.0
 for i in range(round(SR*(duration+loop_overlap))):
  t=i/SR;state=.88*state+.12*RNG.uniform(-1,1)
  values.append(function(t,state))
 if loop_overlap:
  overlap=round(SR*loop_overlap)
  # Join a continuous extra tail to the beginning, then return to the original
  # opening with a raised-cosine blend. The output retains its full 24 s length.
  for i in range(overlap):
   blend=.5-.5*math.cos(math.pi*i/(overlap-1))
   values[i]=values[i]*blend+values[-overlap+i]*(1-blend)
  values=values[:-overlap]
 peak=max(abs(v) for v in values) or 1
 scale=min(1,.72/peak)
 pcm=array.array('h',(int(max(-1,min(1,v*scale))*32767) for v in values))
 path=OUT/(name+'.wav')
 with wave.open(str(path),'wb') as f:f.setparams((1,2,SR,0,'NONE','not compressed'));f.writeframes(pcm.tobytes())
 return {'file':path.relative_to(ROOT).as_posix(),'duration':duration,'sample_rate':SR,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'origin':'Original local procedural synthesis; not sampled from Naruto soundtrack or dialogue.'}
tau=2*math.pi
def env(t,d):return min(1,t/.012)*max(0,1-t/d)**2
manifest=[]
manifest.append(write('chakra-focus',.18,lambda t,n:env(t,.18)*(.35*n+ .12*math.sin(tau*(900*t-1600*t*t)))))
manifest.append(write('chakra-confirm',.65,lambda t,n:env(t,.65)*(.23*math.sin(tau*(170*t+400*t*t))+.12*math.sin(tau*1174.66*t)*math.exp(-t*5)+.35*n*math.exp(-t*9))+.14*math.sin(tau*72*t)*math.exp(-t*22)))
manifest.append(write('scroll-back',.24,lambda t,n:env(t,.24)*(.3*n+.1*math.sin(tau*(650*t-800*t*t)))))
manifest.append(write('scene-wind',1.2,lambda t,n:math.sin(math.pi*t/1.2)**2*(.22*n+.08*math.sin(tau*293.66*t)+.055*math.sin(tau*440*t)+.04*math.sin(tau*587.33*t))))
# Continuous village air; loop continuity comes from the overlapping tail, not silence.
def ambient(t,n):
 wind=.11*n*(.65+.35*math.sin(tau*t/12))
 air=.022*math.sin(tau*146.833333*t)+.012*math.sin(tau*220*t)
 bell=sum(.025*math.sin(tau*f*t)*math.exp(-((t-start)%24)*1.8) for start,f in [(3,587.33),(11,659.25),(18,880)])
 return wind+air+bell
manifest.append(write('konoha-air',24,ambient,loop_overlap=2))
(ROOT/'research/audio-manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
print('Generated',len(manifest),'original WAV assets')
