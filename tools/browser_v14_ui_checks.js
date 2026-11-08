(async()=>{
 const results=[],margins=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);};
 const before={...prefs},root=document.documentElement,savedPx=root.style.getPropertyValue('--px'),savedPy=root.style.getPropertyValue('--py');prefs.audio=false;prefs.motion=false;applyMotion();ambience();
 const lum=c=>c.map(v=>v/255).map(v=>v<=.04045?v/12.92:((v+.055)/1.055)**2.4).reduce((s,v,i)=>s+v*[.2126,.7152,.0722][i],0),rgb=s=>(s.match(/[\d.]+/g)||[]).map(Number);
 for(const id of ['sai','jiraiya','minato']){
  setScene(id,false,false);await decodeScene(scenes.find(s=>s.id===id));await new Promise(r=>requestAnimationFrame(r));
  const button=document.querySelector('.audio-invitation');check(id+' exact accessible label',button.getAttribute('aria-label')===document.querySelector('#audio-label').textContent);check(id+' muted pressed state',button.getAttribute('aria-pressed')==='false');
  if(innerWidth>=901&&innerHeight<=620){
   const style=getComputedStyle(button),fg=rgb(style.color),bg=rgb(style.backgroundColor),alpha=bg[3]??1,worst=bg.slice(0,3).map(v=>v*alpha+255*(1-alpha)),contrast=(Math.max(lum(fg),lum(worst))+.05)/(Math.min(lum(fg),lum(worst))+.05);
   check(id+' compact contrast at least 4.5 even over white',contrast>=4.5,String(contrast));check(id+' compact topbar has contrast shadow',getComputedStyle(document.querySelector('.topbar')).textShadow!=='none');
   for(const px of [-40,0,40])for(const py of [-40,0,40]){
    root.style.setProperty('--px',String(px));root.style.setProperty('--py',String(py));const r=document.querySelector('.backdrop.shown').getBoundingClientRect();check(id+' backdrop covers viewport at '+px+','+py,r.left<=0&&r.top<=0&&r.right>=innerWidth&&r.bottom>=innerHeight,JSON.stringify(r.toJSON()));margins.push({id,px,py,bottom:r.bottom-innerHeight});
   }
  }
 }
 Object.assign(prefs,before);root.style.setProperty('--px',savedPx);root.style.setProperty('--py',savedPy);setScene(prefs.scene,false,false);applyMotion();ambience();return{passed:results.length,results,margins,viewport:[innerWidth,innerHeight]};
})()
