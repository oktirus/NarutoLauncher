(async()=>{
 const checks=[],check=(name,ok,detail='')=>{checks.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+detail);},wait=ms=>new Promise(r=>setTimeout(r,ms));
 const before={...prefs,pins:[...prefs.pins]};if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=false;ambience();
 check('twelve scenes remain',scenes.length===12);
 for(const id of ['hinata','shikamaru','rocklee','neji','choji']){
  const scene=scenes.find(s=>s.id===id);check(id+' uses regenerated v9 image',scene.image.startsWith('v9/'));await decodeScene(scene);setScene(id);await wait(25);
  check(id+' title and stored choice',document.querySelector('#hero-title').innerHTML===scene.title&&JSON.parse(localStorage.getItem('konoha.preferences.v1')).scene===id);
  const r=document.querySelector('#hero-title').getBoundingClientRect();check(id+' text fits',r.left>=0&&r.right<=innerWidth+1);
 }
 openPanel('scenes');await wait(40);const cards=[...document.querySelectorAll('.scene-card img')];await Promise.all(cards.map(i=>i.decode()));check('all twelve gallery images decode',cards.length===12&&cards.every(i=>i.naturalWidth));closePanel();
 for(const hero of ['naruto','gamabunta']){
  const slide=KonohaRunnerSources[hero].slide,img=new Image();img.src=slide.url;await img.decode();check(hero+' four-frame atlas loads',img.naturalWidth===1024&&img.naturalHeight===1024&&slide.columns===2&&slide.rows===2);
  check(hero+' correct retained or new resource',slide.url.includes(hero==='naruto'?'/game/v8/':'/game/v9/'));
  for(const [time,frame]of[[.01,0],[.23,1],[.41,2],[.63,3]]){const p=KonohaRunner.poseFor({hero,status:'running',speed:360,elapsed:8,y:0,duck:.72-time});check(hero+' slide phase '+frame,p.mode==='slide'&&p.frame===frame);}
  openPanel('game');await wait(70);document.querySelector('[data-run-hero='+hero+']').click();check(hero+' UI labels slide',document.querySelector('[data-run=duck]').textContent.includes('Підкат'));
  document.querySelector('#runner-canvas').focus();KonohaRemote('Enter');KonohaRemote('ArrowDown');let shot=KonohaRunner.snapshot();check(hero+' live slide starts',shot.pose.mode==='slide'&&shot.duck>0&&shot.duck<=.72);const phases=new Set([shot.pose.frame]);for(let k=0;k<4;k++){await wait(165);shot=KonohaRunner.snapshot();if(shot.pose.mode==='slide')phases.add(shot.pose.frame);}check(hero+' live four phase sequence',phases.size===4,[...phases].join(','));
  await wait(120);check(hero+' expires into run',KonohaRunner.snapshot().duck===0&&KonohaRunner.snapshot().pose.mode==='run');
  KonohaRemote('ArrowDown');await wait(80);KonohaRemote('ContextMenu');const stopped=KonohaRunner.snapshot();await wait(100);check(hero+' pause holds slide',KonohaRunner.snapshot().duck===stopped.duck&&KonohaRunner.snapshot().pose.frame===stopped.pose.frame);KonohaRemote('Escape');
 }
 check('game disposed',KonohaRunner.snapshot()===null);
 Object.assign(prefs,before);persist();setScene(prefs.scene,false);setTechnique(prefs.technique,false);applyMotion();ambience();
 return {passed:checks.length,checks,viewport:[innerWidth,innerHeight]};
})()
