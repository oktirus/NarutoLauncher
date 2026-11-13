(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+JSON.stringify(detail));};
 const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)],click=s=>$(s).click(),raf=()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));
 if(modal)closePanel();if(ambient)leaveAmbient();prefs.audio=false;prefs.ambience=false;prefs.rotation=0;applyTimers();prefs.motion=true;applyMotion();await techniques.ready;
 check('initialized',KonohaReady);check('18 scenes',scenes.length===18);check('10 techniques',techniques.list.length===10);check('gallery glyph replaces chevron',$('.hero-wallpapers use').getAttribute('href')==='#wallpapers'&&getComputedStyle($('.hero-wallpapers'),'::after').content==='none');
 for(const [name,selector,count] of [['one wallpaper picker','#shell [data-action="scenes"]',1],['archive icon only','#shell [data-action="library"]',1],['game icon only','#shell [data-action="game"]',1],['no tabs','.nav-tab',0],['no right captions','.scene-caption',0],['no audio invitation','.audio-invitation',0],['no duplicated ambient CTA','#shell [data-action="ambient"]',0],['no mood strip','.scene-strip',0],['no hero CTA','.hero-actions',0],['no footer slogan','.footer-quote',0]])check(name,$$(selector).length===count);
 check('archive path is tile',$('#shell [data-action="library"]').classList.contains('app-card'));
 check('game accessible icon',$('.game-shortcut').getAttribute('aria-label').includes('Мінігра')&&$('.game-shortcut use').getAttribute('href')==='#gamepad');
 await $('.naruto-logo').decode();check('drawn logo decoded',$('.naruto-logo').naturalWidth>500&&$('.naruto-logo').alt==='Naruto');
 const manifest=await fetch('../revisions/v18/asset-manifest.json').then(r=>r.json()),changed=new Set(manifest.filter(r=>r.id!=='logo').map(r=>r.id));
 const rects=el=>{const out=[],walker=document.createTreeWalker(el,NodeFilter.SHOW_TEXT);let node;while(node=walker.nextNode()){if(node.textContent.trim()){const r=document.createRange();r.selectNodeContents(node);out.push(...r.getClientRects());}}return out;};
 for(const scene of scenes){
  setScene(scene.id,false,false);await decodeScene(scene);await raf();
  const image=new Image();image.src='../assets/runtime/'+scene.image;await image.decode();check(scene.id+' panorama decoded',image.naturalWidth===1920&&image.naturalHeight===1080);
  if(changed.has(scene.id)){const row=manifest.find(r=>r.id===scene.id);const bytes=await fetch(image.src).then(r=>r.arrayBuffer()),hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))].map(v=>v.toString(16).padStart(2,'0')).join('');check(scene.id+' correct v18 bytes',hash===row.sha256&&scene.image.includes(hash.slice(0,12)));}
  check(scene.id+' quote and author',$('#hero-description').textContent==='«'+KonohaQuotes[scene.id].text+'»'&&$('#quote-author').textContent===KonohaQuotes[scene.id].character);
  check(scene.id+' no quote source button',!$('#quote-source')); 
  check(scene.id+' one picker label',$('.hero-wallpapers').textContent===scene.name&&$('.hero-wallpapers').getAttribute('aria-label').includes(scene.name));
  check(scene.id+' title stays in viewport',rects($('#hero-title')).every(r=>r.left>=0&&r.right<=innerWidth+1));
  check(scene.id+' picker stays inside viewport',$('.hero-wallpapers').getBoundingClientRect().right<=innerWidth);check(scene.id+' picker above apps',$('.hero-wallpapers').getBoundingClientRect().bottom<$('.section-heading').getBoundingClientRect().top);
 }
 check('training replaced boat',scenes.find(s=>s.id==='night').name==='Тренування Расенсюрикена');
 $('.hero-wallpapers').focus();click('.hero-wallpapers');check('picker opens all scenes',modal==='scenes'&&$$('.scene-card').length===18);
 click('.scene-card[data-scene="sakura"]');check('selection and focus restored',prefs.scene==='sakura'&&!modal&&document.activeElement===$('.hero-wallpapers'));
 click('#shell .app-card[data-action="library"]');check('archive opens library',modal==='library'&&$$('.library-item').length===19);closePanel();
 click('.game-shortcut');await raf();check('game icon opens playable game',modal==='game'&&KonohaRunner.snapshot()!==null);closePanel();
 click('[data-action="settings"]');check('settings counts four Naruto scenes',$('#panel-content').textContent.includes('4 сцени Наруто'));check('settings has effects, music and idle',$('[data-pref="audio"]')&&$('[data-pref="ambience"]')&&$('#idle')&&$('[data-action="ambient"]'));
 const rotation=prefs.rotation;$('#rotation').focus();KonohaRemote('ArrowDown');check('settings select vertical navigation preserved',prefs.rotation===rotation&&document.activeElement.id==='idle');
 click('[data-action="ambient"]');check('ambient remains accessible in settings',ambient&&!modal);KonohaRemote('Escape');check('ambient exits',!ambient);
 setTechnique('chidori',false);$('#dock .app-card').focus();const first=document.activeElement;KonohaRemote('ArrowRight');check('D-pad changes app',document.activeElement.matches('#dock .app-card')&&document.activeElement!==first);KonohaRemote('Enter');check('OK uses selected app',$('#toast').textContent.startsWith(document.activeElement.querySelector('.app-name').textContent));
 const c=document.createElement('canvas');c.width=c.height=320;const ctx=c.getContext('2d'),digest=()=>{let h=0;for(const x of ctx.getImageData(0,0,320,320).data)h=(Math.imul(h,31)+x)|0;return h;};
 let images=0,strokes=0;const drawImage=ctx.drawImage.bind(ctx),stroke=ctx.stroke.bind(ctx);ctx.drawImage=(...args)=>{images++;return drawImage(...args);};ctx.stroke=()=>{strokes++;return stroke();};
 const hashes=[];for(const t of [0,300,920,1800]){ctx.clearRect(0,0,320,320);techniques.draw(ctx,'chidori',160,160,90,t);hashes.push(digest());}
 check('Chidori uses no static image',images===0);check('Chidori entire effect has moving strokes',strokes>=800&&new Set(hashes).size===4);check('Chidori concentrated center',ctx.getImageData(156,156,8,8).data.some((v,i)=>i%4===3&&v>200));
 prefs.motion=false;applyMotion();$('#dock .app-card').focus();await raf();const canvas=$('#technique-canvas'),staticA=canvas.toDataURL();await raf();check('motion off freezes complete Chidori',frame===0&&canvas.toDataURL()===staticA);prefs.motion=true;applyMotion();
 const graph=root=>{const all=[...root.querySelectorAll('button,input,select,a[href]')].filter(e=>e.getClientRects().length&&!e.disabled),seen=new Set([all[0]]),queue=[all[0]];while(queue.length){const origin=queue.shift();for(const key of ['ArrowLeft','ArrowRight','ArrowUp','ArrowDown']){origin.focus();KonohaRemote(key);const next=document.activeElement;if(all.includes(next)&&!seen.has(next)){seen.add(next);queue.push(next);}}}return [seen.size,all.length];};
 const mainGraph=graph($('#shell'));check('all main controls reachable by D-pad',mainGraph[0]===mainGraph[1],mainGraph);
 click('.hero-wallpapers');const sceneGraph=graph($('.scroll-panel'));check('all scenes reachable by D-pad',sceneGraph[0]===sceneGraph[1],sceneGraph);closePanel();
 const originalInterval=window.setInterval,originalTimeout=window.setTimeout;let rotateCallback,idleCallback;
 try{
  window.setInterval=(fn,ms,...args)=>{if(ms===60000)rotateCallback=fn;return originalInterval(fn,ms,...args);};prefs.rotation=60;applyTimers();
  const previous=prefs.scene;check('rotation timer remains configured',typeof rotateCallback==='function');rotateCallback();check('rotation works without old mood buttons',prefs.scene!==previous);
  window.setTimeout=(fn,ms,...args)=>{if(ms===300000)idleCallback=fn;return originalTimeout(fn,ms,...args);};prefs.idle=300;resetIdle();
  check('idle timer remains configured',typeof idleCallback==='function');idleCallback();check('automatic quiet mode still works',ambient);leaveAmbient();
 }finally{window.setInterval=originalInterval;window.setTimeout=originalTimeout;prefs.rotation=0;prefs.idle=0;applyTimers();}
 check('no horizontal overflow',document.documentElement.scrollWidth<=innerWidth);if(innerWidth>=900)check('TV footer visible',$('footer').getBoundingClientRect().bottom<=innerHeight+1);
 setScene('dusk',false,false);setTechnique('chidori',false);$('#dock .app-card').focus();
 return {viewport:[innerWidth,innerHeight],passed:results.length,results};
})()
