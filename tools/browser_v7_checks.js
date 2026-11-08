(async()=>{
 const checks=[],check=(name,ok,detail='')=>{checks.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+detail);},wait=ms=>new Promise(r=>setTimeout(r,ms));
 const originalPrefs={...prefs,pins:[...prefs.pins]};if(modal)closePanel();if(ambient)leaveAmbient();await techniques.ready;
 const ids=['rasengan','sharingan','kurama','fireball','amaterasu'];
 check('five complete paintings registered',ids.every(id=>techniques.completePaintings.includes(id)));
 for(const id of ids){check(id+' uses new painted source',KonohaEffectSources[id].includes('/effects/v7/'));const image=new Image();image.src=KonohaEffectSources[id];await image.decode();check(id+' decodes complete image',image.naturalWidth===768);const surface=document.createElement('canvas');surface.width=surface.height=400;const c=surface.getContext('2d');techniques.draw(c,id,200,200,90,0);const data=c.getImageData(155,155,90,90).data;const colors=new Set();let opaque=0;for(let i=0;i<data.length;i+=4){if(data[i+3]>160){opaque++;colors.add([data[i]>>3,data[i+1]>>3,data[i+2]>>3].join(','));}}check(id+' middle is painted with real internal detail',opaque>3500&&colors.size>15,JSON.stringify({opaque,colors:colors.size}));}
 check('new focus label',highlightSounds[0].name==='Завершення печатки');check('new final activation source',KonohaCueSources.jutsu.focus.url.includes('v7-seal-complete'));
 openPanel('settings');check('new label appears in settings',document.querySelector('#highlight-sound').options[0].textContent==='Завершення печатки');closePanel();
 if(!audioReady())throw Error('Activate audio with a real gesture first.');
 const buffer=await audioEngine.load(KonohaCueSources.jutsu.focus.url);check('final activation is under .7s',buffer.duration>.6&&buffer.duration<.7);
 const play=audioEngine.playCue,played=[];audioEngine.playCue=async(cue,type)=>{played.push({cue,type});return true;};prefs.audio=true;
 try{for(const choice of highlightSounds)for(const t of techniques.list){prefs.highlightSound=choice.id;setTechnique(t.id,false);played.length=0;await sound('focus');await sound('confirm');check(choice.id+' / '+t.id+' routing preserved',played.length===2&&played[0].cue===choice.id&&played[0].type==='focus'&&played[1].cue===t.cue&&played[1].type==='confirm');}}
 finally{audioEngine.playCue=play;}
 for(const hero of ['naruto','gamabunta']){
  const art=KonohaRunnerSources[hero],image=new Image();image.src=art.url;await image.decode();check(hero+' four-by-two motion sheet decodes',image.naturalWidth===2048&&image.naturalHeight===1024&&art.columns===4);
  const states=[];for(let frame=0;frame<4;frame++){const rate=hero==='gamabunta'?6.4:11;const pose=KonohaRunner.poseFor({hero,status:'running',speed:360,elapsed:(frame+.1)/rate,y:0,duck:0});states.push(pose.frame);}check(hero+' has four locomotion phases',states.join(',')==='0,1,2,3');
  check(hero+' has separate jump pose',KonohaRunner.poseFor({hero,status:'running',speed:360,elapsed:0,y:80,duck:0}).frame===4);
  check(hero+' has separate idle pose',KonohaRunner.poseFor({hero,status:'ready',speed:360,elapsed:0,y:0,duck:0}).frame===5);
  const duck=[0,.15].map(elapsed=>KonohaRunner.poseFor({hero,status:'running',speed:360,elapsed,y:0,duck:.5}).frame);check(hero+' has two drawn crouch poses',duck.join(',')==='6,7');
 }
 openPanel('game');await wait(60);document.querySelector('[data-run-hero="naruto"]').click();KonohaRemote('Enter');const phases=new Set();for(let k=0;k<9;k++){await wait(55);phases.add(KonohaRunner.snapshot().pose.frame);}check('real Naruto runner plays all run frames',phases.size===4,[...phases].join(','));KonohaRemote('ArrowDown');await wait(25);check('real Naruto duck selects drawn pose',KonohaRunner.snapshot().pose.mode==='duck'&&KonohaRunner.snapshot().pose.frame>=6);KonohaRemote('ContextMenu');const frozen=KonohaRunner.snapshot();await wait(60);check('pause preserves pose and time',KonohaRunner.snapshot().elapsed===frozen.elapsed&&KonohaRunner.snapshot().pose.frame===frozen.pose.frame);KonohaRemote('Escape');
 openPanel('game');await wait(40);document.querySelector('[data-run-hero="gamabunta"]').click();KonohaRemote('Enter');const frogPhases=new Set();for(let k=0;k<14;k++){await wait(55);frogPhases.add(KonohaRunner.snapshot().pose.frame);}check('real Gamabunta plays all four hop frames',frogPhases.size===4,[...frogPhases].join(','));KonohaRemote('ArrowDown');await wait(30);check('real Gamabunta duck uses separate pose',KonohaRunner.snapshot().pose.mode==='duck');KonohaRemote('ArrowUp');await wait(110);check('real Gamabunta jump uses airborne pose',KonohaRunner.snapshot().pose.frame===4&&KonohaRunner.snapshot().y>20);KonohaRemote('Escape');
 check('closing game disposes animation',!KonohaRunner.snapshot());
 const source=await fetch('runner.js?v=10').then(r=>r.text());check('no transparent tree generator',!source.includes('function tree(')&&!source.includes('tree((('));check('no squeezed duck transform',!source.includes('scale(1.2,.55)'));check('runner no longer uses static frog fallback',!source.includes('frogIdle')&&!source.includes('frogJump'));
 Object.assign(prefs,originalPrefs);persist();setTechnique(prefs.technique,false);applyMotion();ambience();document.querySelector('#dock .app-card').focus();
 return {passed:checks.length,checks,viewport:[innerWidth,innerHeight]};
})()
