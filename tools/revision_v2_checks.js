(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+detail);};
 const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)],click=s=>$(s).click(),wait=ms=>new Promise(r=>setTimeout(r,ms)),stored=()=>JSON.parse(localStorage.getItem('konoha.preferences.v1'));
 const digest=canvas=>{const bytes=canvas.getContext('2d').getImageData(0,0,canvas.width,canvas.height).data;let hash=2166136261,count=0;for(let i=0;i<bytes.length;i++){hash=Math.imul(hash^bytes[i],16777619);if(i%4===3&&bytes[i])count++;}return {hash:hash>>>0,count};};
 if(!$('#modal').hidden)window.KonohaRemote('Escape');
 check('nine technique definitions',window.KonohaTechniques.list.length===9);
 check('neutral hero copy',$('#hero-title').textContent==='Волявогню.');
 check('UAKino describes cinema',$('[data-kind="uakino"] .app-meta').textContent==='Кіно українською');
 check('no possessive address on home',!/(твоя|твій|твої|своєму|мої)/i.test($('#shell').innerText));
 const hashes=[];
 for(const t of window.KonohaTechniques.list){
  const sample=document.createElement('canvas');sample.width=320;sample.height=210;const context=sample.getContext('2d');
  window.KonohaTechniques.draw(context,t.id,160,100,64,0);const first=digest(sample);context.clearRect(0,0,320,210);window.KonohaTechniques.draw(context,t.id,160,100,64,1900);const second=digest(sample);
  check(t.id+' has visible geometry',first.count>100);check(t.id+' actually animates',first.hash!==second.hash);hashes.push(first.hash);
  click('[data-action="techniques"]');await wait(40);
  check(t.id+' chooser has nine live previews',$$('[data-preview]').length===9&&$$('[data-preview]').every(c=>digest(c).count>100));
  click('[data-action="technique"][data-technique="'+t.id+'"]');await wait(60);
  check(t.id+' selection persists',stored().technique===t.id&&document.body.dataset.technique===t.id&&$('#modal').hidden);
  check(t.id+' returns to dock',document.activeElement.matches('#dock .app-card'));
  check(t.id+' live effect visible',digest($('#technique-canvas')).count>100);
  window.KonohaRemote('ArrowRight');const pkg=document.activeElement.dataset.package;window.KonohaRemote('Enter');
  check(t.id+' remote opens selected app',$('#toast').textContent.startsWith(document.activeElement.querySelector('.app-name').textContent)&&pkg);
 }
 check('all nine techniques are visually distinct',new Set(hashes).size===9);
 click('[data-action="techniques"]');await wait(40);
 const hover=$('[data-technique="sharingan"]');hover.dispatchEvent(new PointerEvent('pointerover',{bubbles:true,pointerType:'mouse',clientX:17,clientY:19}));check('technique hover synchronizes OK target',document.activeElement===hover);
 const all=[$('[data-action="close"]'),...$$('.technique-card')],seen=new Set([all[0]]),queue=[all[0]];
 while(queue.length){const origin=queue.shift();for(const key of['ArrowLeft','ArrowRight','ArrowUp','ArrowDown']){origin.focus();window.KonohaRemote(key);const next=document.activeElement;if(all.includes(next)&&!seen.has(next)){seen.add(next);queue.push(next);}}}
 check('D-pad reaches every technique and close',seen.size===all.length,seen.size+'/'+all.length);
 click('[data-action="technique"][data-technique="rasengan"]');
 click('[data-action="settings"]');await wait(30);
 const rotationBefore=stored().rotation;$('#rotation').focus();window.KonohaRemote('ArrowDown');check('select Down navigates without edit',stored().rotation===rotationBefore&&document.activeElement.id==='idle');
 const musicBefore=stored().music;$('#music').focus();window.KonohaRemote('ArrowDown');check('music Down navigates without edit',stored().music===musicBefore&&document.activeElement.id==='music-volume');
 $('#music').focus();window.KonohaRemote('ArrowRight');check('music Right switches track',stored().music!==musicBefore);window.KonohaRemote('ArrowLeft');
 const oldEffects=stored().volume;$('#music-volume').value='15';$('#music-volume').dispatchEvent(new Event('input',{bubbles:true}));check('music volume persists independently',stored().musicVolume===.15&&stored().volume===oldEffects);
 click('[data-pref="motion"]');window.KonohaRemote('Escape');$('#dock .app-card').focus();await wait(40);const staticBefore=digest($('#technique-canvas'));await wait(140);const staticAfter=digest($('#technique-canvas'));
 check('reduced-motion effect remains visible',staticBefore.count>100);check('reduced-motion frame is static',staticBefore.hash===staticAfter.hash&&frame===0);
 click('[data-action="techniques"]');await wait(40);const previews=$$('[data-preview]').map(digest);await wait(90);check('reduced-motion previews freeze',$$('[data-preview]').every((c,i)=>digest(c).hash===previews[i].hash));
 window.KonohaRemote('Escape');click('[data-action="settings"]');click('[data-pref="motion"]');window.KonohaRemote('Escape');
 const audioContext=new AudioContext();
 const audioManifest=await fetch('../revisions/v2/audio-manifest.json').then(r=>r.json());
 for(const row of audioManifest.cues){const data=await fetch('../'+row.file+'?v=3').then(r=>r.arrayBuffer());const decoded=await audioContext.decodeAudioData(data);let energy=0;for(const sample of decoded.getChannelData(0))energy+=sample*sample;check(row.file.split('/').at(-1)+' decodes and is non-silent',decoded.duration>.15&&energy>0.001);}
 for(const id of['1584791975','1584792236','1584792240']){const data=await fetch('../assets/audio/naruto/'+id+'.m4a').then(r=>r.arrayBuffer());const decoded=await audioContext.decodeAudioData(data);check('official music preview '+id+' decodes',decoded.duration>=29&&decoded.duration<=31,decoded.duration);}
 await audioContext.close();
 const visualManifest=await fetch('../research/asset-manifest.json').then(r=>r.json());
 for(const scene of scenes){const path='assets/runtime/'+scene.image.split('?')[0],entry=visualManifest.find(r=>r.runtime===path);const bytes=await fetch('../'+path+'?v='+entry.runtime_sha256.slice(0,12)).then(r=>r.arrayBuffer());const hash=[...new Uint8Array(await crypto.subtle.digest('SHA-256',bytes))].map(x=>x.toString(16).padStart(2,'0')).join('');check('scene '+scene.id+' serves corrected bytes',hash===entry.runtime_sha256&&scene.image.includes(entry.runtime_sha256.slice(0,12)));}
 window.YoruPause();check('pause stops music and frame',air.paused&&frame===0);window.YoruRefresh();
 check('viewport has no horizontal overflow',document.documentElement.scrollWidth<=innerWidth);
 if(innerWidth>=900)check('TV footer is visible',$('footer').getBoundingClientRect().bottom<=innerHeight+2);
 if(!$('#modal').hidden)window.KonohaRemote('Escape');
 return {viewport:{width:innerWidth,height:innerHeight},passed:results.filter(r=>r.passed).length,total:results.length,techniqueHashes:hashes,results};
})()
