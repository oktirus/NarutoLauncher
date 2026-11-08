'use strict';
/* Original endless runner. Fixed-step physics is independent of rendering and refresh rate. */
window.KonohaRunner=(()=>{
 const W=1280,H=540,GROUND=440,KEY='konoha.runner.v1';
 const heroes={naruto:{name:'Наруто',title:'Ніколи не здаватися.',width:48,height:84},gamabunta:{name:'Гамабунта',title:'Один стрибок — ціла пригода.',width:80,height:65}};
 const overlap=(a,b)=>a.x<b.x+b.w&&a.x+a.w>b.x&&a.y<b.y+b.h&&a.y+a.h>b.y;
 function poseFor(run){
  const rate=(run.hero==='gamabunta'?6.4:11)*Math.sqrt(run.speed/360);
  if(run.y>1)return {frame:4,mode:'jump',hop:0};
  if(run.duck>0){const progress=Math.max(0,Math.min(1,1-run.duck/.72));return {frame:Math.min(3,Math.floor(progress*4)),mode:'slide',hop:0};}
  if(['running','paused'].includes(run.status)){const frame=Math.floor(run.elapsed*rate)%4;return {frame,mode:'run',hop:run.hero==='gamabunta'?[0,-3,-9,0][frame]:0};}
  return {frame:5,mode:'idle',hop:0};
 }
 class Run{
  constructor(hero='naruto',random=Math.random){this.random=random;this.hero=heroes[hero]?hero:'naruto';this.reset();}
  reset(){this.status='ready';this.distance=0;this.elapsed=0;this.y=0;this.vy=0;this.duck=0;this.speed=360;this.obstacles=[];this.next=1.8;this.sequence=0;this.cleared=0;}
  start(){this.reset();this.status='running';}
  jump(){if(this.status!=='running'||this.y>.5)return false;this.duck=0;this.vy=-810;this.y=.01;return true;}
  crouch(){if(this.status!=='running')return;if(this.y>0)this.vy=Math.max(400,this.vy);else this.duck=.72;}
  box(){const h=heroes[this.hero];return{x:185,y:GROUND-this.y-(this.duck>0?28:h.height),w:h.width,h:this.duck>0?28:h.height};}
  spawn(){const pick=this.random();const kind=this.distance>900&&pick>.67?'shuriken':pick<.5?'log':'stake';const w=kind==='shuriken'?42:kind==='log'?64:40,h=kind==='shuriken'?34:kind==='log'?48:74;this.obstacles.push({id:++this.sequence,kind,x:W+70,y:kind==='shuriken'?GROUND-96:GROUND-h,w,h,passed:false});}
  step(dt){
   if(this.status!=='running')return;
   for(let remaining=Math.min(Math.max(dt,0),.25);remaining>0&&this.status==='running';){const s=Math.min(remaining,1/120);remaining-=s;this.elapsed+=s;this.speed=Math.min(760,360+this.distance*.012);this.distance+=this.speed*s;this.duck=Math.max(0,this.duck-s);
    if(this.y>0||this.vy<0){this.vy+=2100*s;this.y-=this.vy*s;if(this.y<0){this.y=0;this.vy=0;}}
    this.next-=s;if(this.next<=0){this.spawn();this.next=1.28+this.random()*.72;}
    const player=this.box();for(const o of this.obstacles){o.x-=this.speed*s;if(!o.passed&&o.x+o.w<player.x){o.passed=true;this.cleared++;}if(overlap(player,{x:o.x+5,y:o.y+5,w:o.w-10,h:o.h-7})){this.status='over';break;}}
    this.obstacles=this.obstacles.filter(o=>o.x+o.w>-60);
   }
  }
  snapshot(){return {hero:this.hero,status:this.status,score:Math.floor(this.distance/10),distance:this.distance,speed:this.speed,y:this.y,vy:this.vy,duck:this.duck,cleared:this.cleared,obstacles:this.obstacles.map(o=>({...o}))};}
 }
 let session=null;
 function loadRecords(){try{const data=JSON.parse(localStorage.getItem(KEY)||'{}');return Object.fromEntries(Object.keys(heroes).map(id=>[id,Number.isFinite(data[id])&&data[id]>=0?Math.floor(data[id]):0]));}catch(_){return{naruto:0,gamabunta:0};}}
 function mount(root,options={}){
  unmount();const records=loadRecords();let hero='naruto';try{if(heroes[localStorage.getItem(KEY+'.hero')])hero=localStorage.getItem(KEY+'.hero');}catch(_){}
  const run=new Run(hero);const abort=new AbortController();
  root.innerHTML=`<div class="runner-toolbar"><div class="runner-characters" role="group" aria-label="Персонаж"><button data-run-hero="naruto">Наруто</button><button data-run-hero="gamabunta">Гамабунта</button></div><div class="runner-stats"><span>Шлях <strong id="run-score">00000</strong></span><span>Рекорд <strong id="run-best">00000</strong></span></div><button class="run-pause" data-run="pause" aria-label="Пауза">Ⅱ Пауза</button></div><div class="runner-stage"><canvas id="runner-canvas" width="1280" height="540" tabindex="0" aria-label="Шлях шинобі. OK або стрілка вгору — стрибок, вниз — підкат. Menu — пауза, Back — вихід."></canvas><div class="runner-message" id="run-message"><span id="run-subtitle">Тренування на світанку</span><h3 id="run-title">Ліс кличе.</h3><p id="run-description">Перестрибуй колоди й тренувальні стовпи.<br>Роби підкат під летючими сюрікенами.</p><button class="paper-button" data-run="start" id="run-start">OK · Рушай</button></div></div><div class="runner-bottom"><p id="run-status" role="status">Обери героя й починай тренування.</p><div class="runner-touch"><button data-run="duck">↓ Підкат</button><button data-run="jump">↑ Стрибок</button></div></div><p class="runner-help"><span><kbd>← →</kbd> герой до старту</span><span><kbd>OK / ↑ / Space</kbd> стрибок</span><span><kbd>↓</kbd> підкат</span><span><kbd>Menu / P</kbd> пауза</span><span><kbd>Back</kbd> вийти</span></p>`;
  const canvas=root.querySelector('#runner-canvas'),c=canvas.getContext('2d');let raf=0,last=0,previous='',lastScore=-1;
  const images={};const paths={background:'../assets/game/konoha-forest.webp',naruto:window.KonohaRunnerSources.naruto.url,narutoSlide:window.KonohaRunnerSources.naruto.slide.url,gamabunta:window.KonohaRunnerSources.gamabunta.url,gamabuntaSlide:window.KonohaRunnerSources.gamabunta.slide.url};
  for(const [name,url]of Object.entries(paths)){const img=new Image();images[name]=img;img.src=url;img.decode().then(()=>render()).catch(()=>{root.querySelector('#run-status').textContent='Частину малюнків не вдалося відкрити. Онови сторінку.';});}
  function best(){return records[run.hero]||0;}
  function setHero(id){if(!heroes[id]||!['ready','over'].includes(run.status))return;save();run.hero=id;run.reset();try{localStorage.setItem(KEY+'.hero',id);}catch(_){}sync();render();}
  function save(){const score=Math.floor(run.distance/10);if(score>best()){records[run.hero]=score;try{localStorage.setItem(KEY,JSON.stringify(records));}catch(_){root.querySelector('#run-status').textContent='Рекорд залишиться до закриття гри.';}}}
  function sync(){
   const state=run.status,score=Math.floor(run.distance/10);if(score!==lastScore){root.querySelector('#run-score').textContent=String(score).padStart(5,'0');lastScore=score;}
   root.querySelector('#run-best').textContent=String(Math.max(best(),score)).padStart(5,'0');
   root.querySelectorAll('[data-run-hero]').forEach(b=>{b.setAttribute('aria-pressed',String(b.dataset.runHero===run.hero));b.disabled=state==='running'||state==='paused';});
   root.querySelector('[data-run="pause"]').disabled=['ready','over'].includes(state);root.querySelector('[data-run="pause"]').textContent=state==='paused'?'▷ Продовжити':'Ⅱ Пауза';
   root.querySelector('[data-run="duck"]').textContent='↓ Підкат';
   if(state!==previous){
    previous=state;root.querySelector('#run-message').hidden=state==='running';
    root.querySelector('#run-subtitle').textContent=heroes[run.hero].name+' · '+heroes[run.hero].title;
    root.querySelector('#run-title').textContent=state==='over'?'Ще один шлях.':state==='paused'?'Перепочинок.':'Ліс кличе.';
    root.querySelector('#run-description').innerHTML=state==='over'?`Подолано ${score} м · Перешкод: ${run.cleared}.<br>Кожна спроба — нова історія.`:state==='paused'?'Час зупинився. Продовжуй, коли будеш готовий.':'Перестрибуй колоди й тренувальні стовпи.<br>Роби підкат під летючими сюрікенами.';
    root.querySelector('#run-start').textContent=state==='over'?'OK · Спробувати ще':state==='paused'?'OK · Продовжити':'OK · Рушай';
    root.querySelector('#run-status').textContent=state==='running'?'Стеж за дорогою. Не здавайся.':state==='over'?'Забіг завершено. Рекорд збережено локально.':state==='paused'?'Гра на паузі.':'Обери героя й починай тренування.';
   }
  }
  function go(){if(run.status==='paused')run.status='running';else run.start();previous='';last=performance.now();sync();canvas.focus({preventScroll:true});request();}
  function jump(){if(run.status!=='running'){go();return;}if(run.jump())options.onSound?.();}
  function pause(){if(run.status!=='running')return;run.status='paused';save();cancelAnimationFrame(raf);raf=0;sync();render();}
  function togglePause(){if(run.status==='paused')go();else pause();}
  function request(){if(!raf&&run.status==='running')raf=requestAnimationFrame(tick);}
  function tick(now){raf=0;const before=run.status;run.step((now-last)/1000);last=now;if(before==='running'&&run.status==='over'){save();options.onSound?.();}sync();render();request();}
  function render(){
   if(!session||session.run!==run)return;const t=run.elapsed,d=run.distance;c.clearRect(0,0,W,H);
   const bg=images.background;if(bg.complete&&bg.naturalWidth)c.drawImage(bg,0,0,W,H);else{const g=c.createLinearGradient(0,0,0,H);g.addColorStop(0,'#425957');g.addColorStop(.7,'#98a47c');g.addColorStop(1,'#29483c');c.fillStyle=g;c.fillRect(0,0,W,H);}
   c.fillStyle='#092f3438';c.fillRect(0,0,W,H);
   // The hand-painted forest stands on its own, without translucent tree overlays.
   c.fillStyle='#314939';c.beginPath();c.moveTo(0,GROUND-4);for(let x=0;x<=W;x+=16)c.lineTo(x,GROUND-4+Math.sin((x+d*.25)*.035)*3);c.lineTo(W,H);c.lineTo(0,H);c.fill();
   const road=c.createLinearGradient(0,GROUND,0,H);road.addColorStop(0,'#bd9a67');road.addColorStop(.13,'#977948');road.addColorStop(1,'#453e31');c.fillStyle=road;c.fillRect(0,GROUND,W,H-GROUND);c.fillStyle='#dac18c';c.fillRect(0,GROUND,W,3);
   for(let k=0;k<45;k++){const x=((k*91-d*1.02)%W+W)%W,y=GROUND+15+k%4*21;c.fillStyle=k%3?'#d1af6f55':'#342d2588';c.fillRect(x,y,12+k%7*2,2);}
   for(const o of run.obstacles)obstacle(o,t);
   const duck=run.duck>0,air=run.y>1,gama=run.hero==='gamabunta',pose=poseFor(run),slide=pose.mode==='slide',art=slide?window.KonohaRunnerSources[run.hero].slide:window.KonohaRunnerSources[run.hero],size=slide?art.drawSize:gama?142:116;
   const bob=['running','paused'].includes(run.status)&&!air&&!duck&&!options.reduced?.()?(gama?pose.hop:Math.sin(t*22)*1.2):0;
   c.save();c.globalAlpha=.27;c.fillStyle='#092523';c.beginPath();c.ellipse(211,GROUND+3,(gama?58:35)*(1-run.y/400),9,0,0,Math.PI*2);c.fill();c.restore();
   c.save();c.translate(gama?222:207,GROUND-run.y+bob);
   const img=images[slide?run.hero+'Slide':run.hero];
   if(img.complete&&img.naturalWidth){const frame=pose.frame,cell=art.cell;c.drawImage(img,frame%art.columns*cell,Math.floor(frame/art.columns)*cell,cell,cell,-size/2,-size*art.baseline/cell,size,size);}
   c.restore();
   if(run.status==='running'&&!air&&!options.reduced?.()){for(let k=0;k<7;k++){const age=(t*2+k*.14)%1;c.globalAlpha=(1-age)*.3;c.fillStyle='#ddcba2';c.beginPath();c.ellipse(169-age*60,GROUND-3-age*12,3+age*8,2+age*4,0,0,Math.PI*2);c.fill();}c.globalAlpha=1;}
   if(slide&&!options.reduced?.()){c.save();c.lineWidth=1.3;for(let k=0;k<6;k++){const age=(t*3+k*.17)%1;c.globalAlpha=(1-age)*Math.min(1,run.duck/.12)*.45;c.strokeStyle=k%2?'#d4c198':'#8c734e';c.beginPath();c.moveTo(174-age*62,GROUND-2-k%3*2);c.lineTo(150-age*90,GROUND-k%3*2);c.stroke();}c.restore();}
   // Unmoving boundary foliage frames the lane, never obscuring a collision box.
   c.fillStyle='#092c28';for(let x=0;x<W;x+=37){c.beginPath();c.ellipse(x,H+5,40,16+(x%4)*3,-.4,0,Math.PI*2);c.fill();}
  }
  function obstacle(o,t){c.save();c.translate(o.x,o.y);c.lineWidth=2;c.strokeStyle='#3b3027';
   if(o.kind==='shuriken'){c.translate(o.w/2,o.h/2);c.rotate(t*8);c.fillStyle='#bfd0cf';c.beginPath();for(let k=0;k<8;k++){const a=k*Math.PI/4,r=k%2?5:Math.min(o.w,o.h)/2-1;k?c.lineTo(Math.cos(a)*r,Math.sin(a)*r):c.moveTo(Math.cos(a)*r,Math.sin(a)*r);}c.closePath();c.fill();c.stroke();c.fillStyle='#28443d';c.beginPath();c.arc(0,0,3,0,Math.PI*2);c.fill();}
   else if(o.kind==='log'){c.fillStyle='#785a37';c.fillRect(0,3,o.w,o.h-3);c.strokeRect(0,3,o.w,o.h-3);c.fillStyle='#cbab70';c.beginPath();c.ellipse(o.w-3,o.h/2,12,o.h/2,0,0,Math.PI*2);c.fill();c.stroke();for(const r of[5,10,16]){c.beginPath();c.ellipse(o.w-3,o.h/2,r*.45,r,0,0,Math.PI*2);c.stroke();}c.strokeStyle='#a68850';for(let k=1;k<4;k++){c.beginPath();c.moveTo(3,k*11);c.lineTo(o.w-13,k*11+2);c.stroke();}}
   else{c.fillStyle='#a98a54';c.fillRect(6,0,o.w-12,o.h);c.strokeRect(6,0,o.w-12,o.h);c.fillStyle='#dfc58d';c.fillRect(0,12,o.w,9);c.fillRect(1,45,o.w-2,9);c.fillStyle='#8c4030';c.fillRect(7,27,o.w-14,8);c.strokeStyle='#5d4b30';c.beginPath();c.arc(o.w/2,17,9,0,Math.PI*2);c.stroke();}
   c.restore();}
  function handleKey(key){
   if(key==='Escape'){save();options.onExit?.();return true;}
   if(key==='ContextMenu'||key.toLowerCase()==='p'){togglePause();return true;}
   if(key==='ArrowLeft'||key==='ArrowRight'){setHero(run.hero==='naruto'?'gamabunta':'naruto');return true;}
   if(['Enter','ArrowUp',' '].includes(key)){jump();return true;}
   if(key==='ArrowDown'){run.crouch();render();return true;}return false;
  }
  root.addEventListener('click',e=>{const button=e.target.closest('[data-run],[data-run-hero]');if(!button)return;if(button.dataset.runHero)setHero(button.dataset.runHero);else if(button.dataset.run==='start')go();else if(button.dataset.run==='jump')jump();else if(button.dataset.run==='duck')run.crouch();else togglePause();render();},{signal:abort.signal});
  canvas.addEventListener('pointerdown',e=>{if(e.button===0){e.preventDefault();jump();}},{signal:abort.signal});
  session={run,abort,pause,handleKey,render,dispose(){save();cancelAnimationFrame(raf);abort.abort();},snapshot:()=>({...run.snapshot(),pose:poseFor(run),records:{...records},animating:!!raf})};
  sync();render();requestAnimationFrame(()=>{if(session?.run===run)canvas.focus({preventScroll:true});});
 }
 function unmount(){session?.dispose();session=null;}
 return {Run,heroes,overlap,poseFor,mount,unmount,pause:()=>session?.pause(),handleKey:key=>session?.handleKey(key)||false,snapshot:()=>session?.snapshot()||null};
})();
