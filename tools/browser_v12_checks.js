(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);};
 const wait=ms=>new Promise(r=>setTimeout(r,ms)),before={...prefs,pins:[...prefs.pins]};if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=false;ambience();
 const rows=await fetch('../revisions/v12/asset-manifest.json').then(r=>r.json());
 check('seventeen unique scenes',scenes.length===17&&new Set(scenes.map(s=>s.id)).size===17);
 check('twenty v11 icons retained',Object.keys(KonohaIconSources).length===20&&Object.values(KonohaIconSources).every(u=>u.includes('/v11/')));
 for(const row of rows){
  const img=new Image();img.src='../'+row.file;await img.decode();check(row.id+' new image decodes',img.naturalWidth===1920&&img.naturalHeight===1080);
  const bytes=await fetch(img.src).then(r=>r.arrayBuffer()),hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))].map(v=>v.toString(16).padStart(2,'0')).join('');check(row.id+' exact served bytes',hash===row.sha256);
  const s=scenes.find(s=>s.id===row.id);check(row.id+' scene points to v12 art',s.image==='v12/scene-'+row.id+'.jpg?v='+hash.slice(0,12));
  setScene(s.id);await decodeScene(s);check(row.id+' saved selection and title',document.body.dataset.scene===s.id&&document.querySelector('#hero-title').innerHTML===s.title&&JSON.parse(localStorage.getItem('konoha.preferences.v1')).scene===s.id);
  const r=document.querySelector('#hero-title').getBoundingClientRect();check(row.id+' title fits',r.left>=0&&r.right<=innerWidth&&r.bottom<document.querySelector('.scene-strip').getBoundingClientRect().bottom);
  check(row.id+' correct village',document.querySelector('.brand small').textContent.includes(s.id==='gaara'?'піску':'листі'));
 }
 openPanel('scenes');await wait(40);const cards=[...document.querySelectorAll('.scene-card')];await Promise.all(cards.map(c=>c.querySelector('img').decode()));check('seventeen thumbnails decode',cards.length===17&&cards.every(c=>c.querySelector('img').naturalWidth>0));
 document.querySelector('[data-scene=sai].scene-card').click();check('new Sai can be selected from gallery',prefs.scene==='sai'&&!modal&&document.activeElement.matches('[data-action=scenes],#dock .app-card,.nav-tab,.scene-choice'));
 openPanel('settings');check('dynamic scene count shown',document.querySelector('#panel-content').textContent.includes('Три настрої Наруто й 14 шпалер героїв.')&&!document.querySelector('#panel-content').textContent.includes('${'));closePanel();
 check('no horizontal overflow',document.documentElement.scrollWidth<=innerWidth);
 Object.assign(prefs,before);persist();renderDock();setScene(prefs.scene,false,false);applyMotion();ambience();document.querySelector('#dock .app-card').focus();
 return {passed:results.length,results,viewport:[innerWidth,innerHeight]};
})()
