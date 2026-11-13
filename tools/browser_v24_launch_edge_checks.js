(async()=>{
 const checks=[],opens=[],sleep=ms=>new Promise(r=>setTimeout(r,ms));
 const check=(name,ok)=>{checks.push({name,passed:!!ok});if(!ok)throw Error(name);};
 const wait=async()=>{const until=performance.now()+9000;while(pendingLaunch){if(performance.now()>until)throw Error('Timed out');await sleep(15);}};
 const originalToast=toast,originalPrefs={...prefs},load=audioEngine.load,play=audioEngine.playCue,unlock=unlockAudio,pkg=apps[0].package;
 const reset=()=>{cancelLaunch();lastLaunchTime=-Infinity;prefs.audio=true;prefs.ambience=false;prefs.volume=0;prefs.musicVolume=0;prefs.technique='rasengan';audioEngine.load=load;audioEngine.playCue=play;unlockAudio=unlock;audioEngine.unlocked=true;};
 toast=text=>{if(text.includes('у макеті показано'))opens.push(performance.now());originalToast(text);};
 try{
  reset();ambience();prefs.audio=false;let cueCalls=0;audioEngine.playCue=()=>{cueCalls++;throw Error('Muted must not play');};
  let before=opens.length,start=performance.now();launch(pkg);
  check('muted opens synchronously',opens.length===before+1&&!pendingLaunch&&performance.now()-start<50);
  check('muted does not invoke audio',cueCalls===0);
  reset();audioEngine.load=async()=>{throw Error('injected missing file');};before=opens.length;launch(pkg);await wait();
  check('missing audio still opens',opens.length===before+1);
  reset();audioEngine.unlocked=false;unlockAudio=async()=>false;before=opens.length;launch(pkg);await wait();
  check('blocked audio still opens',opens.length===before+1);
  reset();before=opens.length;launch(pkg);await sleep(120);remote('Escape');await sleep(100);
  check('Back cancels playing cue and launch',!pendingLaunch&&opens.length===before&&audioEngine.voices.size===0);
  launch(pkg);check('Back permits immediate retry',!!pendingLaunch);cancelLaunch();
  reset();prefs.technique='hiraishin';before=opens.length;launch(pkg);await wait();launch(pkg);
  check('repeat OK after completed voice is debounced',opens.length===before+1&&!pendingLaunch);
  reset();const buffer=await load.call(audioEngine,KonohaCueSources.rasengan.confirm.url);let release;
  audioEngine.load=()=>new Promise(resolve=>{release=resolve;});before=opens.length;audioEngine.events=[];launch(pkg);await sleep(30);remote('Escape');release(buffer);await sleep(100);
  check('Back during decode prevents late launch',opens.length===before&&!pendingLaunch);
  check('Back during decode prevents late voice',!audioEngine.events.some(e=>e.event==='cue-started'));
  reset();before=opens.length;launch(pkg);await sleep(80);window.YoruPause();await sleep(100);
  check('lifecycle pause cancels pending launch',opens.length===before&&!pendingLaunch);
  window.YoruRefresh();reset();audioEngine.playCue=()=>new Promise(()=>{});before=opens.length;start=performance.now();launch(pkg);await wait();
  check('stuck audio bounded by failsafe',opens.length===before+1&&performance.now()-start>=7900&&performance.now()-start<8800);
  return{passed:checks.length,checks};
 }finally{cancelLaunch();audioEngine.load=load;audioEngine.playCue=play;unlockAudio=unlock;toast=originalToast;Object.assign(prefs,originalPrefs);window.YoruRefresh();}
})()
