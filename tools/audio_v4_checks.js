(async()=>{
 const results=[],check=(name,ok,detail='')=>{results.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+detail);},wait=ms=>new Promise(r=>setTimeout(r,ms));
 const until=async(test)=>{for(let k=0;k<200;k++){if(test())return;await wait(15);}throw Error('audio event did not arrive');};
 if(!audioUnlocked)throw Error('Activate atmosphere with a real click first.');
 if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=true;prefs.ambience=false;ambience();await wait(40);audioEngine.stopCues('test-isolation');await wait(30);
 const manifest=await fetch('../revisions/v4/audio-manifest.json').then(r=>r.json()),buffers={};
 for(const row of manifest){const url=KonohaCueSources[row.technique][row.event].url;const buffer=await audioEngine.load(url);buffers[row.technique+'-'+row.event]=buffer;check(row.technique+' '+row.event+' decodes full prepared bytes',Math.abs(buffer.duration-row.duration)<.01);if(row.event==='confirm')check(row.technique+' activation retains complete source',Math.abs(buffer.duration-row.source_duration)<.06);}
 const events=[];
 for(const technique of techniques.list){
  audioEngine.stopCues('test-isolation');await wait(25);audioEngine.events=[];setTechnique(technique.id);document.querySelector('#dock .app-card').focus();
  KonohaRemote('ArrowRight');await until(()=>audioEngine.events.some(e=>e.event==='cue-started'));
  const charge=audioEngine.events.find(e=>e.event==='cue-started');check(technique.id+' D-pad only plays readiness',charge.type==='focus'&&charge.url===KonohaCueSources[technique.cue].focus.url&&!audioEngine.events.some(e=>e.type==='confirm'));
  KonohaRemote('Enter');await until(()=>audioEngine.events.some(e=>e.event==='cue-started'&&e.type==='confirm'));
  const activation=audioEngine.events.find(e=>e.event==='cue-started'&&e.type==='confirm');check(technique.id+' OK uses complete activation / voice',activation.url===KonohaCueSources[technique.cue].confirm.url&&activation.url!==charge.url);
  check(technique.id+' confirm does not stop readiness',!audioEngine.events.some(e=>e.event==='cue-stopped'));
  await audioEngine.playCue(technique.cue,'focus');check(technique.id+' repeated readiness does not restart',audioEngine.events.filter(e=>e.event==='cue-started'&&e.type==='focus').length===1);
  await wait(Math.max(charge.duration,activation.duration)*1000+80);
  check(technique.id+' both voices finish naturally',['focus','confirm'].every(type=>audioEngine.events.some(e=>e.type===type&&e.event==='cue-ended'&&e.elapsed>=e.duration-.035))&&!audioEngine.events.some(e=>e.event==='cue-stopped'));
  events.push({technique:technique.id,events:[...audioEngine.events]});
 }
 setTechnique('rasengan');audioEngine.events=[];document.querySelector('#dock .app-card').focus();
 for(let k=0;k<12;k++)KonohaRemote(k%2?'ArrowLeft':'ArrowRight');await wait(70);
 check('rapid navigation never schedules a spoken activation',audioEngine.events.every(e=>e.type==='focus'));
 check('rapid navigation coalesces readiness instead of layering twelve copies',audioEngine.events.filter(e=>e.event==='cue-started').length===1);
 await wait(1500);audioEngine.events=[];setScene('gaara');await wait(90);check('wallpaper change does not say a technique name',audioEngine.events.filter(e=>e.event==='cue-started').every(e=>e.cue==='clone'));
 audioEngine.stopCues('test-isolation');prefs.ambience=true;ambience();await until(()=>audioEngine.snapshot().musicNodes.length>=2);const before=audioEngine.snapshot();prefs.musicVolume=.18;ambience();const after=audioEngine.snapshot();
 check('music volume does not restart loop',before.musicNodes[0].start===after.musicNodes[0].start);check('music overlap remains 3.5 seconds',Math.abs(after.musicNodes[0].end-after.musicNodes[1].start-3.5)<.001);
 await audioEngine.playCue('chidori','confirm');YoruPause();await wait(45);check('pause stops every new cue and music node',!audioEngine.snapshot().cues.length&&!audioEngine.snapshot().musicNodes.length);YoruRefresh();
 prefs.audio=false;ambience();await wait(30);check('mute stops every audio voice',!audioEngine.snapshot().cues.length&&!audioEngine.snapshot().musicNodes.length);
 return {passed:results.length,total:results.length,results,events};
})()
