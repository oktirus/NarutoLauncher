'use strict';
/* Reference-led painted plates with distinct motion and a traveling Gamabunta. */
(()=>{
 const api=window.KonohaTechniques,baseDraw=api.draw,tau=Math.PI*2,images={},loaded={};
 const completePaintings=new Set(['rasengan','sharingan','kurama','fireball','amaterasu']);
 api.proceduralEffects=['chidori'];
 api.completePaintings=[...completePaintings];
 const ids=api.list.map(t=>t.id);
 api.list.push({id:'gamabunta',name:'Стрибок Гамабунти',owner:'Володар жаб',color:'#e7b789',cue:'gamabunta',description:'Гамабунта стрибає вперед і здіймає пил при приземленні.'});
 const pending=new Map(),variants=window.KonohaRuntimeAssets.effects;let active='rasengan';
 const names=id=>id==='chidori'?[]:id==='gamabunta'?['gamabunta-idle','gamabunta-jump']:[id];
 function release(name){const image=images[name];if(!image)return;const cached=displaced.get(image);if(cached){cached.surface.width=cached.surface.height=0;displaced.delete(image);}image.removeAttribute('src');delete images[name];delete loaded[name];pending.delete(name);}
 function load(name,pixels){
  const url=pixels<=384?variants[name].small:variants[name].full;
  if(images[name]?.getAttribute('src')===url)return pending.get(name)||Promise.resolve();
  release(name);const image=new Image();images[name]=image;image.src=url;
  const promise=image.decode().then(()=>{if(images[name]!==image)return;loaded[name]=true;dispatchEvent(new Event('konoha-art-ready'));}).catch(()=>{if(images[name]===image)loaded[name]=false;});pending.set(name,promise);return promise;
 }
 api.ensure=(id,pixels=384)=>Promise.all(names(id).map(name=>load(name,pixels)));
 api.trim=()=>{for(const name of Object.keys(images))if(!names(active).includes(name))release(name);};
 api.releaseAll=()=>{for(const name of Object.keys(images))release(name);};
 api.setActive=id=>{active=id;api.trim();api.ready=api.ensure(id,Math.min(181,innerWidth*.106)*1.6*Math.min(devicePixelRatio||1,2));return api.ready;};
 api.ready=Promise.resolve();
 api.artStatus=()=>({...loaded});
 let jump=null,anchor=null,landings=[];
 // Painted jump faces left; seated pose faces right. They need opposite transforms.
 const spriteFlip=(direction,airborne=false)=>airborne?-direction:direction;
 const perch=p=>({x:p.x+p.r*.5,y:p.y-p.r*.26,r:p.r});
 function position(now){
  if(!jump)return anchor;
  const u=Math.max(0,Math.min(1,(now-jump.started)/jump.duration)),ease=u*u*(3-2*u);
  if(u===1&&!jump.emitted){jump.emitted=true;landings.push({...jump.to,started:jump.started+jump.duration});if(landings.length>8)landings.shift();}
  const height=Math.min(128,Math.max(42,Math.hypot(jump.to.x-jump.from.x,jump.to.y-jump.from.y)*.24));
  return {x:jump.from.x+(jump.to.x-jump.from.x)*ease,y:jump.from.y+(jump.to.y-jump.from.y)*ease-Math.sin(Math.PI*u)*height,r:jump.from.r+(jump.to.r-jump.from.r)*ease,u,airborne:(jump.initialAirborne||u>.09)&&u<.9,direction:jump.direction,landed:u===1?(now-jump.started-jump.duration)/1000:null};
 }
 api.move=(from,to,now,direction)=>{
  const target=perch(to),current=position(now)||perch(from||to);
  if(!from){anchor=target;jump=null;landings=[];return;}
  const dx=target.x-current.x;
  jump={from:{x:current.x,y:current.y,r:current.r},to:target,started:now,direction:direction|| (Math.abs(dx)>1?Math.sign(dx):(Math.sign(to.x-from.x)||current.direction||-1)),initialAirborne:!!current.airborne,duration:Math.min(700,Math.max(370,Math.hypot(target.x-current.x,target.y-current.y)*1.5))};anchor=target;
 };
 api.reanchor=(from,to)=>{
  if(!from)return;const dx=to.x-from.x,dy=to.y-from.y;
  // Scrolling changes the coordinate frame, not the jump's timing or facing.
  for(const point of new Set([anchor,jump?.from,jump?.to,...landings]))if(point){point.x+=dx;point.y+=dy;}
 };
 api.resetMotion=()=>{jump=null;anchor=null;landings=[];};
 api.position=()=>position(performance.now());
 api.focusScale=id=>id==='gamabunta'?1:({rasengan:1.36,chidori:1.32,sharingan:1.38,kurama:1.34,summoning:1.4,hiraishin:1.4,fireball:1.34,amaterasu:1.36,rasenshuriken:1.32}[id]||1.34);
 function dust(p,r){
  if(p.landed===null||p.landed===undefined||p.landed<0||p.landed>=.85)return [];
  const u=p.landed/.85,spread=1-Math.pow(1-u,3.4);
  return Array.from({length:14},(_,k)=>{const angle=k*tau/14+.12*(k%3),speed=.9+(k%4)*.23;
   return {x:p.x+Math.cos(angle)*r*(.12+spread*speed),y:p.y+Math.sin(angle)*r*(.03+spread*speed*.42)-u*r*.13,size:r*(.07+(k%3)*.014+u*.14),alpha:Math.pow(1-u,1.35)*(.5+(k%3)*.06)};
  });
 }
 const landingDust=now=>landings.flatMap(p=>dust({...p,landed:(now-p.started)/1000},p.r));
 api.motionSnapshot=(now=performance.now())=>{const p=position(now);return p?{...p,spriteFlip:spriteFlip(p.direction||-1,!!p.airborne),dust:landingDust(now)}:null;};
 const displaced=new WeakMap();
 function plate(c,image,size,t,mode){
  if(mode==='flame'||mode==='tails'){
   let cached=displaced.get(image);
   if(!cached){const surface=document.createElement('canvas');surface.width=surface.height=Math.min(256,image.naturalWidth);cached={surface,context:surface.getContext('2d'),tick:-1};displaced.set(image,cached);}
   const tick=Math.floor(t*30);
   if(cached.tick!==tick){
    const out=cached.context,n=cached.surface.width;out.clearRect(0,0,n,n);out.globalAlpha=1;
    // Each integer destination row is written once; apply transparency only on final composite.
    for(let row=0;row<n;row++){
     const y=row/n,offset=Math.sin(t*(mode==='tails'?1.3:2.2)+y*7)*n*(mode==='tails'?.014:.009);
     out.drawImage(image,0,row*image.height/n,image.width,image.height/n,offset,row,n,1);
    }
    cached.tick=tick;
   }
   c.drawImage(cached.surface,-size/2,-size/2,size,size);
  }else c.drawImage(image,-size/2,-size/2,size,size);
 }
 function electricity(c,r,t,energy=0){
  // Chidori is drawn entirely from moving forks, all converging toward one nucleus.
  const tick=Math.floor(t*24),noise=n=>{const v=Math.sin(n*127.1+tick*311.7)*43758.5453;return v-Math.floor(v);};
  const nucleus={x:Math.sin(t*13)*r*.035,y:Math.cos(t*11)*r*.035};
  const stroke=(points,alpha,width)=>{
   c.beginPath();points.forEach((p,i)=>i?c.lineTo(p.x,p.y):c.moveTo(p.x,p.y));
   c.globalAlpha=alpha;c.strokeStyle='#459cff';c.lineWidth=width*3;c.shadowColor='#67c7ff';c.shadowBlur=r*.085;c.stroke();
   c.strokeStyle='#ecfbff';c.lineWidth=width;c.shadowBlur=r*.018;c.stroke();
  };
  c.save();c.lineJoin='miter';c.lineCap='round';
  for(let k=0;k<22;k++){
   const angle=k*tau/22+(noise(k+3)-.5)*.24,reach=r*(.96+noise(k+63)*.34+energy*.12),points=[];
   for(let j=0;j<=9;j++){
    const u=j/9,rr=reach*(1-u),side=(noise(k*31+j)-.5)*r*.18*Math.sin(u*Math.PI);
    points.push({x:nucleus.x+Math.cos(angle)*rr-Math.sin(angle)*side,y:nucleus.y+Math.sin(angle)*rr+Math.cos(angle)*side});
   }
   stroke(points,.32+noise(k+90)*.4,r*(.006+noise(k+120)*.006));
   for(let f=0;f<2;f++){
    const join=points[3+f*2],a=angle+(f?1:-1)*(.4+noise(k+f+9)*.6),fork=[];
    for(let j=0;j<4;j++){const u=1-j/3;fork.push({x:join.x+Math.cos(a)*r*.28*u+Math.sin(j*4+k+tick)*r*.025*u,y:join.y+Math.sin(a)*r*.28*u});}
    stroke(fork,.35+noise(k+44)*.4,r*.005);
   }
   // A short charge segment travels OUTSIDE -> CENTER, then the next enters.
   const progress=(t*(.85+k%4*.12)+k*.137)%1,index=Math.floor(progress*7);
   stroke(points.slice(index,index+3),.7+energy*.2,r*.013);
  }
  // No filled disc: densely layered short, jagged bolts form the living core.
  for(let k=0;k<18;k++){
   const a=k*2.399+t*.16,rr=r*(.12+noise(k+400)*.17+energy*.08);
   stroke([{x:nucleus.x+Math.cos(a)*rr,y:nucleus.y+Math.sin(a)*rr},{x:nucleus.x+Math.cos(a+.6)*rr*.42,y:nucleus.y+Math.sin(a+.6)*rr*.42},nucleus],.8,r*(.01+noise(k+800)*.014));
  }
  c.restore();
 }
 function accents(c,id,r,t,energy){
  const color=api.list.find(v=>v.id===id).color;
  c.save();c.lineCap='round';
  if(id==='rasengan'||id==='rasenshuriken'){
   for(let k=0;k<18;k++){const a=k*tau/18+t*(id==='rasengan'?1.3:2.4),rad=r*(.82+(k%3)*.09);c.strokeStyle=k%2?'#e6fdff':'#72d8ee';c.globalAlpha=.2+(k%3)*.1;c.lineWidth=.6+(k%3)*.45;c.beginPath();c.ellipse(Math.cos(a)*r*.06,Math.sin(a)*r*.06,rad,rad*.83,a,a,a+.55);c.stroke();}
  }else if(id==='chidori'){
   electricity(c,r,t,energy);
  }else if(id==='sharingan'){
   c.strokeStyle='#f85a68';c.globalAlpha=.35;c.lineWidth=.7;c.beginPath();c.arc(0,0,r*(.75+Math.sin(t)*.025),0,tau);c.stroke();
  }else if(['kurama','fireball','amaterasu'].includes(id)){
   for(let k=0;k<20;k++){const phase=(t*(id==='amaterasu'?.16:.28)+k*.067)%1,a=k*2.399,rr=r*(.88+phase*.2);c.globalAlpha=(1-phase)*.6;c.fillStyle=id==='amaterasu'?'#18101f':color;c.beginPath();c.ellipse(Math.cos(a)*rr,Math.sin(a)*rr-phase*r*.17,1+phase*1.3,2+phase*1.8,a,0,tau);c.fill();}
  }else if(id==='summoning'){
   for(let k=0;k<10;k++){const phase=(t*.22+k*.1)%1,a=k*tau/10;c.globalAlpha=(1-phase)*.2;c.fillStyle='#e4e1d5';c.beginPath();c.arc(Math.cos(a)*r*(.82+phase*.25),Math.sin(a)*r*(.8+phase*.25)-phase*r*.15,r*(.035+phase*.09),0,tau);c.fill();}
  }else if(id==='hiraishin'){
   c.strokeStyle='#fff0b5';c.globalAlpha=.25+.12*Math.sin(t*1.7);c.lineWidth=.9;
   for(let k=0;k<6;k++){const a=k*tau/6;c.beginPath();c.moveTo(Math.cos(a)*r*.82,Math.sin(a)*r*.82);c.lineTo(Math.cos(a+.05)*r,Math.sin(a+.05)*r);c.stroke();}
  }c.restore();
 }
 function core(c,id,r,t){
  // Continuous chakra beneath the painted outer silhouette, with no punched-out center.
  const palette={rasengan:['#d2faff','#218cc5','#165875'],chidori:['#efffff','#6baff6','#3452ac'],sharingan:['#440b12','#c8283b','#6e1229'],kurama:['#f5b640','#df6227','#a72a20'],summoning:['#d8c9a5','#7e816c','#3a5149'],hiraishin:['#f7e4a3','#b28b38','#69552e'],fireball:['#ffe29a','#ee7c22','#b93820'],amaterasu:['#1b1728','#1a1224','#3f254e'],rasenshuriken:['#e4ffff','#72cbd1','#2f858f']}[id];
  c.save();const g=c.createRadialGradient(-r*.15,-r*.15,0,0,0,r*1.16);
  g.addColorStop(0,palette[0]);g.addColorStop(.5,palette[1]);g.addColorStop(.8,palette[2]);g.addColorStop(1,palette[2]+'00');c.globalAlpha=.9;c.fillStyle=g;c.beginPath();c.arc(0,0,r*1.16,0,tau);c.fill();
  c.beginPath();c.arc(0,0,r*1.08,0,tau);c.clip();
  if(id==='rasengan'||id==='rasenshuriken'){
   for(let k=0;k<23;k++){c.save();c.rotate(t*(k%2?1.1:-.85)+k*2.399);c.scale(1,.24+(k%5)*.13);c.strokeStyle=k%3?'#dafcff':'#4fbdea';c.lineWidth=.7+(k%3)*.55;c.globalAlpha=.25+(k%4)*.1;c.beginPath();c.arc(r*.13,0,r*(.22+k*.033),.15,4.9);c.stroke();c.restore();}
  }else if(id==='chidori'){
   for(let k=0;k<21;k++){const a=k*2.399+t*.32;c.strokeStyle=k%2?'#d3faff':'#6a93ed';c.globalAlpha=.4+(k%3)*.14;c.lineWidth=.5+k%3*.35;c.beginPath();for(let j=0;j<12;j++){const rr=r*j/11,angle=a+Math.sin(j*3.9+k+Math.floor(t*20))*.22;(j?c.lineTo.bind(c):c.moveTo.bind(c))(Math.cos(angle)*rr,Math.sin(angle)*rr);}c.stroke();}
  }else if(id==='sharingan'){
   c.rotate(t*.38);c.globalAlpha=.75;c.fillStyle='#210e19';c.beginPath();c.arc(0,0,r*.18,0,tau);c.fill();c.strokeStyle='#310e19';c.lineWidth=1.4;c.beginPath();c.arc(0,0,r*.6,0,tau);c.stroke();for(let k=0;k<3;k++){const a=k*tau/3;c.save();c.rotate(a);c.translate(r*.6,0);c.beginPath();c.arc(0,0,r*.105,0,tau);c.fill();c.beginPath();c.moveTo(-r*.1,0);c.bezierCurveTo(-r*.19,-r*.15,0,-r*.24,r*.13,-r*.17);c.quadraticCurveTo(-r*.015,-r*.12,r*.08,0);c.fill();c.restore();}
  }else if(id==='summoning'||id==='hiraishin'){
   c.strokeStyle=id==='summoning'?'#263c35':'#fae9a7';c.globalAlpha=.6;c.lineWidth=1.1;
   for(let k=0;k<12;k++){c.save();c.rotate(k*tau/12);c.beginPath();c.moveTo(r*.19,0);c.lineTo(r*.38,0);c.lineTo(r*.42,r*.08);c.lineTo(r*.67,r*.08);c.lineTo(r*.67,-r*.06);c.lineTo(r*.93,-r*.06);c.stroke();c.restore();}
   c.globalAlpha=.26;c.lineWidth=r*.07;for(const rad of[.32,.77]){c.beginPath();c.arc(0,0,r*rad,0,tau);c.stroke();}
  }else{
   const dark=id==='amaterasu';
   for(let k=0;k<19;k++){const a=k*2.399,flow=t*(dark?.65:1.35)+k*.7;c.save();c.rotate(a);c.globalAlpha=dark?.38:.3;c.strokeStyle=dark?'#8860a6':id==='kurama'?'#ffe58d':'#fff1bc';c.lineWidth=r*(.015+(k%3)*.007);c.beginPath();c.moveTo(-r*.18,r*.18);c.bezierCurveTo(r*.16,-r*.4,r*.62*Math.sin(flow),-r*.15,r*(.6+(k%4)*.1),-r*.6);c.stroke();c.restore();}
  }c.restore();
 }
 function frog(c,x,y,r,time,preview,energy){
  const t=time/1000;
  let p;
  if(preview){p={...perch({x,y,r}),direction:1,airborne:false,landed:null};}
  else p=time?(position(time)||perch({x,y,r})):{...(anchor||perch({x,y,r})),direction:jump?.direction||-1,airborne:false,landed:null};
  // The summoned creature is the focus marker; its landing seal remains on the target.
  // The painted summoning plate includes permanent smoke: use an ink-only seal here.
  c.save();c.translate(x,y);c.globalAlpha=.25;c.strokeStyle='#c9b18a';c.lineWidth=1.1;
  for(const scale of[1.14,1.28]){c.beginPath();c.arc(0,0,r*scale,0,tau);c.stroke();}
  for(let k=0;k<8;k++){const a=k*tau/8;c.save();c.rotate(a);c.beginPath();c.moveTo(r*1.03,0);c.lineTo(r*1.18,0);c.lineTo(r*1.18,r*.055);c.lineTo(r*1.32,r*.055);c.moveTo(r*1.22,-r*.04);c.lineTo(r*1.3,-r*.04);c.stroke();c.restore();}
  c.restore();
  c.save();c.translate(p.x,p.y);const shadowY=(anchor?.y??p.y)-p.y;c.globalAlpha=p.airborne?.15:.3;c.fillStyle='#000';c.beginPath();c.ellipse(0,shadowY+4,r*.34,r*.08,0,0,tau);c.fill();c.globalAlpha=1;
  const bounce=!p.airborne&&p.landed!==null&&p.landed!==undefined?Math.sin(p.landed*19)*Math.exp(-p.landed*8)*.13:0;
  c.rotate(p.airborne?(p.direction||-1)*-.12:Math.sin(t*1.3)*.015);c.scale(spriteFlip(p.direction||-1,!!p.airborne)*(1+(p.airborne?-.06:bounce)),1+(p.airborne?.12:-bounce));
  const name=p.airborne?'gamabunta-jump':'gamabunta-idle',sprite=images[name],size=p.r*1.45;
  if(loaded[name])c.drawImage(sprite,-size/2,-size*.88,size,size);
  else{c.fillStyle='#b9684a';c.beginPath();c.ellipse(0,-size*.3,size*.32,size*.26,0,0,tau);c.fill();}
  c.restore();
  // Foot-level plume remains visible in front of the landing pose, then evaporates.
  c.save();for(const puff of (preview?dust(p,p.r):landingDust(time))){c.globalAlpha=puff.alpha;c.fillStyle='#d9cfb6';c.strokeStyle='#96866d';c.lineWidth=.5;c.beginPath();c.ellipse(puff.x,puff.y,puff.size,puff.size*.6,0,0,tau);c.fill();c.stroke();c.fillStyle='#eee5d1';c.beginPath();c.ellipse(puff.x-puff.size*.2,puff.y-puff.size*.28,puff.size*.65,puff.size*.5,0,0,tau);c.fill();}c.restore();
 }
 api.draw=(c,id,x,y,r,time=0,energy=0,preview=false)=>{
  if(id==='gamabunta'){frog(c,x,y,r,time,preview,energy);return;}
  if(id==='chidori'){c.save();c.translate(x,y);electricity(c,r,time/1000,energy);c.restore();return;}
  if(!loaded[id]){baseDraw(c,id,x,y,r,time,energy);return;}
  const t=time/1000;c.save();c.translate(x,y);
  if(!completePaintings.has(id))core(c,id,r,t);
  const rotate=id==='rasengan'?t*.82:id==='sharingan'?t*.38:id==='rasenshuriken'?t*2.05:id==='hiraishin'?Math.sin(t)*.018:0;
  c.rotate(rotate);c.globalAlpha=id==='amaterasu'?.95:id==='chidori'?.88+Math.sin(t*17)*.07:.84;
  const size=r*2.46*(1+Math.sin(t*(id==='chidori'?3.2:1.5))*.016+energy*.035);
  plate(c,images[id],size,t,['fireball','amaterasu'].includes(id)?'flame':id==='kurama'?'tails':'still');c.restore();
  c.save();c.translate(x,y);if(id==='sharingan'){c.fillStyle='#fff5e4';c.globalAlpha=.85;c.beginPath();c.ellipse(-r*.68,-r*.35,r*.065,r*.12,.6,0,tau);c.fill();}if(!completePaintings.has(id)||id==='chidori')accents(c,id,r,t,energy);if(energy){c.globalAlpha=energy*.5;c.strokeStyle=api.list.find(v=>v.id===id).color;c.lineWidth=1.2;c.beginPath();c.arc(0,0,r*(1.04+(1-energy)*.14),0,tau);c.stroke();}c.restore();
 };
})();
