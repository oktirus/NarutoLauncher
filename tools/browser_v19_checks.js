(async()=>{
 const $=s=>document.querySelector(s),results=[],check=(name,ok,detail='')=>{results.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+JSON.stringify(detail));},wait=async()=>{const panel=$('.scroll-panel');panel.getBoundingClientRect();await Promise.all(panel.getAnimations().filter(a=>Number.isFinite(a.effect.getTiming().iterations)).map(a=>a.finished.catch(()=>{})));await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));};
 const keys=['konoha.preferences.v1','konoha.runner.v1','konoha.runner.v1.hero'],saved=keys.map(k=>localStorage.getItem(k));
 try{
  if(modal)closePanel();prefs.audio=false;prefs.ambience=false;prefs.rotation=0;prefs.idle=0;applyTimers();
  check('search removed',!$('.top-status [data-action="search"]'));check('badge removed',!$('.shinobi-badge'));check('game and settings remain',$('.game-shortcut')&&$('.top-status [data-action="settings"]'));
  $('.game-shortcut').focus();$('.game-shortcut').click();await wait();
  const panel=$('.game-panel'),stage=$('.runner-stage'),r=panel.getBoundingClientRect(),s=stage.getBoundingClientRect(),css=getComputedStyle(panel);
  check('game panel fills viewport',Math.abs(r.left)<1&&Math.abs(r.width-innerWidth)<1,{left:r.left,width:r.width,viewport:innerWidth});
  check('stage fills content width',Math.abs(s.width-(panel.clientWidth-parseFloat(css.paddingLeft)-parseFloat(css.paddingRight)))<1,{stage:s.width,content:panel.clientWidth});
  check('canvas logical dimensions preserved',$('#runner-canvas').width===1280&&$('#runner-canvas').height===540);
  check('no horizontal overflow',panel.scrollWidth<=panel.clientWidth&&document.documentElement.scrollWidth<=innerWidth);
  check('close control visible',$('.game-panel [data-action="close"]').getBoundingClientRect().right<=innerWidth);
  for(const hero of ['naruto','gamabunta']){
   if(KonohaRunner.snapshot().status!=='ready'){closePanel();$('.game-shortcut').focus();$('.game-shortcut').click();await wait();}
   $('[data-run-hero="'+hero+'"]').click();$('#run-start').click();check(hero+' starts',KonohaRunner.snapshot().status==='running');
   KonohaRemote('ArrowDown');check(hero+' slides',KonohaRunner.snapshot().duck>0);
   KonohaRemote('ArrowUp');check(hero+' jumps',KonohaRunner.snapshot().vy<0);
   KonohaRemote('p');check(hero+' pauses',KonohaRunner.snapshot().status==='paused');KonohaRemote('p');check(hero+' resumes',KonohaRunner.snapshot().status==='running');
  }
  KonohaRemote('Escape');check('Back exits and unmounts',!modal&&KonohaRunner.snapshot()===null);check('focus returns to game icon',document.activeElement===$('.game-shortcut'));
  $('.top-status [data-action="settings"]').click();check('settings stays ordinary side panel',$('.scroll-panel').getBoundingClientRect().width<innerWidth||innerWidth<700);closePanel();
  return{viewport:[innerWidth,innerHeight],passed:results.length,results};
 }finally{if(modal)closePanel();keys.forEach((k,i)=>saved[i]===null?localStorage.removeItem(k):localStorage.setItem(k,saved[i]));}
})()
