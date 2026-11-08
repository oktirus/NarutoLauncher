(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);};
 const wait=ms=>new Promise(r=>setTimeout(r,ms)),before={...prefs,pins:[...prefs.pins]};if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=false;ambience();
 const rows=await fetch('../revisions/v11/asset-manifest.json').then(r=>r.json());
 check('twelve wallpapers and twenty icon sources',scenes.length===12&&Object.keys(KonohaIconSources).length===20);
 for(const row of rows){
  const img=new Image();img.src='../'+row.file;await img.decode();check(row.id+' asset decodes',img.naturalWidth===row.size[0]&&img.naturalHeight===row.size[1]);
  const data=await fetch(img.src).then(r=>r.arrayBuffer()),hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',data))].map(v=>v.toString(16).padStart(2,'0')).join('');check(row.id+' served exact bytes',hash===row.sha256);
  if(row.kind==='icon'){
   check(row.id+' registry points to correct version',KonohaIconSources[row.id]==='../'+row.file+'?v='+row.sha256.slice(0,12));
   const canvas=document.createElement('canvas');canvas.width=canvas.height=640;const c=canvas.getContext('2d');c.drawImage(img,0,0);check(row.id+' transparent corners',[[0,0],[639,0],[0,639],[639,639]].every(([x,y])=>c.getImageData(x,y,1,1).data[3]===0));
  }else{
   const s=scenes.find(s=>s.id===row.id);setScene(s.id);await decodeScene(s);check(row.id+' new scene preference and title',document.body.dataset.scene===s.id&&document.querySelector('#hero-title').innerHTML===s.title&&JSON.parse(localStorage.getItem('konoha.preferences.v1')).scene===s.id);const r=document.querySelector('#hero-title').getBoundingClientRect();check(row.id+' title fits',r.left>=0&&r.right<=innerWidth);
  }
 }
 setScene('dusk');openPanel('library');await wait(70);const cards=[...document.querySelectorAll('#panel-content .app-card')].filter(c=>c.dataset.kind);check('nineteen apps in library',cards.length===19);
 for(const c of cards){const img=c.querySelector('.app-emblem img');await img.decode();check(c.dataset.kind+' library uses new icon',img.getAttribute('src')===KonohaIconSources[c.dataset.kind]);}
 const packages=apps.slice(0,6).map(a=>a.package);prefs.pins=packages;renderDock();closePanel();const dock=[...document.querySelectorAll('#dock .app-emblem img')];await Promise.all(dock.map(i=>i.decode()));check('all seven dock images use new set',dock.length===7&&dock.every(i=>i.src.includes('/runtime/v11/')));check('archive new icon',dock.at(-1).getAttribute('src')===KonohaIconSources.archive);
 openPanel('techniques');await wait(90);const previews=[...document.querySelectorAll('.technique-art>img:not(.frog-second-app)')];await Promise.all(previews.map(i=>i.decode()));check('ten technique previews use new SmartTube',previews.length===10&&previews.every(i=>i.getAttribute('src')===KonohaIconSources.smarttube));check('frog second app uses new Lampa',document.querySelector('.frog-second-app').getAttribute('src')===KonohaIconSources.lampa);closePanel();
 openPanel('scenes');await wait(50);const gallery=[...document.querySelectorAll('.scene-card img')];await Promise.all(gallery.map(i=>i.decode()));check('all twelve scene thumbnails decode',gallery.length===12&&gallery.every(i=>i.naturalWidth));document.querySelector('[data-scene=gaara].scene-card').click();check('scene chooser selects Gaara and closes',prefs.scene==='gaara'&&!modal&&document.querySelector('.brand small').textContent.includes('піску'));
 check('no horizontal viewport overflow',document.documentElement.scrollWidth<=innerWidth);
 Object.assign(prefs,before);persist();renderDock();setScene(prefs.scene,false,false);applyMotion();ambience();document.querySelector('#dock .app-card').focus();
 return {passed:results.length,results,viewport:[innerWidth,innerHeight]};
})()
