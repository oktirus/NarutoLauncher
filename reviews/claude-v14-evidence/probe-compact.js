(()=>{const W=innerWidth,H=innerHeight;const bs=[...document.querySelectorAll('.backdrop')];const b=document.querySelector('.backdrop.shown')||bs[0];
const r=e=>{const q=e.getBoundingClientRect();return {l:+q.left.toFixed(1),t:+q.top.toFixed(1),r:+q.right.toFixed(1),b:+q.bottom.toFixed(1)}};
const cov=q=>q.l<=0.5&&q.t<=0.5&&q.r>=W-0.5&&q.b>=H-0.5;
const nat={rect:r(b),tr:getComputedStyle(b).transform,px:getComputedStyle(b).getPropertyValue('--px'),py:getComputedStyle(b).getPropertyValue('--py')};nat.cov=cov(nat.rect);
const syn=[];for(const px of[-40,0,40])for(const py of[-40,0,40]){b.style.setProperty('--px',px);b.style.setProperty('--py',py);const q=r(b);syn.push({px,py,b:q.b,t:q.t,l:q.l,r:q.r,cov:cov(q)});}
b.style.removeProperty('--px');b.style.removeProperty('--py');
const ai=document.querySelector('.audio-invitation');const as=getComputedStyle(ai),aa=getComputedStyle(ai,'::after');
const lab=document.getElementById('audio-label');
const els={title:'.hero-copy h1',desc:'#hero-description',caption:'.scene-caption',audio:'.audio-invitation',clock:'.clock',date:'.clock span',time:'.clock time',dock:'.app-dock',strip:'.scene-strip',nav:'.topbar nav',heading:'.section-heading',footer:'footer'};
const R={};for(const[k,s]of Object.entries(els)){const e=document.querySelector(s);if(e){const cs=getComputedStyle(e);R[k]={...r(e),vis:cs.display!=='none'&&cs.visibility!=='hidden'&&+cs.opacity>0}}}
const ov=(a,b)=>a&&b&&a.l<b.r&&b.l<a.r&&a.t<b.b&&b.t<a.b;
const pairs=[['title','caption'],['desc','caption'],['audio','caption'],['audio','dock'],['audio','heading'],['caption','strip'],['title','strip'],['clock','nav'],['caption','dock']];
const overl=pairs.filter(([x,y])=>ov(R[x],R[y])).map(p=>p.join('/'));
const ctrls=[...document.querySelectorAll('#shell button, #shell a')].filter(e=>e.offsetParent).map(e=>({t:(e.getAttribute('aria-label')||e.textContent).trim().slice(0,24),...r(e)}));
const off=ctrls.filter(c=>c.r>W+1||c.b>H+1||c.l<-1||c.t<-1);
const hidden=ctrls.filter(c=>{const x=(c.l+c.r)/2,y=(c.t+c.b)/2;if(x<0||y<0||x>W||y>H)return false;const e=document.elementFromPoint(x,y);return false});
return JSON.stringify({vp:[W,H],scene:document.body.dataset.scene,img:getComputedStyle(b).backgroundImage.split('/').slice(-2).join('/'),nat,synAllCov:syn.every(s=>s.cov),synMinBottomMargin:Math.min(...syn.map(s=>s.b-H)).toFixed(1),synMinTopMargin:Math.min(...syn.map(s=>-s.t)).toFixed(1),synMinSide:Math.min(...syn.map(s=>Math.min(-s.l,s.r-W))).toFixed(1),audio:{bg:as.backgroundColor,shadow:as.textShadow,color:as.color,after:aa.content,labelClip:getComputedStyle(lab).clipPath,ariaLabel:ai.getAttribute('aria-label'),pressed:ai.getAttribute('aria-pressed'),rect:R.audio},topShadow:getComputedStyle(document.querySelector('.topbar')).textShadow,overlaps:overl,offscreenControls:off,R});})()
