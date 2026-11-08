(async()=>{
 const results=[],check=(name,ok,detail='')=>{results.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+detail);},wait=ms=>new Promise(r=>setTimeout(r,ms));
 if(modal)closePanel();if(ambient)leaveAmbient();await techniques.ready;
 setTechnique('gamabunta');openPanel('techniques');await wait(30);
 check('frog preview belongs to Gamabunta even when body has the same data attribute',document.querySelector('.frog-preview')?.closest('.technique-card').dataset.technique==='gamabunta');
 check('Rasengan retains its centered single app preview',document.querySelectorAll('.technique-card[data-technique="rasengan"] img').length===1);
 check('Gamabunta displays two application targets',document.querySelectorAll('.technique-card[data-technique="gamabunta"] img').length===2);closePanel();
 setScene('gaara');await decodeScene(scenes.find(s=>s.id==='gaara'));
 check('Gaara brand subtitle matches the Sand Village',document.querySelector('.brand small').textContent==='Село, приховане в піску');
 check('Gaara hero copy matches the brand',document.querySelector('.hero-kicker').textContent===document.querySelector('.brand small').textContent);
 openPanel('scenes');check('wallpaper gallery has a village-neutral title',document.querySelector('#panel-title').textContent==='Шпалери');closePanel();
 setScene('dusk');check('returning to Konoha restores the Leaf Village subtitle',document.querySelector('.brand small').textContent==='Село, приховане в листі');
 check('hero gallery label includes all Naruto characters',document.querySelector('.hero-wallpapers').textContent==='Герої Naruto');
 const first=document.querySelector('#dock .app-card');document.querySelector('.brand').focus();first.focus();const style=getComputedStyle(first.querySelector('.app-name'));
 check('focused label has a dark backplate',style.backgroundColor!=='rgba(0, 0, 0, 0)'&&style.boxShadow!=='none');
 const now=performance.now(),a={x:100,y:300,r:50},b={x:300,y:300,r:50};techniques.resetMotion();techniques.move(null,a,now);techniques.move(a,b,now,1);
 const original=techniques.motionSnapshot(now+160);techniques.reanchor(b,{...b,x:b.x-70});const shifted=techniques.motionSnapshot(now+160);
 check('scroll shifts coordinates without restarting the airborne arc',Math.abs(original.u-shifted.u)<.00001&&Math.abs(shifted.x-original.x+70)<.01&&Math.abs(shifted.y-original.y)<.01);
 check('scroll preserves the intended direction',shifted.direction===1&&shifted.spriteFlip===-1);
 setTechnique('gamabunta');first.focus();updateFocus(first);KonohaRemote('ArrowRight');const before=techniques.position();
 const dock=document.querySelector('#dock');const oldScroll=dock.scrollLeft;dock.scrollLeft=oldScroll+20;dock.dispatchEvent(new Event('scroll'));const after=techniques.position();
 check('same-card scroll does not reset progress',after.u>=before.u&&after.direction===before.direction);dock.scrollLeft=oldScroll;dock.dispatchEvent(new Event('scroll'));
 if(innerWidth<=900){
  const cards=[...document.querySelectorAll('#dock .app-card')];cards[3].focus({preventScroll:true});cards[3].scrollIntoView({block:'nearest',inline:'center',behavior:'instant'});await wait(60);updateFocus(cards[3]);
  KonohaRemote('ArrowRight');const states=[];for(let k=0;k<24;k++){await wait(16);const p=techniques.position();states.push({u:p.u,direction:p.direction,x:p.x});}
  check('mobile rightward jump keeps its facing throughout smooth dock scroll',states.every(s=>s.direction===1),JSON.stringify(states));
  check('mobile scroll does not restart a jump every frame',states.every((s,i)=>!i||s.u+0.0001>=states[i-1].u),JSON.stringify(states));
  KonohaRemote('ArrowLeft');const reverse=[];for(let k=0;k<24;k++){await wait(16);const p=techniques.position();reverse.push({u:p.u,direction:p.direction});}
  check('mobile leftward jump keeps its facing throughout smooth dock scroll',reverse.every(s=>s.direction===-1),JSON.stringify(reverse));
  check('leftward scrolling preserves continuous jump progress',reverse.every((s,i)=>!i||s.u+0.0001>=reverse[i-1].u),JSON.stringify(reverse));
 }
 setTechnique('rasengan');setScene('dusk');document.querySelector('#dock .app-card').focus();
 return {viewport:{width:innerWidth,height:innerHeight},passed:results.length,total:results.length,results};
})()
