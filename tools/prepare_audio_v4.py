"""Separate non-verbal charge cues from complete activation/voice cues."""
from pathlib import Path
import hashlib,json,subprocess,wave
ROOT=Path(__file__).resolve().parents[1];V4=ROOT/'revisions/v4';OUT=ROOT/'assets/audio/naruto'
sources={r['id']:r for name in ['candidates.json','precise-sources.json','voice-sources.json'] for r in json.loads((V4/'audio'/name).read_text(encoding='utf-8')) if not r.get('error')}
for row in json.loads((ROOT/'revisions/v3/audio-manifest.json').read_text(encoding='utf-8')):
    sources['old-'+row['cue']]={'source':row['source'],'page':row['source_page'],'duration':row['source_duration']}
# Focus uses only a non-verbal SFX window. Confirm retains the complete source phrase.
spec={
 'rasengan':{'focus':('rasengan-c',0,1.45),'confirm':('rasengan-a',None,None)},
 'chidori':{'focus':('old-chidori',2.9,1.25),'confirm':('chidori-voice',None,None)},
 'sharingan':{'focus':('old-sharingan',None,None),'confirm':('sharingan-voice',None,None)},
 'kurama':{'focus':('kurama',.2,1.2),'confirm':('kurama',None,None)},
 'summoning':{'focus':('old-clone',None,None),'confirm':('summoning',None,None)},
 'hiraishin':{'focus':('hiraishin-in',None,None),'confirm':('hiraishin-out',None,None)},
 'fireball':{'focus':('fireball',3.1,1.5),'confirm':('fireball',None,None)},
 'amaterasu':{'focus':('amaterasu-itachi',2.2,1.85),'confirm':('amaterasu-itachi',None,None)},
 'rasenshuriken':{'focus':('rasenshuriken',0,1.8),'confirm':('rasenshuriken-voice',None,None)},
 'gamabunta':{'focus':('gamabunta',.25,1.15),'confirm':('summoning',None,None)},
}
records=[];registry={}
def duration(path):
    with wave.open(str(path),'rb') as file:return file.getnframes()/file.getframerate()
for cue,variants in spec.items():
    registry[cue]={}
    for event,(source_id,start,length) in variants.items():
        row=sources[source_id];source=ROOT/row['source'];dest=OUT/f'v4-{cue}-{event}.wav'
        filters=[]
        if start is not None:filters+= [f'atrim=start={start}:duration={length}','asetpts=PTS-STARTPTS']
        filters+=['loudnorm=I=-22:LRA=11:TP=-2','aresample=44100','afade=t=in:d=0.005']
        # Fade the last 15 ms after reversing to avoid relying on container duration.
        filters+=['areverse','afade=t=in:d=0.015','areverse']
        subprocess.run(['ffmpeg','-v','error','-y','-i',str(source),'-vn','-af',','.join(filters),'-ar','44100','-ac','1',str(dest)],check=True)
        digest=hashlib.sha256(dest.read_bytes()).hexdigest()
        records.append({'technique':cue,'event':event,'source_id':source_id,'source':row['source'],'source_page':row['page'],'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'source_duration':row['duration'],'window':None if start is None else {'start':start,'duration':length},'file':dest.relative_to(ROOT).as_posix(),'duration':duration(dest),'sha256':digest,'processing':'non-verbal SFX window' if event=='focus' else 'complete source phrase / activation SFX, no crop','gain':.42 if event=='focus' else .8})
        registry[cue][event]={'url':'../'+dest.relative_to(ROOT).as_posix()+'?v='+digest[:12],'gain':.42 if event=='focus' else .8}
(V4/'audio-manifest.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'prototype/audio-sources.js').write_text("'use strict';\n/* Non-verbal focus and complete activation are deliberately separate assets. */\nwindow.KonohaCueSources="+json.dumps(registry,indent=2)+";\n",encoding='utf-8')
print([(r['technique'],r['event'],round(r['duration'],3)) for r in records])
