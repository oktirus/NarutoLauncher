"""Prepare intentional cue envelopes; retain source files and document every cut/mix."""
from pathlib import Path
import array,hashlib,json,math,struct,subprocess,wave
from analyze_audio_v5 import metrics,envelope
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'assets/audio/naruto';V5=ROOT/'revisions/v5';RATE=44100
old=json.loads((ROOT/'revisions/v4/audio-manifest.json').read_text(encoding='utf-8'))
sources={r['source_id']:r for r in old}
spec={
 'rasengan':{'focus':('rasengan-c',0,1.45),'confirm':('rasengan-a',0,None)},
 'chidori':{'focus':('old-chidori',2.9,1.25),'confirm':('chidori-voice',0,None)},
 'sharingan':{'focus':('old-sharingan',0,None),'confirm':('sharingan-voice',0,None)},
 'kurama':{'focus':('kurama',.2,1.2),'confirm':('kurama',0,4.45)},
 'summoning':{'focus':('old-clone',0,None),'confirm':('summoning',.5,1.72)},
 'hiraishin':{'focus':('hiraishin-in',0,None),'confirm':('hiraishin-out',0,None)},
 'fireball':{'focus':('fireball',3.1,1.5),'confirm':('fireball',0,None)},
 'amaterasu':{'focus':('amaterasu-itachi',2.2,1.85),'confirm':('amaterasu-itachi',0,None)},
 'rasenshuriken':{'focus':('rasenshuriken',.43,1.35),'confirm':('rasenshuriken-voice',0,None)},
 'gamabunta':{'focus':('gamabunta',1.18,.94),'confirm':('summoning',.5,1.72)},
 'ui':{'confirm':('old-clone',0,.27)},
}
def normalized(source_id,start,length,extra=''):
    source=ROOT/sources[source_id]['source'];filters=[f'atrim=start={start}'+(f':duration={length}' if length is not None else ''),'asetpts=PTS-STARTPTS']
    if extra:filters.append(extra)
    filters+=['loudnorm=I=-22:LRA=11:TP=-2','aresample=44100']
    raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-vn','-af',','.join(filters),'-ar',str(RATE),'-ac','1','-f','f32le','-'])
    samples=array.array('f');samples.frombytes(raw)
    values=envelope(samples,RATE,.01);threshold=max(max(values)*.015,.0001);active=next((i for i,v in enumerate(values) if v>threshold),0)
    trim=max(0,active*.01-.02);return array.array('f',samples[round(trim*RATE):]),trim
def ramps(samples,release):
    attack=min(round(.008*RATE),len(samples));tail=min(round(release*RATE),len(samples))
    for i in range(attack):samples[i]*=math.sin(i/max(1,attack-1)*math.pi/2)**2
    for i in range(tail):samples[len(samples)-tail+i]*=math.cos(i/max(1,tail-1)*math.pi/2)**2
    return samples
records=[];registry={}
for cue,variants in spec.items():
    registry[cue]={}
    for event,(source_id,start,length) in variants.items():
        extra='highpass=f=400,lowpass=f=3200' if cue=='ui' else ''
        samples,head=normalized(source_id,start,length,extra)
        release=.22 if event=='focus' else .30
        if cue in ['summoning','gamabunta'] and event=='confirm':release=.16
        if cue=='rasenshuriken' and event=='confirm':release=.15
        if cue=='ui':release=.20
        ramps(samples,release);mix=None
        if cue=='gamabunta' and event=='confirm':
            impact,impact_head=normalized('gamabunta',1.18,.72,'lowpass=f=1200');ramps(impact,.28)
            offset=max(0,len(samples)-round(.16*RATE));combined=array.array('f',[0])*max(len(samples),offset+len(impact))
            for i,v in enumerate(samples):combined[i]+=v
            for i,v in enumerate(impact):combined[offset+i]+=v*.45
            samples=combined;mix={'source':sources['gamabunta']['source'],'source_page':sources['gamabunta']['source_page'],'source_start':1.18+impact_head,'source_window':.72,'offset':offset/RATE,'gain':.45,'release':.28}
        dest=OUT/f'v5-{cue}-{event}.wav'
        pcm=array.array('h',(round(max(-1,min(1,v))*32767) for v in samples))
        with wave.open(str(dest),'wb') as file:file.setnchannels(1);file.setsampwidth(2);file.setframerate(RATE);file.writeframes(pcm.tobytes())
        # Measure the quantized deliverable, not just the float preparation buffer.
        quantized=array.array('f',(v/32768 for v in pcm));quality=metrics(quantized,RATE)
        if quality['leading_silence']>.08 or quality['tail_ratio_50ms']>.1:raise RuntimeError(f'{cue}/{event} failed envelope guard: {quality}')
        digest=hashlib.sha256(dest.read_bytes()).hexdigest();gain=.24 if cue=='ui' else .42 if event=='focus' else .8
        records.append({'technique':cue,'event':event,'source_id':source_id,'source':sources[source_id]['source'],'source_page':sources[source_id]['source_page'],'source_sha256':hashlib.sha256((ROOT/sources[source_id]['source']).read_bytes()).hexdigest(),'source_duration':sources[source_id]['source_duration'],'source_window':{'start':start,'duration':length},'leading_trim':head,'release_seconds':release,'mix':mix,'file':dest.relative_to(ROOT).as_posix(),'duration':len(pcm)/RATE,'sha256':digest,'quality':quality,'gain':gain,'processing':'explicit source window, remove leading silence, loudness normalization, cosine-squared attack/release; optional documented landing mix'})
        registry[cue][event]={'url':'../'+dest.relative_to(ROOT).as_posix()+'?v='+digest[:12],'gain':gain}
(V5/'audio-manifest.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'prototype/audio-sources.js').write_text("'use strict';\n/* Separate non-verbal readiness, activation and neutral service cues. */\nwindow.KonohaCueSources="+json.dumps(registry,indent=2)+";\n",encoding='utf-8')
print(json.dumps([{'cue':r['technique'],'event':r['event'],'duration':round(r['duration'],3),'head':r['quality']['leading_silence'],'tail_ratio':round(r['quality']['tail_ratio_50ms'],4)} for r in records]))
