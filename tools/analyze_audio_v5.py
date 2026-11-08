"""Small dependency-free PCM envelope audit, including source-level tails."""
from pathlib import Path
import array,json,math,subprocess
ROOT=Path(__file__).resolve().parents[1]
def decode(path,rate=22050):
    data=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-ar',str(rate),'-ac','1','-f','f32le','-'])
    samples=array.array('f');samples.frombytes(data);return samples,rate
def rms(samples):return math.sqrt(sum(x*x for x in samples)/max(1,len(samples)))
def envelope(samples,rate,step=.05):
    n=round(rate*step);return [round(rms(samples[i:i+n]),6) for i in range(0,len(samples),n)]
def metrics(samples,rate):
    values=envelope(samples,rate,.01);peak=max(values,default=0);threshold=max(peak*.02,.0001)
    active=[i for i,v in enumerate(values) if v>threshold]
    return {'duration':len(samples)/rate,'peak_rms_10ms':peak,'leading_silence':active[0]*.01 if active else len(samples)/rate,'trailing_silence':max(0,len(samples)/rate-(active[-1]+1)*.01) if active else len(samples)/rate,'tail_rms_20ms':rms(samples[-round(rate*.02):]),'tail_rms_50ms':rms(samples[-round(rate*.05):]),'tail_ratio_50ms':rms(samples[-round(rate*.05):])/peak if peak else 0}
if __name__=='__main__':
    rows=[]
    for name in ['gamabunta','kurama','summoning','fireball','rasengan-a','rasenshuriken']:
        path=ROOT/'revisions/v4/audio'/(name+'-source.mp3');samples,rate=decode(path)
        rows.append({'file':str(path.relative_to(ROOT)),**metrics(samples,rate),'rms_100ms':envelope(samples,rate,.1)})
    (ROOT/'revisions/v5/verification/source-envelope-audit.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
    for row in rows:print(json.dumps(row))
