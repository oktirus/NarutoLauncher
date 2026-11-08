"""Decode the complete source clips, without duration slicing or truncation."""
from pathlib import Path
import json,subprocess,hashlib,wave
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'assets/audio/naruto';records=[]
old=json.loads((ROOT/'revisions/v2/audio-manifest.json').read_text(encoding='utf-8'))
for cue in ('rasengan','chidori','sharingan','clone'):
 row=next(r for r in old['cues'] if Path(r['file']).name==cue+'-confirm.wav');source=ROOT/row['source_file'];dest=OUT/('full-'+cue+'.wav')
 duration=row['source_duration']
 # Only a 5 ms entrance and 20 ms safety tail ramp; no -ss/-t, all content decoded.
 subprocess.run(['ffmpeg','-y','-v','error','-i',str(source),'-vn','-af',f'loudnorm=I=-22:LRA=11:TP=-2,afade=t=in:d=0.005,afade=t=out:st={max(0,duration-.02)}:d=0.02','-ar','44100','-ac','1',str(dest)],check=True)
 with wave.open(str(dest),'rb') as f:decoded=f.getnframes()/f.getframerate()
 records.append({'cue':cue,'source':row['source_file'],'source_page':row['source_page'],'source_duration':duration,'file':dest.relative_to(ROOT).as_posix(),'duration':decoded,'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'processing':'complete source decode, loudness normalization, 5ms/20ms safety ramps, no crop'})
(ROOT/'revisions/v3/audio-manifest.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
print([(r['cue'],round(r['duration'],3),round(r['source_duration'],3)) for r in records])
