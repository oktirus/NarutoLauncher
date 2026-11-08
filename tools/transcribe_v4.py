"""Local supplementary ASR evidence; never used as proof of a voice actor's identity."""
from pathlib import Path
import json, subprocess,sys
import numpy as np
from faster_whisper import WhisperModel
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v4'
model=WhisperModel('small' if '--small' in sys.argv else 'base',device='cpu',compute_type='int8',cpu_threads=6)
rows=[]
for path in sorted((OUT/'audio').glob('*-source.mp3')):
    if '--small' in sys.argv and path.stem not in ['rasengan-a-source','rasengan-c-source','amaterasu-itachi-source','rasenshuriken-voice-source','chidori-voice-source','sharingan-voice-source','summoning-source']:continue
    raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-ar','16000','-ac','1','-f','f32le','-'])
    segments,info=model.transcribe(np.frombuffer(raw,dtype=np.float32),language='ja',beam_size=5,vad_filter=False,condition_on_previous_text=False)
    row={'file':path.name,'segments':[{'start':s.start,'end':s.end,'text':s.text,'logprob':s.avg_logprob,'no_speech_prob':s.no_speech_prob} for s in segments]}
    rows.append(row);print(json.dumps(row,ensure_ascii=False),flush=True)
    (OUT/('audio/asr-small-review.json' if '--small' in sys.argv else 'audio/asr-review.json')).write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
