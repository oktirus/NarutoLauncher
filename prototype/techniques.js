'use strict';
/* Each technique has its own geometry and movement, shared by previews and TV focus. */
window.KonohaTechniques=(()=>{
 const tau=Math.PI*2;
 const list=[
  {id:'rasengan',name:'Расенґан',owner:'Наруто',color:'#85e5ff',cue:'rasengan',description:'Щільні спіралі чакри й обертовий вихор.'},
  {id:'chidori',name:'Чідорі',owner:'Саске / Какаші',color:'#b8dfff',cue:'chidori',description:'Рвані розгалуження блискавки навколо іконки.'},
  {id:'sharingan',name:'Шарінґан',owner:'Клан Учіха',color:'#ff6265',cue:'sharingan',description:'Три томое на червоній райдужці.'},
  {id:'kurama',name:'Чакра Курами',owner:'Наруто',color:'#ff9a46',cue:'kurama',description:'Дев’ять живих хвостів і теплий покрив чакри.'},
  {id:'summoning',name:'Виклик жаби',owner:'Джирая / Наруто',color:'#d7c08b',cue:'summoning',description:'Печатка призову, дим і маленька жаба.'},
  {id:'hiraishin',name:'Хірайшин',owner:'Мінато · Четвертий хокаге',color:'#ffe59b',cue:'hiraishin',description:'Трипроменевий кунай і золотий слід телепорту.'},
  {id:'fireball',name:'Вогняна куля',owner:'Саске',color:'#ffb65c',cue:'fireball',description:'Вогняна корона з язиками полум’я.'},
  {id:'amaterasu',name:'Аматерасу',owner:'Ітачі / Саске',color:'#c3a0ed',cue:'amaterasu',description:'Чорне полум’я з холодним фіолетовим краєм.'},
  {id:'rasenshuriken',name:'Расенсюрикен',owner:'Наруто · стихія вітру',color:'#b7f1e5',cue:'rasenshuriken',description:'Чотири леза вітру навколо спірального ядра.'}
 ];
 function stroke(c,color,width=1.5,glow=0){c.strokeStyle=color;c.lineWidth=width;c.shadowColor=color;c.shadowBlur=glow;c.lineCap='round';c.lineJoin='round';}
 function circle(c,r,color,width=1.5){stroke(c,color,width);c.beginPath();c.arc(0,0,r,0,tau);c.stroke();}
 function spiral(c,r,t){
  for(let j=0;j<7;j++){
   c.save();c.rotate(t*1.45+j*tau/7);stroke(c,j%2?'#d8faff':'#62cfea',j%2?1:2,5);c.globalAlpha=.45;
   c.beginPath();for(let k=0;k<=44;k++){const a=k/44*3.6,rad=r*(.72+k/44*.3),x=Math.cos(a)*rad,y=Math.sin(a)*rad*.96;k?c.lineTo(x,y):c.moveTo(x,y);}c.stroke();c.restore();
  }
  c.save();c.globalAlpha=.5;circle(c,r*.94,'#bff7ff',1);c.restore();
 }
 function bolt(c,x,y,angle,length,phase,alpha=1){
  c.save();c.translate(x,y);c.rotate(angle);c.globalAlpha*=alpha;stroke(c,'#c8e9ff',1.5,9);c.beginPath();c.moveTo(0,0);
  for(let k=1;k<=6;k++){const px=length*k/6,py=Math.sin(k*12.3+phase)*length*.15;c.lineTo(px,py);if(k===3){c.moveTo(px,py);c.lineTo(px+length*.16,py+length*.28);c.moveTo(px,py);}}
  c.stroke();c.restore();
 }
 function tomoe(c,x,y,s){c.save();c.translate(x,y);c.fillStyle='#190b11';c.beginPath();c.arc(0,0,s,0,tau);c.fill();c.beginPath();c.moveTo(-s,0);c.bezierCurveTo(-s*1.4,-s*1.7,s*.7,-s*2.3,s*1.9,-s*1.5);c.quadraticCurveTo(s*.2,-s*1.4,s*.6,s*.1);c.fill();c.restore();}
 function seal(c,r,t){
  c.save();c.rotate(Math.sin(t*.3)*.025);circle(c,r,'#cbb786',1.7);circle(c,r*.83,'#a89775',1);
  for(let k=0;k<8;k++){c.save();c.rotate(k*tau/8);stroke(c,'#d9c895',1.3);c.beginPath();c.moveTo(r*.6,0);c.lineTo(r*.8,0);c.lineTo(r*.91,r*.07);c.lineTo(r*1.1,r*.07);c.moveTo(r*.96,-r*.055);c.lineTo(r*1.07,-r*.055);c.stroke();c.restore();}c.restore();
 }
 function kunai(c,r,t){
  c.save();c.translate(r*.72,r*.63);c.rotate(-.6+Math.sin(t)*.04);stroke(c,'#fff0bc',1.5,7);c.fillStyle='#78613b';
  c.beginPath();c.moveTo(0,-r*.48);c.lineTo(r*.08,-r*.2);c.lineTo(r*.22,-r*.35);c.lineTo(r*.17,-r*.1);c.lineTo(0,0);c.lineTo(-r*.17,-r*.1);c.lineTo(-r*.22,-r*.35);c.lineTo(-r*.08,-r*.2);c.closePath();c.fill();c.stroke();
  c.beginPath();c.moveTo(0,0);c.lineTo(0,r*.24);c.stroke();c.beginPath();c.arc(0,r*.29,r*.055,0,tau);c.stroke();c.restore();
 }
 function flames(c,r,t,dark){
  for(let k=0;k<22;k++){
   const a=k*tau/22,phase=t*(dark?1.5:2)+k*1.61,h=r*(.15+.16*(.5+.5*Math.sin(phase))),w=r*.105;
   c.save();c.rotate(a);c.translate(r*.97,0);c.rotate(Math.PI/2);c.fillStyle=dark?'#100c19':(k%2?'#f47a25':'#ffd16c');stroke(c,dark?'#9970c4':'#ffb94a',dark?1.3:.65,dark?3:7);
   c.beginPath();c.moveTo(-w,0);c.bezierCurveTo(-w*1.2,-h*.5,-w*.4,-h*.5,Math.sin(phase)*w,-h);c.bezierCurveTo(w*.6,-h*.6,w,-h*.2,w,0);c.closePath();c.fill();c.stroke();
   if(!dark){c.globalAlpha=.8;c.fillStyle='#ffe5a1';c.beginPath();c.moveTo(-w*.4,0);c.quadraticCurveTo(0,-h*.65,w*.45,0);c.fill();}c.restore();
  }
 }
 function draw(c,id,x,y,r,time=0,energy=0){
  const t=time/1000,definition=list.find(v=>v.id===id)||list[0];id=definition.id;
  c.save();c.translate(x,y);c.lineWidth=1.5;
  // Clear icon center by placing all ornament around its perimeter.
  if(id==='rasengan'){
   spiral(c,r,t);c.save();c.translate(r*.68,r*.67);spiral(c,r*.24,-t*2);c.restore();
  }else if(id==='chidori'){
   c.globalAlpha=.35;circle(c,r*.91,'#6cacf6');c.globalAlpha=1;
   for(let k=0;k<12;k++){const a=k*tau/12+t*.12,phase=Math.floor(t*9)+k*1.9,rr=r*(.88+.08*Math.sin(phase));bolt(c,Math.cos(a)*rr,Math.sin(a)*rr,a+.4,r*(.18+.2*(.5+.5*Math.sin(phase))),phase,.6+.25*Math.sin(phase));}
  }else if(id==='sharingan'){
   c.globalAlpha=.65;circle(c,r,'#ee424b',3);circle(c,r*.79,'#741c28',1);c.globalAlpha=1;c.rotate(t*.6);
   for(let k=0;k<3;k++){const a=k*tau/3;c.save();c.rotate(a);c.translate(r*.87,0);c.fillStyle='#ec4d54';c.beginPath();c.arc(0,0,r*.145,0,tau);c.fill();tomoe(c,0,0,r*.075);c.restore();}
   c.rotate(-t*.6);c.translate(r*.67,r*.65);c.fillStyle='#bc2534';c.beginPath();c.arc(0,0,r*.22,0,tau);c.fill();circle(c,r*.22,'#ff7d7d',1);c.rotate(t*.6);c.fillStyle='#160d14';c.beginPath();c.arc(0,0,r*.042,0,tau);c.fill();for(let k=0;k<3;k++){c.save();c.rotate(k*tau/3);tomoe(c,r*.125,0,r*.036);c.restore();}
  }else if(id==='kurama'){
   for(let k=0;k<9;k++){const a=-Math.PI+(k+.5)*Math.PI/9,phase=t*1.7+k*.8;c.save();c.rotate(a);stroke(c,k%2?'#ffad4c':'#f57631',r*.09,9);c.globalAlpha=.65;c.beginPath();c.moveTo(r*.77,0);c.bezierCurveTo(r*1.05,-r*.3,r*1.48,Math.sin(phase)*r*.3,r*1.1,-r*.43);c.stroke();stroke(c,'#ffe09c',1.3);c.globalAlpha=.65;c.stroke();c.restore();}
   c.globalAlpha=.5;circle(c,r*.92,'#fda644',2.5);
  }else if(id==='summoning'){
   seal(c,r,t);
   for(let k=0;k<7;k++){const phase=(t*.27+k/7)%1,a=k*tau/7;c.save();c.globalAlpha=(1-phase)*.23;c.fillStyle='#dddaca';c.beginPath();c.arc(Math.cos(a)*r*(1+phase*.25),Math.sin(a)*r*(1+phase*.25)-phase*r*.12,r*(.07+phase*.09),0,tau);c.fill();c.restore();}
   // Deliberately small, ink-like toad silhouette beside the seal.
   c.save();c.translate(r*.72,r*.64);c.fillStyle='#77906b';stroke(c,'#d0c69b',1);c.beginPath();c.ellipse(0,0,r*.2,r*.12,0,0,tau);c.fill();c.stroke();for(const s of[-1,1]){c.beginPath();c.arc(s*r*.12,-r*.09,r*.06,0,tau);c.fill();c.stroke();c.fillStyle='#e7d692';c.fillRect(s*r*.12-r*.015,-r*.105,r*.025,r*.025);c.fillStyle='#77906b';}c.restore();
  }else if(id==='hiraishin'){
   c.save();c.globalAlpha=.45;stroke(c,'#ffe6a0',1,5);
   for(let k=0;k<4;k++){const a=k*Math.PI/2;c.save();c.rotate(a);c.beginPath();c.moveTo(r*.72,-r*.6);c.lineTo(r*.93,-r*.6);c.lineTo(r*.93,r*.28);c.moveTo(r*.93,r*.38);c.lineTo(r*.93,r*.6);c.lineTo(r*.76,r*.6);c.stroke();c.restore();}c.restore();
   const travel=(t*.37)%1;c.save();c.rotate(travel*tau);stroke(c,'#fff0b5',2,8);c.beginPath();c.arc(0,0,r,0,.34);c.stroke();c.restore();kunai(c,r,t);
  }else if(id==='fireball'){
   c.globalAlpha=.4;circle(c,r*.93,'#fca33c',3);c.globalAlpha=.85;flames(c,r,t,false);
   for(let k=0;k<6;k++){const p=(t*.4+k/6)%1,a=k*tau/6;c.globalAlpha=(1-p)*.65;c.fillStyle='#ffd37a';c.beginPath();c.arc(Math.cos(a)*r*(1+p*.3),Math.sin(a)*r*(1+p*.3)-p*r*.18,1.2,0,tau);c.fill();}
  }else if(id==='amaterasu'){
   c.globalAlpha=.9;flames(c,r,t,true);c.globalAlpha=.4;circle(c,r*.92,'#3a234c',3);
  }else if(id==='rasenshuriken'){
   c.save();c.rotate(t*1.6);for(let k=0;k<4;k++){c.rotate(Math.PI/2);c.fillStyle='#b8f5eb33';stroke(c,'#b9efe5',1.3,5);c.beginPath();c.moveTo(r*.72,-r*.18);c.quadraticCurveTo(r*1.27,-r*.48,r*1.38,-r*.08);c.quadraticCurveTo(r*1.1,-r*.17,r*.86,r*.13);c.closePath();c.fill();c.stroke();}c.restore();spiral(c,r*.8,-t*.7);
  }
  if(energy>0){c.globalAlpha=Math.min(energy,.75);stroke(c,definition.color,1.8,7);c.beginPath();c.arc(0,0,r*(1+(1-energy)*.7),0,tau);c.stroke();}
  c.restore();
 }
 function trail(c,id,a,b,life,time){
  const def=list.find(v=>v.id===id)||list[0],alpha=life/.45;
  c.save();c.globalAlpha=alpha*.6;stroke(c,def.color,1.5,6);
  if(id==='hiraishin'){
   for(const p of[a,b]){c.beginPath();c.moveTo(p.x-9,p.y-30);c.lineTo(p.x,p.y-42);c.lineTo(p.x+9,p.y-30);c.moveTo(p.x,p.y-42);c.lineTo(p.x,p.y+20);c.stroke();}
  }else if(id==='chidori'){
   const length=Math.hypot(b.x-a.x,b.y-a.y);bolt(c,a.x,a.y,Math.atan2(b.y-a.y,b.x-a.x),length,Math.floor(time/60),1);
  }else if(id==='summoning'){
   for(let k=0;k<6;k++){const p=k/5;c.fillStyle='#d0c6aa';c.globalAlpha=alpha*.13;c.beginPath();c.arc(a.x+(b.x-a.x)*p,a.y+(b.y-a.y)*p-12*Math.sin(p*Math.PI),4+(1-alpha)*9,0,tau);c.fill();}
  }else{
   c.beginPath();c.moveTo(a.x,a.y);c.quadraticCurveTo((a.x+b.x)/2,(a.y+b.y)/2-25,b.x,b.y);c.stroke();
  }c.restore();
 }
 return {list,draw,trail};
})();
