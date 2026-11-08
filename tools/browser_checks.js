(async()=>{
 const results=[];
 const check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw new Error(name+': '+detail);};
 const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)],click=s=>$(s).click(),prefs=()=>JSON.parse(localStorage.getItem('konoha.preferences.v1'));
 const buttons=root=>[...root.querySelectorAll('button,input,select')].filter(e=>e.getClientRects().length&&!e.disabled);
 const graph=root=>{const all=buttons(root),seen=new Set([all[0]]),queue=[all[0]];while(queue.length){const origin=queue.shift();for(const key of['ArrowLeft','ArrowRight','ArrowUp','ArrowDown']){origin.focus();window.KonohaRemote(key);const next=document.activeElement;if(all.includes(next)&&!seen.has(next)){seen.add(next);queue.push(next);}}}return[seen.size,all.length];};
 if(!$('#modal').hidden)window.KonohaRemote('Escape');
 check('prototype initialized',window.KonohaReady===true);
 check('three Naruto scenes',$$('.scene-choice').length===3);
 for(const b of $$('.scene-choice')){b.click();check('scene '+b.dataset.scene,prefs().scene===b.dataset.scene&&b.getAttribute('aria-pressed')==='true');}
 click('[data-scene="dusk"]');
 const first=$('#dock .app-card');first.focus();check('chakra focus visible',!$('#chakra-focus').hidden&&first.classList.contains('is-focus'));
 window.KonohaRemote('ArrowRight');check('D-pad moves app focus',document.activeElement.dataset.package!==first.dataset.package&&!$('#chakra-focus').hidden);
 click('[data-action="settings"]');check('modal isolates home',!$('#modal').hidden&&$('#shell').inert);
 click('[data-pref="motion"]');check('motion toggle stops animation',document.body.classList.contains('motion-off')&&!prefs().motion);
 click('[data-pref="motion"]');check('motion toggle resumes',prefs().motion);
 $('#volume').value='30';$('#volume').dispatchEvent(new Event('input',{bubbles:true}));check('volume persists',prefs().volume===.3&&$('#volume-value').textContent==='30%');
 $('#rotation').value='60';$('#rotation').dispatchEvent(new Event('change',{bubbles:true}));check('rotation persists',prefs().rotation===60);
 $('#idle').value='300';$('#idle').dispatchEvent(new Event('change',{bubbles:true}));check('idle persists',prefs().idle===300);
 const sg=graph($('.scroll-panel'));check('D-pad reaches settings',sg[0]===sg[1],sg.join('/'));
 $('#rotation').value='0';$('#rotation').dispatchEvent(new Event('change',{bubbles:true}));$('#idle').value='0';$('#idle').dispatchEvent(new Event('change',{bubbles:true}));$('#volume').value='45';$('#volume').dispatchEvent(new Event('input',{bubbles:true}));
 window.KonohaRemote('Escape');click('[data-action="search"]');
 const input=$('#app-search');input.value='Kodi';input.dispatchEvent(new Event('input',{bubbles:true}));
 check('library search',$$('#library-grid .app-card').length===1&&$('#library-grid .app-name').textContent==='Kodi');
 input.focus();input.setSelectionRange(4,4);window.KonohaRemote('ArrowLeft',true);check('native search cursor',input.selectionStart===3);
 window.KonohaRemote('Enter');check('search Enter focuses result',document.activeElement.dataset.action==='launch');
 const pinned=prefs().pins.includes('preview.kodi');click('#library-grid [data-action="pin"]');check('unpin persists',prefs().pins.includes('preview.kodi')!==pinned);click('#library-grid [data-action="pin"]');check('repin persists',prefs().pins.includes('preview.kodi')===pinned);
 input.value='x-неіснуючий';input.dispatchEvent(new Event('input',{bubbles:true}));check('empty search guidance',!!$('#library-grid .empty'));
 window.KonohaRemote('Escape');if(!$('#modal').hidden)window.KonohaRemote('Escape');
 click('[data-action="ambient"]');check('ambient opens',!$('#ambient').hidden&&document.body.classList.contains('ambient-on'));
 window.KonohaRemote('Enter');check('ambient closes', $('#ambient').hidden&&!$('#shell').inert);
 const dg=graph($('#shell'));check('D-pad reaches home',dg[0]===dg[1],dg.join('/'));
 check('all images decoded',$$('img').every(i=>i.complete&&i.naturalWidth>0));
 for(const name of['chakra-focus','chakra-confirm','scroll-back','scene-wind','konoha-air']){const a=new Audio('../assets/audio/'+name+'.wav');await new Promise((resolve,reject)=>{a.addEventListener('loadedmetadata',resolve,{once:true});a.addEventListener('error',()=>reject(new Error(name)),{once:true});a.load();});check('audio decodes '+name,a.duration>0);}
 check('canvas has live drawing',!!$('#chakra-canvas').getContext('2d')&&$('#chakra-canvas').width>0);
 check('no horizontal viewport overflow',document.documentElement.scrollWidth===innerWidth);
 check('TV footer in viewport',$('footer').getBoundingClientRect().bottom<=innerHeight+1);
 click('[data-scene="dusk"]');$('.brand').focus();
 return{passed:results.length,total:results.length,viewport:{width:innerWidth,height:innerHeight},results};
})()
