(async()=>{
 const results=[],layouts=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);};
 const wait=ms=>new Promise(r=>setTimeout(r,ms)),before={...prefs,pins:[...prefs.pins]};if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=false;ambience();
 const [rows,regions]=await Promise.all([fetch('../revisions/v13/asset-manifest.json').then(r=>r.json()),fetch('../revisions/v13/visibility-regions.json').then(r=>r.json())]);
 const box=e=>{const r=e.getBoundingClientRect();return{left:r.left,top:r.top,right:r.right,bottom:r.bottom,width:r.width,height:r.height};};
 const overlap=(a,b)=>Math.max(0,Math.min(a.right,b.right)-Math.max(a.left,b.left))*Math.max(0,Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top));
 function project(n){const layer=document.querySelector('.backdrop.shown'),rect=box(layer),w=layer.clientWidth,h=layer.clientHeight,k=Math.max(w/1920,h/1080),iw=1920*k,ih=1080*k,pos=getComputedStyle(layer).backgroundPosition.split(' ').map(v=>parseFloat(v)/100),sx=rect.width/w,sy=rect.height/h;return{left:rect.left+((w-iw)*pos[0]+n[0]*iw)*sx,top:rect.top+((h-ih)*pos[1]+n[1]*ih)*sy,right:rect.left+((w-iw)*pos[0]+n[2]*iw)*sx,bottom:rect.top+((h-ih)*pos[1]+n[3]*ih)*sy};}
 check('seventeen scenes retained',scenes.length===17&&new Set(scenes.map(s=>s.id)).size===17);check('twenty icon sources retained',Object.keys(KonohaIconSources).length===20);
 for(const row of rows){
  const image=new Image();image.src='../'+row.file;await image.decode();check(row.id+' Full HD decodes',image.naturalWidth===1920&&image.naturalHeight===1080);
  const bytes=await fetch(image.src).then(r=>r.arrayBuffer()),hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))].map(v=>v.toString(16).padStart(2,'0')).join('');check(row.id+' exact served bytes',hash===row.sha256);
  const s=scenes.find(s=>s.id===row.id);check(row.id+' versioned v13 path',s.image==='v13/scene-'+s.id+'.jpg?v='+hash.slice(0,12));setScene(s.id);await decodeScene(s);await wait(80);
  check(row.id+' selection and title saved',prefs.scene===s.id&&JSON.parse(localStorage.getItem('konoha.preferences.v1')).scene===s.id&&document.querySelector('#hero-title').innerHTML===s.title);
  const title=box(document.querySelector('#hero-title'));check(row.id+' title fits viewport',title.left>=0&&title.right<=innerWidth);
  const principal=project(regions[s.id].principal),dockTop=Math.min(...[...document.querySelectorAll('#dock .app-emblem')].map(e=>box(e).top)),caption=box(document.querySelector('.scene-caption')),area=(principal.right-principal.left)*(principal.bottom-principal.top),visible=overlap(principal,{left:0,top:0,right:innerWidth,bottom:innerHeight})/area;
  check(s.id+' principal prop above app icons',principal.bottom<dockTop,JSON.stringify({principal,dockTop}));check(s.id+' principal prop inside viewport',visible>.96,String(visible));check(s.id+' caption clear of principal prop',overlap(principal,caption)===0);
  if(innerWidth<=560){check(s.id+' heading clear of principal prop',overlap(principal,title)===0);check(s.id+' action buttons clear of principal prop',[...document.querySelectorAll('.hero-actions button')].every(e=>overlap(principal,box(e))===0));}
  if(s.id==='sai')check('v12 Sai would fail the same above-dock guard',project(regions.beforeSai).bottom>=dockTop);
  layouts.push({id:s.id,principal,dockTop,caption,visible});
 }
 openPanel('scenes');const thumbs=[...document.querySelectorAll('.scene-card img')];await Promise.all(thumbs.map(i=>i.decode()));check('seventeen scene thumbnails decode',thumbs.length===17&&thumbs.every(i=>i.naturalWidth));document.querySelector('[data-scene=sai].scene-card').click();check('Sai chooser selection works',prefs.scene==='sai'&&!modal);
 check('no horizontal overflow',document.documentElement.scrollWidth<=innerWidth);Object.assign(prefs,before);persist();renderDock();setScene(prefs.scene,false,false);applyMotion();ambience();document.querySelector('#dock .app-card').focus();
 return {passed:results.length,results,layouts,viewport:[innerWidth,innerHeight]};
})()
