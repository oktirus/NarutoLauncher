(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+JSON.stringify(detail));};
 const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)],raf=()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))),key='konoha.preferences.v1',saved=localStorage.getItem(key),originalPins=[...prefs.pins];
 const packages=apps.slice(0,8).map(a=>a.package),same=(a,b)=>JSON.stringify(a)===JSON.stringify(b),order=()=>$$('.order-row').map(e=>e.dataset.orderPackage),dock=()=>$$('#dock [data-package]').map(e=>e.dataset.package);
 try{
  prefs.audio=false;prefs.ambience=false;prefs.rotation=0;prefs.idle=0;applyTimers();if(modal)closePanel();if(ambient)leaveAmbient();
  check('ready',KonohaReady);check('no village kicker',!$('.hero-kicker'));check('no quote link',!$('#quote-source'));check('clock no background',getComputedStyle($('.clock')).backgroundColor==='rgba(0, 0, 0, 0)');check('new heading',$('#apps-title').textContent==='Сувої розваг');
  prefs.pins=[...packages];renderDock();$('#dock [data-action="library"]').focus();$('#dock [data-action="library"]').click();check('archive entry',modal==='library'&&$('[data-action="order"]'));
  $('[data-action="order"]').focus();$('[data-action="order"]').click();await raf();check('eight visible favorites',order().length===8);check('first up disabled',$('.order-row:first-child [data-action="order-up"]').disabled);check('last down disabled',$('.order-row:last-child [data-action="order-down"]').disabled);check('initial focus enabled arrow',document.activeElement.matches('.order-move:not(:disabled)'));
  let btn=$$('.order-move').find(b=>b.dataset.package===packages[6]&&b.dataset.action==='order-up');btn.focus();KonohaRemote('Enter');let expected=[...packages];[expected[5],expected[6]]=[expected[6],expected[5]];
  check('OK moves seventh into dock',same(prefs.pins,expected)&&same(dock(),expected.slice(0,6)));check('focus follows moved row',document.activeElement.dataset.package===packages[6]&&document.activeElement.dataset.action==='order-up');check('membership unchanged',same([...prefs.pins].sort(),[...packages].sort()));check('stored order',same(JSON.parse(localStorage.getItem(key)).pins,expected));check('status announces position',$('#order-status').textContent.includes('позиція 6 із 8'));check('archive stays final',$('#dock').lastElementChild.dataset.action==='library');
  const before=[...prefs.pins];for(const arrow of ['ArrowLeft','ArrowRight','ArrowDown','ArrowUp'])KonohaRemote(arrow);check('navigation alone does not reorder',same(prefs.pins,before));
  const controlsNow=controls(),seen=new Set([controlsNow[0]]),queue=[controlsNow[0]];while(queue.length){const origin=queue.shift();for(const arrow of ['ArrowLeft','ArrowRight','ArrowUp','ArrowDown']){origin.focus();KonohaRemote(arrow);const next=document.activeElement;if(controlsNow.includes(next)&&!seen.has(next)){seen.add(next);queue.push(next);}}}check('every manager control reachable',seen.size===controlsNow.length,[seen.size,controlsNow.length]);
  moveFavorite(prefs.pins[0],-1);moveFavorite(prefs.pins.at(-1),1);check('boundary moves no-op',same(prefs.pins,before));
  closePanel();check('close restores archive focus',document.activeElement.matches('#dock [data-action="library"]'));
  prefs.pins=[packages[0],'missing.app',packages[1],packages[2]];openPanel('order');moveFavorite(packages[1],-1);check('missing package retained',same(prefs.pins,[packages[1],'missing.app',packages[0],packages[2]]));check('no missing row',order().length===3);closePanel();
  prefs.pins=[packages[0]];openPanel('order');check('single row both disabled',$$('.order-move:disabled').length===2);await raf();check('single row back focus',document.activeElement.dataset.action==='library');closePanel();
  prefs.pins=[];renderDock();openPanel('order');check('empty no arrows',order().length===0&&!!$('.empty'));await raf();check('empty back focus',document.activeElement.dataset.action==='library');$('[data-action="library"]',$('.scroll-panel'));
  $('.scroll-panel [data-action="library"]').click();check('empty back opens archive',modal==='library'&&$$('.library-item').length===19);closePanel();
  check('no horizontal overflow',document.documentElement.scrollWidth<=innerWidth);
  return{viewport:[innerWidth,innerHeight],passed:results.length,results};
 }finally{if(modal)closePanel();prefs.pins=originalPins;renderDock();if(saved===null)localStorage.removeItem(key);else localStorage.setItem(key,saved);}
})()
