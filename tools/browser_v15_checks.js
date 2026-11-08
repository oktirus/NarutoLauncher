(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+JSON.stringify(detail));};
 const $=s=>document.querySelector(s);
 if(modal)closePanel();if(ambient)leaveAmbient();
 prefs.audio=false;prefs.ambience=false;prefs.rotation=0;applyTimers();
 await KonohaTechniques.ready;setScene('sakura',false,false);await decodeScene(scenes.find(s=>s.id==='sakura'));setTechnique('chidori',false);
 await new Promise(requestAnimationFrame);await Promise.all(document.getAnimations().filter(a=>a.effect.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})));
 check('17 scenes retained',scenes.length===17);check('10 techniques retained',KonohaTechniques.list.length===10);
 check('Sakura v15 active',getComputedStyle($('.backdrop.shown')).backgroundImage.includes('/v15/scene-sakura.jpg'));
 check('Chidori v15 source',KonohaEffectSources.chidori.includes('/v15/chidori.webp'));
 check('all painted effects decoded',Object.values(KonohaTechniques.artStatus()).every(Boolean));
 check('Chidori is a complete painting',KonohaTechniques.completePaintings.includes('chidori'));
 const canvas=document.createElement('canvas');canvas.width=canvas.height=320;const c=canvas.getContext('2d');
 const digest=time=>{c.clearRect(0,0,320,320);KonohaTechniques.draw(c,'chidori',160,160,90,time,0,true);const data=c.getImageData(0,0,320,320).data;let hash=0;for(const v of data)hash=(Math.imul(hash,31)+v)|0;return hash;};
 const first=digest(0),middle=digest(530),last=digest(1900);check('three different animation phases',first!==middle&&middle!==last&&first!==last);
 const center=c.getImageData(155,155,10,10).data;check('center painted without hollow',Array.from(center).filter((_,i)=>i%4===3).every(a=>a>180));
 prefs.motion=false;applyMotion();digest(0);check('motion-off center remains painted',c.getImageData(160,160,1,1).data[3]>180);prefs.motion=true;applyMotion();
 const bg=$('.backdrop.shown'),r=bg.getBoundingClientRect(),style=getComputedStyle(bg),image=new Image();image.src='../assets/runtime/'+scenes.find(s=>s.id==='sakura').image;await image.decode();
 const scale=Math.max(bg.offsetWidth/image.naturalWidth,bg.offsetHeight/image.naturalHeight),w=image.naturalWidth*scale,h=image.naturalHeight*scale;
 const parts=style.backgroundPosition.split(' '),pct=s=>s==='center'?.5:parseFloat(s)/100;
 const ox=(bg.offsetWidth-w)*pct(parts[0]),oy=(bg.offsetHeight-h)*pct(parts[1]||'50%'),sx=r.width/bg.offsetWidth,sy=r.height/bg.offsetHeight;
 const project=([x1,y1,x2,y2])=>({left:r.left+(ox+w*x1)*sx,top:r.top+(oy+h*y1)*sy,right:r.left+(ox+w*x2)*sx,bottom:r.top+(oy+h*y2)*sy});
 // Approximate face skin (not the wider hair silhouette) and both holding hands.
 const face=project([.727,.215,.816,.368]),hands=project([.712,.44,.781,.54]),overlap=(a,b)=>Math.min(a.right,b.right)>Math.max(a.left,b.left)&&Math.min(a.bottom,b.bottom)>Math.max(a.top,b.top);
 check('face within screen',face.left>=0&&face.right<=innerWidth&&face.top>=0&&face.bottom<=innerHeight,face);
 check('hands above app dock',hands.bottom<$('#dock').getBoundingClientRect().top,hands);
 for(const selector of ['#hero-title','#hero-description','.hero-actions .paper-button','.hero-actions .ghost-button','.audio-invitation','.clock']){
  const el=$(selector);if(el.getClientRects().length){
   const boxes=[];
   if(selector.startsWith('#hero-')){const walker=document.createTreeWalker(el,NodeFilter.SHOW_TEXT);let node;while(node=walker.nextNode()){if(!node.textContent.trim())continue;const range=document.createRange();range.selectNodeContents(node);boxes.push(...range.getClientRects());}}
   else boxes.push(el.getBoundingClientRect());
   check(selector+' text/control clear of face',boxes.every(b=>!overlap(face,b)));
   check(selector+' text/control clear of hands',boxes.every(b=>!overlap(hands,b)));
  }
 }
 const app=$('#dock .app-card');app.focus();check('focus starts on first app',document.activeElement===app);
 KonohaRemote('ArrowRight');check('D-pad moves to another app',document.activeElement.matches('#dock .app-card')&&document.activeElement!==app);check('Chidori remains selected',prefs.technique==='chidori');
 app.focus();return {viewport:[innerWidth,innerHeight],passed:results.filter(r=>r.passed).length,results,face,hands};
})()
