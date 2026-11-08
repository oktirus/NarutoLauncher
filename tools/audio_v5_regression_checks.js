(async()=>{
 const results=[],check=(name,ok,detail='')=>{results.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+detail);},wait=ms=>new Promise(r=>setTimeout(r,ms));
 if(!audioReady())throw Error('A real activation gesture is required first.');if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=true;prefs.ambience=false;ambience();audioEngine.stopCues('test-isolation');await wait(30);
 const rows=await fetch('../revisions/v5/audio-manifest.json').then(r=>r.json());
 for(const row of rows){const buffer=await audioEngine.load(KonohaCueSources[row.technique][row.event].url);check(row.technique+' '+row.event+' prepared duration matches decoded buffer',Math.abs(buffer.duration-row.duration)<.01);}
 setTechnique('fireball');audioEngine.events=[];
 const cold=KonohaCueSources.fireball.confirm.url;audioEngine.cache.delete(cold);
 const batch=await Promise.all(Array.from({length:6},()=>audioEngine.playCue('fireball','confirm')));
 check('six simultaneous cold-load confirms start one voice',batch.filter(Boolean).length===1&&audioEngine.snapshot().cues.filter(v=>v.cue==='fireball'&&v.type==='confirm').length===1);
 const repeat=await audioEngine.playCue('fireball','confirm');check('active confirm is preserved without layering or restarting',repeat===false&&audioEngine.events.filter(e=>e.event==='cue-started').length===1);
 await audioEngine.playCue('fireball','focus');check('readiness can coexist with activation',audioEngine.snapshot().cues.some(v=>v.type==='focus')&&audioEngine.snapshot().cues.some(v=>v.type==='confirm'));
 await wait(4700);check('both channels end naturally',audioEngine.events.filter(e=>e.event==='cue-ended').length===2&&!audioEngine.events.some(e=>e.event==='cue-stopped'));
 // A canceled old decode must not erase the pending ticket of a new request.
 const originalLoad=audioEngine.load,buffer=await originalLoad.call(audioEngine,KonohaCueSources.gamabunta.confirm.url),gates=[];
 audioEngine.load=()=>new Promise(resolve=>gates.push(resolve));
 const old=audioEngine.playCue('gamabunta','confirm');audioEngine.stopCues('test-cancel-pending');const fresh=audioEngine.playCue('gamabunta','confirm');gates[0](buffer);await old;
 check('stale decode cleanup preserves the newer pending confirm',audioEngine.pendingCues.size===1);
 check('another request cannot bypass a newer pending confirm',await audioEngine.playCue('gamabunta','confirm')===false);
 gates[1](buffer);await fresh;audioEngine.load=originalLoad;audioEngine.stopCues('test-isolation');await wait(30);
 audioEngine.events=[];document.querySelector('[data-action="settings"]').click();await wait(40);
 const toggle=document.querySelector('[data-pref="motion"]');for(let k=0;k<3;k++){toggle.click();await wait(30);}if(!prefs.motion)toggle.click();
 check('menus and toggles never speak the selected technique',audioEngine.events.filter(e=>e.event==='cue-started').every(e=>e.cue==='ui'));
 check('service feedback uses the separate neutral resource',audioEngine.events.some(e=>e.event==='cue-started'&&e.url===KonohaCueSources.ui.confirm.url));closePanel();
 audioEngine.stopCues('test-isolation');await wait(25);audioEngine.events=[];setScene('gaara',true,false);enterAmbient(false);await wait(50);
 check('automatic scene and idle transitions stay silent',!audioEngine.events.some(e=>e.event==='cue-started'));leaveAmbient();
 const savedInterval=window.setInterval,savedTimeout=window.setTimeout;let rotation,idle;
 window.setInterval=(fn,delay,...args)=>{if(delay===60000){rotation=fn;return -101;}return savedInterval(fn,delay,...args);};
 window.setTimeout=(fn,delay,...args)=>{if(delay===300000){idle=fn;return -102;}return savedTimeout(fn,delay,...args);};
 prefs.rotation=60;prefs.idle=300;applyTimers();window.setInterval=savedInterval;window.setTimeout=savedTimeout;audioEngine.events=[];const scene=prefs.scene;rotation();await wait(40);idle();await wait(40);
 check('actual rotation callback changes scene without starting sound',prefs.scene!==scene&&!audioEngine.events.some(e=>e.event==='cue-started'));
 check('actual idle callback enters quiet mode without sound',ambient&&!audioEngine.events.some(e=>e.event==='cue-started'));leaveAmbient();prefs.rotation=0;prefs.idle=0;applyTimers();
 check('Gamabunta and plain summon have different rendered activations',KonohaCueSources.gamabunta.confirm.url!==KonohaCueSources.summoning.confirm.url&&rows.find(r=>r.technique==='gamabunta'&&r.event==='confirm').sha256!==rows.find(r=>r.technique==='summoning'&&r.event==='confirm').sha256);
 check('neutral feedback differs from summon readiness',rows.find(r=>r.technique==='ui').sha256!==rows.find(r=>r.technique==='summoning'&&r.event==='focus').sha256);
 prefs.ambience=true;ambience();for(let k=0;k<100&&audioEngine.snapshot().musicNodes.length<2;k++)await wait(20);
 const music=audioEngine.snapshot();check('music still schedules overlapping nodes after activation changes',music.musicNodes.length>=2&&Math.abs(music.musicNodes[0].end-music.musicNodes[1].start-3.5)<.001);
 prefs.musicVolume=.18;ambience();check('changing volume does not restart the music',audioEngine.snapshot().musicNodes[0].start===music.musicNodes[0].start);
 await audioEngine.playCue('gamabunta','focus');await audioEngine.playCue('gamabunta','confirm');YoruPause();await wait(40);check('pause stops every cue after the routing changes',audioEngine.snapshot().cues.length===0&&audioEngine.snapshot().musicNodes.length===0);YoruRefresh();
 prefs.audio=false;ambience();setTechnique('rasengan');setScene('dusk',true,false);document.querySelector('#dock .app-card').focus();
 return {passed:results.length,total:results.length,results};
})()
