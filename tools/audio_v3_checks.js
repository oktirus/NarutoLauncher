(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);},wait=ms=>new Promise(r=>setTimeout(r,ms));
 if(!audioUnlocked)throw Error('Activate audio with a real browser click before running this suite.');
 prefs.audio=true;prefs.ambience=false;ambience();await wait(35);audioEngine.events=[];
 const audioManifest=await fetch('../revisions/v3/audio-manifest.json').then(r=>r.json());
 for(const row of audioManifest){const buffer=await audioEngine.load('../'+row.file+'?v=4');check(row.cue+' retains complete source duration',Math.abs(buffer.duration-row.source_duration)<.06,buffer.duration);}
 await audioEngine.playCue('chidori','confirm');const started=audioEngine.snapshot().cues.find(v=>v.cue==='chidori');await audioEngine.playCue('rasengan','focus');await audioEngine.playCue('clone','confirm');await audioEngine.playCue('sharingan','confirm');await wait(250);
 check('new cues do not stop the long cue',audioEngine.snapshot().cues.some(v=>v.cue==='chidori'&&v.start===started.start));check('different cues overlap without truncation',audioEngine.snapshot().cues.length>=4);
 const count=audioEngine.snapshot().events.filter(e=>e.event==='cue-started'&&e.cue==='rasengan').length;await audioEngine.playCue('rasengan','focus');check('rapid focus does not restart an active cue',audioEngine.snapshot().events.filter(e=>e.event==='cue-started'&&e.cue==='rasengan').length===count);
 await wait(8100);const natural=audioEngine.snapshot().events;
 check('all four cues finish naturally',['chidori','rasengan','clone','sharingan'].every(c=>natural.some(e=>e.cue===c&&e.event==='cue-ended'&&e.elapsed>=e.duration-.03))&&!natural.some(e=>e.event==='cue-stopped'));
 const music=[];
 for(const id of['1584791975','1584792236','1584792240']){
  const buffer=await audioEngine.load('../assets/audio/naruto/'+id+'.m4a'),rate=22050,seconds=65,offline=new OfflineAudioContext(2,rate*seconds,rate),fade=3.5,step=buffer.duration-fade;
  for(let start=0;start<seconds;start+=step)KonohaAudio.scheduleLoop(offline,offline.destination,buffer,start,fade);
  const rendered=await offline.startRendering(),samples=rendered.getChannelData(0),seams=[];
  for(const at of[step,step+fade/2,step+fade,step*2,step*2+fade/2,step*2+fade]){let energy=0,n=0;for(let i=Math.floor((at-.075)*rate);i<(at+.075)*rate;i++){energy+=samples[i]*samples[i];n++;}seams.push({at,rms:Math.sqrt(energy/n)});}
  check(id+' two music seams contain continuous audio',seams.every(s=>s.rms>.0001));music.push({id,crossfade:fade,duration:buffer.duration,seams});
 }
 prefs.ambience=true;ambience();await wait(90);const before=audioEngine.snapshot();prefs.musicVolume=.18;ambience();const after=audioEngine.snapshot();check('volume adjustment does not restart music',before.musicNodes[0]?.start===after.musicNodes[0]?.start&&after.musicNodes.length>=2);
 check('live music nodes overlap exactly 3.5 seconds',Math.abs(after.musicNodes[0].end-after.musicNodes[1].start-3.5)<.001);
 YoruPause();await wait(40);check('pause stops every audio voice',audioEngine.snapshot().cues.length===0&&audioEngine.snapshot().musicNodes.length===0);YoruRefresh();
 prefs.audio=false;ambience();await wait(30);check('mute stops music and effects',audioEngine.snapshot().cues.length===0&&audioEngine.snapshot().musicNodes.length===0);
 return {passed:results.length,total:results.length,results,naturalCueEvents:natural,music};
})()
