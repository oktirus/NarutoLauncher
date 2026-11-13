(async()=>{
 const checks=[],wait=ms=>new Promise(r=>setTimeout(r,ms)),check=(name,ok)=>{checks.push({name,passed:!!ok});if(!ok)throw Error(name);};
 const saved={...prefs},storage=localStorage.getItem('konoha.preferences.v1');
 try{
  if(modal)closePanel();prefs.audio=true;prefs.ambience=false;prefs.volume=0;prefs.musicVolume=0;await unlockAudio();
  check('audio running after unlock',audioReady());
  prefs.technique='gamabunta';setTechnique('gamabunta',false);await techniques.ready;
  check('only active frog images retained',Object.keys(techniques.artStatus()).sort().join(',')==='gamabunta-idle,gamabunta-jump');
  check('at most two scene decodes',decodedScenes.size<=2);
  openPanel('techniques');await wait(600);
  check('gallery has all ten static previews',$$('[data-preview]').length===10);
  check('home render loop stopped under gallery',frame===0);
  const pixels=$$('[data-preview]').map(c=>c.toDataURL());await wait(700);
  check('every technique preview remains pixel identical',$$('[data-preview]').every((c,i)=>c.toDataURL()===pixels[i]));
  const ids=$$('[data-preview]').map(c=>c.dataset.preview);
  check('no raster Chidori downloaded',!('chidori' in techniques.artStatus()));
  for(const id of ids){const c=$(`[data-preview="${id}"]`),data=c.getContext('2d').getImageData(0,0,320,210).data;check(id+' preview has visible pixels',data.some((v,i)=>i%4===3&&v>0));}
  closePanel();await wait(50);
  check('closed gallery content removed',$('#panel-content').childElementCount===0&&$$('[data-preview]').length===0);
  check('unused effect images released',Object.keys(techniques.artStatus()).length===2);
  openPanel('scenes');check('all 18 wallpaper choices present',$$('.scene-card').length===18);
  check('wallpaper cards use lazy thumbnails',$$('.scene-card img').every(i=>i.loading==='lazy'&&i.src.includes('/v26/thumb-')));closePanel();
  for(let i=0;i<3;i++){openPanel('game');await wait(200);let snap=KonohaRunner.snapshot();check('game loads only selected hero '+i,snap.images.length===3&&snap.cell===256);const surface=$('#runner-canvas');closePanel();check('game canvas freed '+i,surface.width===0&&surface.height===0&&KonohaRunner.snapshot()===null&&$('#panel-content').childElementCount===0);}
  openPanel('game');await wait(100);const hero=KonohaRunner.snapshot().hero;KonohaRunner.handleKey('ArrowRight');await wait(150);check('hero switch replaces atlases',KonohaRunner.snapshot().hero!==hero&&KonohaRunner.snapshot().images.length===3);
  KonohaRunner.handleKey('Enter');KonohaRunner.handleKey('ArrowDown');check('slide still uses short window',KonohaRunner.snapshot().duck>0&&KonohaRunner.snapshot().pose.mode==='slide');
  window.YoruPause();await wait(100);check('pause releases fullscreen and game canvases',canvas.width===0&&techniqueCanvas.width===0&&$('#runner-canvas').width===0);
  check('pause releases game images',KonohaRunner.snapshot().images.length===0);
  check('pause clears audio and suspends context',audioEngine.cache.size===0&&audioEngine.context.state==='suspended');
  window.YoruRefresh();await wait(200);check('resume restores surfaces and game without restarting run',canvas.width>0&&$('#runner-canvas').width===1280&&KonohaRunner.snapshot().status==='paused'&&KonohaRunner.snapshot().images.length===3);closePanel();
  await audioEngine.configure({audio:true,ambience:true,paused:false,volume:0,musicVolume:0,music:'1584791975'});
  await audioEngine.configure({music:'1584792236'});await audioEngine.configure({music:'1584792240'});
  check('only active music buffer retained',[...audioEngine.cache.keys()].filter(k=>k.includes('.m4a')).length===1);
  check('crossfade and scheduled loop intact',audioEngine.fade===3.5&&audioEngine.musicNodes.size>=2&&audioEngine.musicNodes.size<=3);
  prefs.ambience=false;await audioEngine.configure({ambience:false});check('music off drops PCM',![...audioEngine.cache.keys()].some(k=>k.includes('.m4a')));
  for(const cue of ['rasengan','chidori','fireball','kurama','amaterasu','rasenshuriken'])await audioEngine.load(KonohaCueSources[cue].confirm.url);
  check('FX cache bounded (plus pinned cues)',[...audioEngine.cache.keys()].filter(k=>!audioEngine.pinned.has(k)).length<=4);
  check('render loop runs again after close',frame!==0);
  await wait(1900);check('old backdrop discarded after crossfade',getComputedStyle([$('#back-a'),$('#back-b')][1-backLayer]).backgroundImage==='none');
  check('original illustrations preserved in runtime map',Object.values(runtimeAssets.scenes).every(s=>!s.full.includes('/v26/')&&s.small.includes('/v26/')));
  return {passed:checks.length,checks};
 }finally{if(modal)closePanel();Object.assign(prefs,saved);setTechnique(saved.technique,false);setScene(saved.scene,false,false);ambience();window.YoruRefresh();if(storage===null)localStorage.removeItem('konoha.preferences.v1');else localStorage.setItem('konoha.preferences.v1',storage);}
})()
