(async()=>{
 const checks=[],samples=[],opens=[];
 const check=(name,ok)=>{checks.push({name,passed:!!ok});if(!ok)throw Error(name);};
 const sleep=ms=>new Promise(r=>setTimeout(r,ms));
 const wait=async predicate=>{const until=performance.now()+7600;while(!predicate()){if(performance.now()>until)throw Error('Timed out');await sleep(15);}};
 const originalToast=toast,originalPrefs={...prefs},pkg=apps[0].package,button=document.querySelector('#dock .app-card');
 toast=text=>{if(text.includes('у макеті показано'))opens.push({time:performance.now(),events:audioEngine.snapshot().events});originalToast(text);};
 try{
  prefs.audio=true;prefs.ambience=false;prefs.volume=0;prefs.musicVolume=0;ambience();
  check('real running AudioContext',audioReady());
  for(const technique of techniques.list){
   prefs.technique=technique.id;audioEngine.stopCues('test-reset');audioEngine.events=[];lastLaunchTime=-Infinity;button.focus();
   const before=opens.length,start=performance.now();launch(pkg);
   check(technique.id+' waits after OK',!!pendingLaunch&&opens.length===before);
   check(technique.id+' busy state',button.getAttribute('aria-busy')==='true');
   launch(pkg);launch(apps[1].package);
   await wait(()=>!pendingLaunch);
   const events=audioEngine.snapshot().events,end=events.find(e=>e.event==='cue-ended'&&e.cue===technique.cue&&e.type==='confirm');
   check(technique.id+' one launch',opens.length===before+1);
   check(technique.id+' one confirm',events.filter(e=>e.event==='cue-started'&&e.type==='confirm').length===1);
   check(technique.id+' full cue before open',!!end&&end.elapsed>=end.duration-.04&&opens.at(-1).events.some(e=>e===end));
   check(technique.id+' busy cleared',!button.hasAttribute('aria-busy'));
   samples.push({technique:technique.id,cueSeconds:end.duration,elapsedMs:performance.now()-start});
  }
  return{passed:checks.length,checks,samples};
 }finally{cancelLaunch();toast=originalToast;Object.assign(prefs,originalPrefs);ambience();}
})()
