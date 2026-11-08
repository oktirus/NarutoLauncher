(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);};
 const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)],click=s=>$(s).click(),wait=ms=>new Promise(r=>setTimeout(r,ms)),stored=()=>JSON.parse(localStorage.getItem('konoha.preferences.v1'));
 const digest=canvas=>{const bytes=canvas.getContext('2d').getImageData(0,0,canvas.width,canvas.height).data;let hash=2166136261,count=0;for(let i=0;i<bytes.length;i++){hash=Math.imul(hash^bytes[i],16777619);if(i%4===3&&bytes[i])count++;}return {hash:hash>>>0,count};};
 const graph=root=>{const all=[...root.querySelectorAll('button,input,select,a[href]')].filter(e=>e.getClientRects().length&&!e.disabled),seen=new Set([all[0]]),queue=[all[0]];while(queue.length){const origin=queue.shift();for(const key of['ArrowLeft','ArrowRight','ArrowUp','ArrowDown']){origin.focus();KonohaRemote(key);const next=document.activeElement;if(all.includes(next)&&!seen.has(next)){seen.add(next);queue.push(next);}}}return[seen.size,all.length];};
 if(modal)closePanel();if(ambient)leaveAmbient();if(!prefs.motion){openPanel('settings');click('[data-pref="motion"]');closePanel();}await techniques.ready;
 check('initialized',KonohaReady);check('twelve wallpaper choices',scenes.length===12);check('ten technique choices',techniques.list.length===10);check('all eleven generated plates/sprites decoded',Object.values(techniques.artStatus()).filter(Boolean).length===11);
 const manifest=await fetch('../research/asset-manifest.json').then(r=>r.json());manifest.push(...(await fetch('../revisions/v8/asset-manifest.json').then(r=>r.json())).filter(r=>r.kind==='wallpaper').map(r=>({runtime:r.file,runtime_sha256:r.sha256})));
 for(const scene of scenes){
  setScene(scene.id);await decodeScene(scene);const image=new Image();image.src='../assets/runtime/'+scene.image;await image.decode();
  check(scene.id+' panorama decodes',image.naturalWidth>=1600&&image.naturalHeight>=900);
  check(scene.id+' preference and title',stored().scene===scene.id&&$('#hero-title').innerHTML===scene.title);
  const entry=manifest.find(r=>r.runtime==='assets/runtime/'+scene.image.split('?')[0]),bytes=await fetch(image.src).then(r=>r.arrayBuffer()),hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))].map(v=>v.toString(16).padStart(2,'0')).join('');
  check(scene.id+' corrected bytes and cache key',hash===entry.runtime_sha256&&scene.image.includes(entry.runtime_sha256.slice(0,12)));
  const title=$('#hero-title').getBoundingClientRect();check(scene.id+' title fits viewport',title.right<innerWidth&&title.left>=0);
 }
 setScene('dusk');check('neutral branding',!/(твоя|твій|твої|мої)/i.test($('#shell').innerText));check('UAKino cinema description',$('[data-kind="uakino"] .app-meta').textContent==='Кіно українською');
 click('[data-action="scenes"]');check('twelve selectable wallpaper cards',$$('.scene-card').length===12);const sceneGraph=graph($('.scroll-panel'));check('D-pad reaches all wallpapers',sceneGraph[0]===sceneGraph[1],sceneGraph.join('/'));closePanel();
 const hashes=[];
 for(const t of techniques.list){
  const canvas=document.createElement('canvas');canvas.width=320;canvas.height=210;const c=canvas.getContext('2d');techniques.draw(c,t.id,160,100,64,0,0,true);const first=digest(canvas);c.clearRect(0,0,320,210);techniques.draw(c,t.id,160,100,64,1900,0,true);const second=digest(canvas);
  check(t.id+' detailed art is visible',first.count>200);check(t.id+' has actual motion',first.hash!==second.hash);hashes.push(first.hash);
  click('[data-action="techniques"]');await wait(25);click('[data-action="technique"][data-technique="'+t.id+'"]');await wait(35);
  check(t.id+' selection is saved',stored().technique===t.id&&document.body.dataset.technique===t.id&&$('#modal').hidden);
  check(t.id+' returns to an app',document.activeElement.matches('#dock .app-card'));
 }
 check('ten distinct rendered effects',new Set(hashes).size===10);
 click('[data-action="techniques"]');await wait(30);check('ten live chooser previews',$$('[data-preview]').length===10&&$$('[data-preview]').every(c=>digest(c).count>200));
 const hover=$('[data-technique="sharingan"]');hover.dispatchEvent(new PointerEvent('pointerover',{bubbles:true,pointerType:'mouse',clientX:17,clientY:19}));check('hover synchronizes OK target',document.activeElement===hover);
 const techGraph=graph($('.scroll-panel'));check('D-pad reaches all techniques and references',techGraph[0]===techGraph[1],techGraph.join('/'));
 click('[data-action="references"]');await wait(60);
 for(const t of techniques.list){$('#reference-technique').value=t.id;$('#reference-technique').dispatchEvent(new Event('change',{bubbles:true}));await wait(35);const images=$$('.anime-reference img');await Promise.all(images.map(i=>i.decode()));check(t.id+' has three anime references',images.length===3&&$$('.anime-reference a').every(a=>a.getAttribute('href').startsWith('https://')));}
 click('[data-action="techniques"]');await wait(20);click('[data-action="technique"][data-technique="gamabunta"]');$('#dock .app-card').focus();
 KonohaRemote('ArrowRight');const start=techniques.position();await wait(110);const mid=techniques.position();check('Gamabunta has a real airborne arc',mid.airborne&&mid.x>start.x+1&&mid.y<start.y-5,JSON.stringify({start,mid,motion:prefs.motion}));
 KonohaRemote('ArrowRight');const redirected=techniques.position();check('redirect starts at current airborne position',Math.hypot(redirected.x-mid.x,redirected.y-mid.y)<7&&redirected.airborne);
 await wait(760);const landed=techniques.position();check('Gamabunta lands on focused emblem',Math.abs(landed.x-(focusCenter.x+focusCenter.r*.5))<1&&Math.abs(landed.y-(focusCenter.y-focusCenter.r*.26))<1&&!landed.airborne);
 const selected=document.activeElement.querySelector('.app-name').textContent;KonohaRemote('Enter');check('OK opens the actually selected app',$('#toast').textContent.startsWith(selected));
 $('.brand').focus();check('leaving apps clears Gamabunta and trails',techniques.position()===null&&focusCenter===null&&trails.length===0);
 click('[data-action="settings"]');await wait(20);const rotation=stored().rotation;$('#rotation').focus();KonohaRemote('ArrowDown');check('select Up/Down does not edit rotation',stored().rotation===rotation&&document.activeElement.id==='idle');
 const oldVolume=stored().volume;$('#music-volume').value='15';$('#music-volume').dispatchEvent(new Event('input',{bubbles:true}));check('music volume remains separate',stored().musicVolume===.15&&stored().volume===oldVolume);
 if(!prefs.motion){click('[data-pref="motion"]');}click('[data-pref="motion"]');closePanel();$('#dock .app-card').focus();await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));await wait(40);const staticA=digest($('#technique-canvas'));await wait(100);check('reduced motion preserves static painted focus',frame===0&&staticA.count>200&&digest($('#technique-canvas')).hash===staticA.hash);
 click('[data-action="techniques"]');await wait(30);const snapshots=$$('[data-preview]').map(digest);await wait(100);check('reduced-motion chooser is static',$$('[data-preview]').every((c,i)=>digest(c).hash===snapshots[i].hash));closePanel();click('[data-action="settings"]');click('[data-pref="motion"]');closePanel();
 const previous=getComputedStyle(document.documentElement).getPropertyValue('--px');await wait(120);check('background moves without pointer',getComputedStyle(document.documentElement).getPropertyValue('--px')!==previous);
 YoruPause();check('pause stops motion and music',frame===0&&audioEngine.snapshot().musicNodes.length===0);YoruRefresh();
 check('no horizontal viewport overflow',document.documentElement.scrollWidth<=innerWidth);
 if(innerWidth>=900)check('TV footer remains visible',$('footer').getBoundingClientRect().bottom<=innerHeight+2);
 setTechnique('rasengan');setScene('dusk');$('#dock .app-card').focus();
 return {viewport:{width:innerWidth,height:innerHeight},passed:results.length,total:results.length,techniqueHashes:hashes,results};
})()
