'use strict';
const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const native=typeof window.YoruNative!=='undefined';
const scenes=window.KonohaScenes;
const iconArt=window.KonohaIconSources;
const runtimeAssets=window.KonohaRuntimeAssets;
const smartTubeArt=iconArt.smarttube;
const examples=[
 {package:'preview.smarttube',name:'SmartTube',kind:'smarttube',description:'Відео без поспіху'},
 {package:'preview.lampa',name:'Lampa',kind:'lampa',description:'Кіно на цей вечір'},
 {package:'preview.uakino',name:'UAKino',kind:'uakino',description:'Кіно українською'},
 {package:'preview.netflix',name:'Netflix',kind:'netflix',description:'Ще одна серія'},
 {package:'preview.kodi',name:'Kodi',kind:'kodi',description:'Медіатека'},
 {package:'preview.spotify',name:'Spotify',kind:'spotify',description:'Музика та подкасти'},
 {package:'preview.crunchyroll',name:'Crunchyroll',kind:'crunchyroll',description:'Світ аніме'},
 {package:'preview.vlc',name:'VLC',kind:'vlc',description:'Відеоплеєр'},
 {package:'preview.plex',name:'Plex',kind:'plex',description:'Домашнє кіно'},
 {package:'preview.youtube',name:'YouTube',kind:'youtube',description:'Відео та трансляції',aliases:['com.google.android.youtube.tv']},
 {package:'preview.tvbro',name:'TV Bro',kind:'tvbro',description:'Браузер для телевізора',aliases:['com.phlox.tvwebbrowser','browser']},
 {package:'preview.kyivstar',name:'Київстар ТБ',kind:'kyivstar',description:'Телебачення та кіно',aliases:['kyivstar','київстар','ua.kyivstar.tv']},
 {package:'preview.megogo',name:'MEGOGO',kind:'megogo',description:'Кіно, канали та спорт'},
 {package:'preview.sweettv',name:'SWEET.TV',kind:'sweettv',description:'Канали та кіно українською',aliases:['sweet.tv','sweet tv','tv.sweet']},
 {package:'preview.primevideo',name:'Prime Video',kind:'primevideo',description:'Фільми та серіали',aliases:['prime video','com.amazon.amazonvideo.livingroom']},
 {package:'preview.disneyplus',name:'Disney+',kind:'disneyplus',description:'Історії та анімація',aliases:['disney','com.disney.disneyplus']},
 {package:'preview.jellyfin',name:'Jellyfin',kind:'jellyfin',description:'Особиста медіатека'},
 {package:'preview.sendfiles',name:'Send Files to TV',kind:'sendfiles',description:'Передавання файлів',aliases:['send files','com.yablio.sendfilestotv']},
 {package:'preview.xplore',name:'X-plore',kind:'xplore',description:'Файловий менеджер',aliases:['x-plore','com.lonelycatgames.xplore']}
];
const escapeHTML=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let saved={};try{saved=JSON.parse(localStorage.getItem('konoha.preferences.v1')||'{}')||{};}catch(_){/* Invalid preferences recover to defaults. */}
const techniques=window.KonohaTechniques;
const musicTracks=[{id:'1584791975',name:'The Rising Fighting Spirit'},{id:'1584792236',name:'Strong and Strike'},{id:'1584792240',name:'Naruto Main Theme'}];
const highlightSounds=[{id:'jutsu',name:'Завершення печатки'},{id:'amaterasu',name:'Аматерасу'},{id:'rasenshuriken',name:'Расенсюрикен'},{id:'hiraishin',name:'Хірайшин'},{id:'summoning',name:'Виклик жаби'},{id:'sharingan',name:'Шарінґан'}];
const prefs={scene:'dusk',audio:false,ambience:true,volume:.45,musicVolume:.22,music:'1584791975',technique:'rasengan',highlightSound:'jutsu',motion:true,rotation:0,idle:0,pins:null,...saved};
if(!highlightSounds.some(s=>s.id===prefs.highlightSound))prefs.highlightSound='jutsu';
if(!techniques.list.some(t=>t.id===prefs.technique))prefs.technique='rasengan';
if(!musicTracks.some(t=>t.id===prefs.music))prefs.music=musicTracks[0].id;
prefs.musicVolume=Number.isFinite(Number(prefs.musicVolume))?Math.max(0,Math.min(1,Number(prefs.musicVolume))):.22;
for(const [key,fallback] of Object.entries({audio:false,ambience:true,motion:true}))if(typeof prefs[key]!=='boolean')prefs[key]=fallback;
if(prefs.scene==='night')prefs.scene='team7'; // Replace retired training scene in saved preferences.
if(!scenes.some(s=>s.id===prefs.scene))prefs.scene='dusk';
if(![0,60,180,600].includes(Number(prefs.rotation)))prefs.rotation=0;
if(![0,300,900].includes(Number(prefs.idle)))prefs.idle=0;
prefs.volume=Number.isFinite(Number(prefs.volume))?Math.max(0,Math.min(1,Number(prefs.volume))):.45;
if(!Array.isArray(prefs.pins))prefs.pins=null;else prefs.pins=[...new Set(prefs.pins.filter(value=>typeof value==='string'))];
let apps=[],modal='',ambient=false,paused=false,audioUnlocked=false,lastFocus=null,filterPinned=false,backLayer=0;
let toastTimer,rotationTimer,idleTimer,frame=0,lastTime=0,focusTarget=null,focusCenter=null,sceneVersion=0;
let pointerX=0,pointerY=0,pointerTime=-Infinity;
let pointerClientX=null,pointerClientY=null;
const motionQuery=matchMedia('(prefers-reduced-motion: reduce)');
const canMove=()=>prefs.motion&&!motionQuery.matches;
function persist(){try{localStorage.setItem('konoha.preferences.v1',JSON.stringify(prefs));}catch(_){toast('Цього разу налаштування не вдалося зберегти.');}}
function toast(text){clearTimeout(toastTimer);$('#toast').textContent=text;$('#toast').hidden=false;toastTimer=setTimeout(()=>$('#toast').hidden=true,3600);}
const audioEngine=new window.KonohaAudio.Engine();
const audioReady=()=>audioEngine.unlocked&&audioEngine.context?.state==='running';
audioEngine.onStateChange=()=>{audioUnlocked=audioReady();updateAudioUI();};
function currentTechnique(){return techniques.list.find(t=>t.id===prefs.technique);}
async function sound(type='focus',options={}){
 if(options.signal?.aborted||!prefs.audio||paused)return false;
 if(!audioReady()&&!await unlockAudio())return;
 if(options.signal?.aborted||!prefs.audio||paused)return false;
 const cue=type==='focus'?prefs.highlightSound:type==='confirm'?currentTechnique().cue:'ui';
 try{return await audioEngine.playCue(cue,type==='focus'?'focus':'confirm',options);}catch(_){toast('Звуковий ефект не вдалося відкрити.');return false;}
}
function updateAudioUI(){
 document.body.classList.toggle('audio-on',prefs.audio&&audioReady()&&!paused);
}
function ambience(){
 updateAudioUI();
 audioEngine.configure({audio:prefs.audio,ambience:prefs.ambience,volume:prefs.volume,musicVolume:prefs.musicVolume,music:prefs.music,paused}).catch(()=>toast('Музичне прев’ю не відкрилося. Обери інший трек.'));
}
function preloadTechniqueAudio(){const urls=[window.KonohaCueSources[prefs.highlightSound]?.focus,window.KonohaCueSources[prefs.technique]?.confirm].filter(Boolean).map(v=>v.url);audioEngine.pinCues(urls);if(prefs.audio&&!paused&&audioEngine.context)for(const url of urls)audioEngine.load(url).catch(()=>{});}
async function unlockAudio(){ambience();try{audioUnlocked=await audioEngine.unlock();updateAudioUI();if(audioUnlocked)preloadTechniqueAudio();return audioUnlocked;}catch(_){audioUnlocked=false;updateAudioUI();toast('Браузер ще не дозволив звук. Увімкни його в налаштуваннях.');return false;}}
function clock(){const now=new Date();const time=new Intl.DateTimeFormat('uk-UA',{hour:'2-digit',minute:'2-digit'}).format(now);const date=new Intl.DateTimeFormat('uk-UA',{weekday:'long',day:'numeric',month:'long'}).format(now);$('#time').textContent=time;$('#date').textContent=date;$('#ambient-time').textContent=time;$('#ambient-date').textContent=date;}
const decodedScenes=new Map();
let sceneCleanupTimer=0;
function sceneURL(scene){const pixels=Math.max(innerWidth,innerHeight*16/9)*Math.min(devicePixelRatio||1,2)*1.08;return runtimeAssets.scenes[scene.id][pixels<=1600?'small':'full'];}
function decodeScene(scene){
 const url=sceneURL(scene);if(!decodedScenes.has(url)){const load=()=>{const image=new Image();image.src=url;return image.decode();};const pending=load().catch(()=>load()).catch(error=>{if(decodedScenes.get(url)===pending)decodedScenes.delete(url);throw error;});decodedScenes.set(url,pending);while(decodedScenes.size>2)decodedScenes.delete(decodedScenes.keys().next().value);}
 return decodedScenes.get(url);
}
function preloadNextScene(){if(prefs.rotation&&!paused)decodeScene(scenes[(scenes.findIndex(s=>s.id===prefs.scene)+1)%scenes.length]).catch(()=>{});}
function setScene(id,save=true,feedback=true){
 const scene=scenes.find(s=>s.id===id);if(!scene)return;
 const version=++sceneVersion;
 decodeScene(scene).then(()=>{if(version!==sceneVersion||paused)return;const next=1-backLayer;const layers=[$('#back-a'),$('#back-b')];layers[next].style.backgroundImage=`url('${sceneURL(scene)}')`;layers[next].classList.add('shown');layers[backLayer].classList.remove('shown');backLayer=next;clearTimeout(sceneCleanupTimer);sceneCleanupTimer=setTimeout(()=>{if(version===sceneVersion)layers[1-backLayer].style.backgroundImage='none';},1700);preloadNextScene();}).catch(()=>toast('Панораму не вдалося відкрити. Обери іншу сцену.'));
 prefs.scene=id;document.body.dataset.scene=id;
 const quote=window.KonohaQuotes[id];
 $('#hero-description').textContent='«'+quote.text+'»';
 $('#quote-author').textContent=quote.character;
 document.body.classList.toggle('hero-scene',!!scene.hero);document.documentElement.style.setProperty('--scene-mobile-position',scene.mobilePosition||'65% center');
 $('#hero-title').innerHTML=scene.title;
 const heroes=$('.hero-wallpapers');if(heroes){$('.scene-choice-name',heroes).textContent=scene.name;heroes.setAttribute('aria-label','Обрати шпалери. Поточна сцена: '+scene.name);}
 $$('[data-scene]').forEach(el=>{const active=el.dataset.scene===id;el.classList.toggle('active',active);el.setAttribute('aria-pressed',active);});
 if(save){persist();if(feedback)sound('scene');}
}
function sceneButtons(){
 $('#scene-buttons').innerHTML='<button class="scene-choice hero-wallpapers" data-action="scenes" aria-haspopup="dialog"><svg class="wallpaper-glyph" aria-hidden="true"><use href="#wallpapers"/></svg><span class="scene-choice-name">Обрати шпалери</span></button>';
}
function kindFor(app){const text=(app.name+' '+app.package).toLowerCase();return [...examples].reverse().find(x=>[x.kind,...(x.aliases||[])].some(alias=>text.includes(alias)))?.kind||'';}
function readApps(){
 const focused=document.activeElement?.dataset.package;
 if(native){try{apps=JSON.parse(window.YoruNative.getApps()).map(app=>({...app,kind:kindFor(app),description:'Встановлений застосунок'}));}catch(_){apps=[];toast('Не вдалося прочитати застосунки. Спробуй оновити архів.');}}
 else apps=examples.map(x=>({...x}));
 if(prefs.pins===null){const themed=native?apps.filter(a=>a.kind).sort((a,b)=>examples.findIndex(x=>x.kind===a.kind)-examples.findIndex(x=>x.kind===b.kind)):[];prefs.pins=(themed.length?themed:apps).slice(0,6).map(a=>a.package);persist();}
 renderDock();if(focused&&!modal)$$('#dock [data-package]').find(el=>el.dataset.package===focused)?.focus();
 $('.prototype-tag').innerHTML=native?'<i></i>Коноха':'<i></i>Локальний макет';
}
function appCard(app){
 const sizes=app.kind?runtimeAssets.icons[app.kind]:null,loading=modal==='library'||modal==='search'?'lazy':'eager';
 const art=app.kind?`<img src="${sizes.small}" srcset="${sizes.small} 256w, ${sizes.medium} 384w, ${sizes.full} 640w" sizes="(max-width:900px) 115px, (max-width:1707px) 10.6vw, 181px" loading="${loading}" decoding="async" alt="" draggable="false">`:native&&app.icon?`<img src="${escapeHTML(app.icon)}" loading="${loading}" decoding="async" alt="" draggable="false">`:`<span class="app-fallback">${escapeHTML(app.name.slice(0,2))}</span>`;
 return `<button class="app-card" data-action="launch" data-kind="${escapeHTML(app.kind||'')}" data-package="${escapeHTML(app.package)}" aria-label="Відкрити ${escapeHTML(app.name)}"><span class="app-emblem">${art}</span><span class="app-name">${escapeHTML(app.name)}</span><span class="app-meta">${escapeHTML(app.description||'Застосунок')}</span></button>`;
}
function renderDock(){
 $('#dock').innerHTML=prefs.pins.map(pkg=>apps.find(a=>a.package===pkg)).filter(Boolean).slice(0,6).map(appCard).join('')+'<button class="app-card" data-action="library" aria-label="Весь архів застосунків"><span class="app-emblem"><img src="'+iconArt.archive+'" alt="" draggable="false"></span><span class="app-name">Весь архів</span><span class="app-meta">Усі застосунки</span></button>';
 $('#dock').style.setProperty('--dock-count',$('#dock').childElementCount);
}
let lastLaunchPackage='',lastLaunchTime=-Infinity,pendingLaunch=null;
function clearLaunch(request){
 clearTimeout(request.timer);request.controller.abort();request.button?.removeAttribute('aria-busy');
 if(pendingLaunch===request)pendingLaunch=null;
 clearTimeout(toastTimer);$('#toast').hidden=true;
}
function cancelLaunch(){if(!pendingLaunch)return false;clearLaunch(pendingLaunch);lastLaunchTime=-Infinity;return true;}
function launch(pkg){
 const app=apps.find(a=>a.package===pkg);if(!app||pendingLaunch)return;
 const now=performance.now();if(pkg===lastLaunchPackage&&now-lastLaunchTime<400)return;lastLaunchPackage=pkg;lastLaunchTime=now;
 const open=()=>{if(native)window.YoruNative.launch(pkg);else toast(`${app.name} — у макеті показано оформлення. Запуск на Android потребує native-збірки.`);};
 burst();if(!prefs.audio){open();return;}
 const request={controller:new AbortController(),button:document.activeElement,timer:0};pendingLaunch=request;
 request.button?.setAttribute('aria-busy','true');toast(`${currentTechnique().name}… Back — скасувати`);clearTimeout(toastTimer);
 audioEngine.stopCues('launch-confirm');
 const finish=()=>{if(pendingLaunch!==request)return;lastLaunchTime=performance.now();clearLaunch(request);open();};
 // A broken decoder/context must never trap the user. Normal playback waits for onended.
 request.timer=setTimeout(finish,8000);
 sound('confirm',{waitForEnd:true,signal:request.controller.signal}).then(finish,finish);
}
function openPanel(type){
 window.KonohaRunner?.unmount();$('.scroll-panel').classList.remove('game-panel');
 for(const c of $$('#panel-content canvas'))c.width=c.height=0;techniques.trim?.();
 if(ambient)leaveAmbient();if(!modal)lastFocus=document.activeElement;modal=type;filterPinned=false;$('#modal').hidden=false;$('#shell').inert=true;$('.scroll-panel').classList.toggle('wide',['library','search','techniques','scenes','references'].includes(type));hideFocus();
 $$('.nav-tab').forEach(b=>b.classList.toggle('selected',b.dataset.action===(type==='search'?'library':type)));
 applyMotion();
 if(type==='game'){gamePanel();return;}
 if(type==='settings')settingsPanel();else if(type==='order')orderPanel();else if(type==='scenes')scenesPanel();else if(type==='techniques')techniquesPanel();else if(type==='references')referencesPanel();else libraryPanel();
 requestAnimationFrame(()=>{if(modal===type)(type==='search'||type==='library'?$('#app-search'):type==='techniques'?$('.technique-card[aria-pressed="true"]'):type==='order'?$('.order-move:not(:disabled)')||$('[data-action="library"]',$('.scroll-panel')):$('[data-action="close"]',$('.scroll-panel')))?.focus();});
}
function closePanel(){window.KonohaRunner?.unmount();for(const c of $$('#panel-content canvas'))c.width=c.height=0;$('#panel-content').replaceChildren();techniques.trim?.();$('.scroll-panel').classList.remove('game-panel');hideFocus();modal='';$('#modal').hidden=true;$('#shell').inert=false;$('.scroll-panel').scrollTop=0;$$('.nav-tab').forEach(b=>b.classList.toggle('selected',b.dataset.action==='home'));if(lastFocus?.isConnected)lastFocus.focus();else $('#dock .app-card')?.focus();applyMotion();resetIdle();}
function gamePanel(){
 $('.scroll-panel').classList.add('game-panel');$('#panel-title').textContent='Шлях шинобі';
 window.KonohaRunner.mount($('#panel-content'),{onExit:closePanel,onSound:()=>sound('ui'),reduced:()=>!canMove()});
}
function scenesPanel(){
 $('#panel-title').textContent='Шпалери';
 $('#panel-content').innerHTML='<div class="scene-grid">'+scenes.map(s=>`<button class="scene-card ${s.id===prefs.scene?'active':''}" data-action="scene" data-scene="${s.id}" aria-pressed="${s.id===prefs.scene}"><img src="${runtimeAssets.scenes[s.id].thumb}" loading="lazy" decoding="async" style="object-position:${s.cardPosition||'72% center'}" alt=""><span><strong>${s.name}</strong><small>${s.mood}</small></span></button>`).join('')+'</div><p class="panel-note">Усі сцени створені для цього макета. Вітер, листя й чакра рухаються окремо від панорами. Зміна сцени залишається в межах світу Naruto.</p>';
}
function libraryPanel(){
 $('#panel-title').textContent='Архів застосунків';
 $('#panel-content').innerHTML='<input type="search" class="library-search" id="app-search" placeholder="Знайти застосунок…" aria-label="Пошук застосунків" autocomplete="off"><div class="library-toolbar"><span id="app-count"></span><button class="filter-button" data-action="filter" aria-pressed="false">Лише обране</button><button class="filter-button" data-action="order">Порядок обраного</button></div><div class="library-grid" id="library-grid"></div><p class="panel-note">Печатка під іконкою додає застосунок в обране. «Порядок обраного» змінює послідовність. Перші шість видно на головному екрані. '+(native?'Список з пристрою.':'Це інтерактивний макет: системні дії й запуск застосунків позначено як демонстрацію.')+'</p>';
 $('#app-search').addEventListener('input',renderLibrary);renderLibrary();
}
function renderLibrary(){
 const query=($('#app-search')?.value||'').toLocaleLowerCase('uk');const list=apps.filter(a=>a.name.toLocaleLowerCase('uk').includes(query)&&(!filterPinned||prefs.pins.includes(a.package))).sort((a,b)=>a.name.localeCompare(b.name,'uk'));
 $('#app-count').textContent=`${list.length} ${native?'застосунків':'ілюстрованих застосунків'}`;
 $('#library-grid').innerHTML=list.length?list.map(app=>`<div class="library-item">${appCard(app)}<button class="pin-button ${prefs.pins.includes(app.package)?'pinned':''}" data-action="pin" data-package="${escapeHTML(app.package)}" aria-pressed="${prefs.pins.includes(app.package)}" aria-label="${prefs.pins.includes(app.package)?'Прибрати з обраного':'Додати в обране'} ${escapeHTML(app.name)}"><svg><use href="#pin"/></svg>${prefs.pins.includes(app.package)?'Закріплено':'Закріпити'}</button></div>`).join(''):'<p class="empty">Цього застосунку в архіві немає. Спробуй іншу назву або вимкни фільтр.</p>';
 hideFocus();
}
function orderedFavorites(){return prefs.pins.map(pkg=>apps.find(app=>app.package===pkg)).filter(Boolean);}
function orderPanel(){
 $('#panel-title').textContent='Порядок обраного';
 const list=orderedFavorites();
 $('#panel-content').innerHTML='<p class="panel-note order-instructions">Обери стрілку ↑ або ↓ біля застосунку та натисни OK. Перші шість позицій видно на головному екрані. Зміни зберігаються одразу.</p><div id="order-status" class="order-status" role="status" aria-live="polite">Обраних застосунків: '+list.length+'.</div><ol class="favorite-order">'+list.map((app,i)=>`<li class="order-row ${i<6?'on-home':''}" data-order-package="${escapeHTML(app.package)}"><span class="order-number">${i+1}</span><span class="order-icon">${app.kind?`<img src="${iconArt[app.kind]}" alt="">`:escapeHTML(app.name.slice(0,2))}</span><span class="order-info"><strong>${escapeHTML(app.name)}</strong><small>${i<6?'На головному екрані':'Поза головним екраном'}</small></span><div class="order-buttons"><button class="order-move" data-action="order-up" data-package="${escapeHTML(app.package)}" aria-label="Раніше: ${escapeHTML(app.name)}" ${i===0?'disabled':''}>↑</button><button class="order-move" data-action="order-down" data-package="${escapeHTML(app.package)}" aria-label="Пізніше: ${escapeHTML(app.name)}" ${i===list.length-1?'disabled':''}>↓</button></div></li>`).join('')+'</ol>'+(list.length?'':'<p class="empty">Спочатку закріпи застосунки в архіві.</p>')+'<button class="settings-link" data-action="library">До архіву застосунків<svg><use href="#arrow"/></svg></button>';
}
function moveFavorite(pkg,direction){
 const list=orderedFavorites(),index=list.findIndex(app=>app.package===pkg),next=index+direction;
 if(index<0||next<0||next>=list.length)return;
 const from=prefs.pins.indexOf(pkg),to=prefs.pins.indexOf(list[next].package),pins=[...prefs.pins];
 [pins[from],pins[to]]=[pins[to],pins[from]];prefs.pins=pins;persist();const returnToArchive=lastFocus?.dataset.action==='library';renderDock();if(returnToArchive)lastFocus=$('#dock [data-action="library"]');orderPanel();
 const buttons=$$('.order-move').filter(b=>b.dataset.package===pkg&&!b.disabled),action=direction<0?'order-up':'order-down';
 (buttons.find(b=>b.dataset.action===action)||buttons[0]||$('[data-action="library"]',$('.scroll-panel')))?.focus();
 $('#order-status').textContent=list[index].name+': позиція '+(next+1)+' із '+list.length+(next<6?'. На головному екрані.':'. Поза головним екраном.');sound('ui');
}
function settingsPanel(){
 $('#panel-title').textContent='Шлях ніндзя';
 const toggle=(key,name,description)=>`<div class="settings-row"><div><strong>${name}</strong><p>${description}</p></div><button class="toggle" role="switch" aria-checked="${!!prefs[key]}" data-action="toggle" data-pref="${key}" aria-label="${name}"></button></div>`;
 $('#panel-content').innerHTML=toggle('motion','Живий світ','Листя, parallax, сліди чакри та рухомі печатки.')+toggle('audio','Звуки технік','Короткі аніме-ефекти на виборі та OK.')+toggle('ambience','Атмосфера села','Офіційні прев’ю Naruto OST.')+'<div class="settings-row"><div><strong>Гучність</strong><p>Окремий баланс для цього лаунчера.</p></div><div class="volume-control"><input type="range" id="volume" aria-label="Гучність" min="0" max="100" step="5"><output id="volume-value"></output></div></div><div class="settings-row"><div><strong>Зміна сцен</strong><p>'+scenes.filter(s=>!s.hero).length+' сцени Наруто й '+scenes.filter(s=>s.hero).length+' шпалер героїв.</p></div><select id="rotation" aria-label="Інтервал зміни сцен"><option value="0">Вручну</option><option value="60">Щохвилини</option><option value="180">Кожні 3 хв</option><option value="600">Кожні 10 хв</option></select></div><div class="settings-row"><div><strong>Тиха мить</strong><p>Перехід до спокою без активності.</p></div><select id="idle" aria-label="Час до режиму спокою"><option value="0">Вимкнено</option><option value="300">Через 5 хв</option><option value="900">Через 15 хв</option></select></div><button class="settings-link" data-action="ambient">Увійти в режим спокою<svg><use href="#moon"/></svg></button><button class="settings-link" data-action="system">Налаштування Android<svg><use href="#arrow"/></svg></button><button class="settings-link" data-action="wifi">Мережа й Wi-Fi<svg><use href="#arrow"/></svg></button><button class="settings-link" data-action="refresh">Оновити архів застосунків<svg><use href="#scroll"/></svg></button><button class="settings-link" data-action="about">Про Коноху та ресурси<svg><use href="#shuriken"/></svg></button><p class="panel-note">Звук вмикається після натискання. Системне зменшення анімацій має пріоритет. Час — системний, налаштування — локальні.</p>';
 $('#volume').value=String(Math.round(prefs.volume*100));$('#volume-value').textContent=Math.round(prefs.volume*100)+'%';$('#rotation').value=String(prefs.rotation);$('#idle').value=String(prefs.idle);
 $('#panel-content').insertAdjacentHTML('afterbegin',`<button class="settings-link" data-action="techniques"><span>Техніка навігації: ${escapeHTML(currentTechnique().name)}</span><svg><use href="#shuriken"/></svg></button>`);
 $('[data-pref="audio"]').previousElementSibling.querySelector('p').textContent='Чакра, блискавка та печатки з аніме.';
 $('[data-pref="ambience"]').previousElementSibling.querySelector('strong').textContent='Музика Naruto';
 $('[data-pref="ambience"]').setAttribute('aria-label','Музика Naruto');
 $('[data-pref="ambience"]').previousElementSibling.querySelector('p').textContent='Офіційні прев’ю OST замість синтезованого фону.';
 $('#volume').closest('.settings-row').querySelector('strong').textContent='Гучність ефектів';
 $('#volume').setAttribute('aria-label','Гучність ефектів');
 $('#volume').closest('.settings-row').insertAdjacentHTML('afterend',`<div class="settings-row"><div><strong>Музика</strong><p>Оригінальний Naruto OST · MUSASHI PROJECT</p></div><select id="music" aria-label="Музика Naruto">${musicTracks.map(t=>`<option value="${t.id}">${t.name}</option>`).join('')}</select></div><div class="settings-row"><div><strong>Гучність музики</strong><p>Окремо від звуків навігації.</p></div><div class="volume-control"><input type="range" id="music-volume" aria-label="Гучність музики" min="0" max="100" step="5" value="${Math.round(prefs.musicVolume*100)}"><output id="music-volume-value">${Math.round(prefs.musicVolume*100)}%</output></div></div><button class="settings-link" data-action="sound-ready">Заряджання техніки<svg><use href="#sound"/></svg></button><button class="settings-link" data-action="sound-preview">Використати: ${escapeHTML(currentTechnique().name)}<svg><use href="#sound"/></svg></button><p class="music-credit">Apple Music: офіційні 30-секундні прев’ю OST. <a href="https://music.apple.com/us/album/naruto-original-soundtrack/1584791969" target="_blank" rel="noopener noreferrer">Повний альбом ↗</a><br>Заряджання й використання мають окремі звуки. Репліки з Naruto-кліпів очищені від зайвої тиші та сторонніх кінцівок. Джерела: Myinstants, Voicy, ZEDGE.</p>`);
 $('#music').value=prefs.music;
 $('#panel-content').insertAdjacentHTML('afterbegin',`<div class="settings-row highlight-setting"><div><strong>Звук виділення</strong><p>Один для всіх застосунків. OK — звук обраної техніки.</p></div><select id="highlight-sound" aria-label="Звук виділення застосунку">${highlightSounds.map(s=>`<option value="${s.id}">${s.name}</option>`).join('')}</select></div>`);
 $('#highlight-sound').value=prefs.highlightSound;
 $('[data-action="sound-ready"]').firstChild.textContent='Прослухати звук виділення';
 $('#panel-content .panel-note').textContent='↑↓ — перейти між рядками; ←→ — змінити значення. Звук вмикається після натискання. Системне зменшення анімацій має пріоритет. Час системний, налаштування локальні.';
}
function setTechnique(id,save=true){
 if(!techniques.list.some(t=>t.id===id))return;
 prefs.technique=id;document.body.dataset.technique=id;
 document.documentElement.style.setProperty('--technique-color',currentTechnique().color);
 document.documentElement.style.setProperty('--chakra',currentTechnique().color);
 $('#technique-label').textContent=currentTechnique().name;
 techniques.setActive?.(id);
 preloadTechniqueAudio();
 trails.length=0;bursts.length=0;techniques.resetMotion?.();if(save)persist();
 if(!canMove())drawTechniques(0);
}
function techniquesPanel(){
 $('#panel-title').textContent='Техніки навігації';
 $('#panel-content').innerHTML='<p class="technique-intro">Обери техніку. Вона огортатиме застосунок цільною чакрою. Виділення має один звук із налаштувань; OK — репліка або вивільнення обраної техніки.</p><div class="technique-grid">'+techniques.list.map(t=>`<button class="technique-card" data-action="technique" data-technique="${t.id}" aria-pressed="${prefs.technique===t.id}" style="--card-chakra:${t.color}"><span class="technique-art"><canvas width="320" height="210" data-preview="${t.id}" aria-hidden="true"></canvas><img src="${smartTubeArt}" alt=""></span><span class="technique-name">${t.name}</span><span class="technique-owner">${t.owner}</span><span class="technique-description">${t.description}</span><span class="technique-state">${prefs.technique===t.id?'Обрано':'Обрати техніку'}</span></button>`).join('')+'</div><p class="panel-note">↑↓←→ — порівняти; OK — застосувати. Вибір зберігається. За зменшення анімацій ефекти стають нерухомими. Хірайшин — техніка Мінато, Четвертого хокаге. Расенсюрикен — додатковий варіант зі стихією вітру.</p>';
 $('#panel-content .technique-intro').insertAdjacentHTML('afterend','<button class="reference-button" data-action="references">Порівняти з кадрами аніме <span>3 референси на техніку</span></button>');
 const frogArt=$('.technique-card[data-technique="gamabunta"] .technique-art');if(frogArt){frogArt.classList.add('frog-preview');frogArt.insertAdjacentHTML('beforeend','<img class="frog-second-app" src="'+iconArt.lampa+'" alt="">');}
 drawTechniquePreviews();
}
function drawTechniquePreviews(){for(const preview of $$('[data-preview]')){const id=preview.dataset.preview;techniques.ensure(id,384).then(()=>{if(!preview.isConnected||modal!=='techniques')return;const c=preview.getContext('2d');c.clearRect(0,0,320,210);techniques.draw(c,id,160,100,64,0,0,true);});}}
let referenceVersion=0;
function referencesPanel(){
 $('#panel-title').textContent='Техніки в аніме';
 $('#panel-content').innerHTML='<div class="reference-toolbar"><select id="reference-technique" aria-label="Техніка для порівняння">'+techniques.list.map(t=>`<option value="${t.id}">${t.name}</option>`).join('')+'</select><button class="text-button" data-action="techniques">До анімацій →</button></div><div id="anime-references" class="anime-references"></div><p class="panel-note">Три кадри для порівняння силуетів, кольорів та руху. Малюнки ефектів створені окремо для лаунчера. Посилання ведуть на сторінки, де знайдено кадри.</p>';
 $('#reference-technique').value=prefs.technique;renderReferences(prefs.technique);
}
async function renderReferences(id){
 const version=++referenceVersion;
 try{const rows=await fetch('../revisions/v3/reference-manifest.json').then(r=>{if(!r.ok)throw Error(r.status);return r.json();});if(modal!=='references'||version!==referenceVersion)return;
 $('#anime-references').innerHTML=rows.filter(r=>r.tag===id&&r.file).map((row,i)=>`<figure class="anime-reference"><img src="../${escapeHTML(row.file)}" alt="${escapeHTML(techniques.list.find(t=>t.id===id).name)} — кадр ${i+1}"><figcaption><span>Кадр ${i+1}</span><a href="${escapeHTML(row.page)}" target="_blank" rel="noopener noreferrer">Джерело ↗</a></figcaption></figure>`).join('');
 }catch(_){if(modal==='references'&&version===referenceVersion)$('#anime-references').textContent='Кадри не вдалося відкрити. Спробуй ще раз.';}
}
function enterAmbient(feedback=true){if(modal)closePanel();lastFocus=document.activeElement;ambient=true;$('#ambient').hidden=false;$('#shell').inert=true;document.body.classList.add('ambient-on');$('.ambient-exit').focus();hideFocus();clearTimeout(idleTimer);if(feedback)sound('scene');}
function leaveAmbient(){ambient=false;$('#ambient').hidden=true;$('#shell').inert=false;document.body.classList.remove('ambient-on');if(lastFocus?.isConnected)lastFocus.focus();else $('.brand').focus();resetIdle();}
function applyTimers(){clearInterval(rotationTimer);clearTimeout(idleTimer);if(prefs.rotation&&!paused){preloadNextScene();rotationTimer=setInterval(()=>{if(!modal&&!document.hidden)setScene(scenes[(scenes.findIndex(s=>s.id===prefs.scene)+1)%scenes.length].id,true,false);},Number(prefs.rotation)*1000);}resetIdle();}
function resetIdle(){clearTimeout(idleTimer);if(prefs.idle&&!paused&&!ambient)idleTimer=setTimeout(()=>{if(!modal)enterAmbient(false);else resetIdle();},Number(prefs.idle)*1000);}
function action(button){
 const type=button.dataset.action;
 if(pendingLaunch){if(type==='launch')return;cancelLaunch();}
 if(['settings','scenes','library','search','order','techniques','references','game'].includes(type)){sound('ui');openPanel(type);return;}
 switch(type){
 case'home':if(modal)closePanel();$('.brand').focus();sound('back');break;
 case'close':sound('back');closePanel();break;
 case'scene':setScene(button.dataset.scene);if(modal)closePanel();break;
 case'launch':launch(button.dataset.package);break;
 case'order-up':moveFavorite(button.dataset.package,-1);break;
 case'order-down':moveFavorite(button.dataset.package,1);break;
 case'technique':setTechnique(button.dataset.technique);closePanel();$('#dock .app-card')?.focus();sound('confirm');burst();break;
 case'sound-preview':prefs.audio=true;unlockAudio();persist();sound('confirm');break;
 case'sound-ready':prefs.audio=true;unlockAudio();persist();sound('focus');break;
 case'ambient':enterAmbient();break;
 case'exit-ambient':sound('back');leaveAmbient();break;
 case'audio':{const enable=!prefs.audio||!audioReady();prefs.audio=enable;if(enable)unlockAudio();else ambience();persist();sound('ui');break;}
 case'toggle':prefs[button.dataset.pref]=!prefs[button.dataset.pref];button.setAttribute('aria-checked',!!prefs[button.dataset.pref]);if(button.dataset.pref==='audio'&&prefs.audio)unlockAudio();persist();applyMotion();ambience();sound('ui');break;
 case'pin':{const pkg=button.dataset.package;const slot=$$('#library-grid [data-action="pin"]').indexOf(button);const added=!prefs.pins.includes(pkg);prefs.pins=added?[...prefs.pins,pkg]:prefs.pins.filter(x=>x!==pkg);persist();renderDock();renderLibrary();const pins=$$('#library-grid [data-action="pin"]');(pins.find(b=>b.dataset.package===pkg)||pins[Math.min(slot,pins.length-1)]||$('[data-action="filter"]')||$('#app-search'))?.focus();if(added&&prefs.pins.length>6)toast(`В обраному: ${prefs.pins.length}. На головному екрані видно перші шість. Зміни послідовність у «Порядок обраного».`);sound('ui');break;}
 case'filter':filterPinned=!filterPinned;button.classList.toggle('active',filterPinned);button.setAttribute('aria-pressed',filterPinned);renderLibrary();break;
 case'system':case'wifi':if(native)window.YoruNative.settings(type);else toast('Ця системна дія буде доступна після інтеграції в Android-збірку.');break;
 case'refresh':readApps();toast('Архів застосунків оновлено.');break;
 case'about':$('#panel-title').textContent='Коноха — воля вогню';$('#panel-content').innerHTML='<img src="leaf.svg" width="84" alt=""><p class="panel-note">Окремий Naruto-only launcher prototype.<br>Дванадцять шпалер, двадцять іконок і детальні ефекти створено ImageGen для цього проєкту.<br>Десять технік навігації, зокрема стрибок Гамабунти.<br>Мінігра «Шлях шинобі» з Наруто та Гамабунтою.<br>Шість спільних звуків виділення; OK — звук обраної техніки.<br>Заряджання й використання мають окремі звуки. Репліки з Naruto-кліпів очищені від зайвої тиші та сторонніх кінцівок. Джерела: Myinstants, Voicy, ZEDGE.<br>Музика: офіційні прев’ю Naruto Original Soundtrack, MUSASHI PROJECT / Apple Music.<br>Референси скелі: кадри аніме й офіційна репліка Nijigen no Mori.<br>Шрифти Nunito Sans і Caveat: SIL OFL.<br>Візуальні референси: Naruto Official, Project Rasengan, Bandai Namco.<br>Архітектурна основа: попередній Yoru / cleanyourtv (MIT).<br><br>Локальний фанатський прототип. Права на Naruto та торговельні марки належать їхнім власникам. Макет не встановлює та не запускає Android-застосунки.</p><button class="paper-button" data-action="settings">До налаштувань</button>';hideFocus();break;
 }
}
document.addEventListener('click',event=>{resetIdle();const button=event.target.closest('[data-action]');if(button){if(prefs.audio&&!audioReady()&&button.dataset.action!=='audio')unlockAudio();action(button);}});
document.addEventListener('input',event=>{const id=event.target.id;if(id==='volume'||id==='music-volume'){prefs[id==='volume'?'volume':'musicVolume']=Number(event.target.value)/100;$('#'+(id==='volume'?'volume-value':'music-volume-value')).textContent=event.target.value+'%';persist();ambience();}});
document.addEventListener('change',event=>{if(event.target.id==='reference-technique'){renderReferences(event.target.value);return;}if(event.target.id==='highlight-sound'){prefs.highlightSound=event.target.value;persist();preloadTechniqueAudio();sound('focus');return;}if(event.target.id==='rotation')prefs.rotation=Number(event.target.value);else if(event.target.id==='idle')prefs.idle=Number(event.target.value);else if(event.target.id==='music'){prefs.music=event.target.value;ambience();}else return;persist();applyTimers();});
function controls(){const root=ambient?$('#ambient'):modal?$('.scroll-panel'):$('#shell');return $$('button,input,select,a[href]',root).filter(el=>!el.disabled&&el.getClientRects().length&&!el.closest('[hidden]'));}
function remote(key,fromNative=false){
 if(pendingLaunch){if(key==='Escape')cancelLaunch();return;}
 resetIdle();const active=document.activeElement;if(prefs.audio&&!audioReady()&&!(key==='Enter'&&active?.dataset.action==='audio'))unlockAudio();
 if(modal==='game'&&!(key==='Enter'&&active?.tagName==='BUTTON')&&window.KonohaRunner.handleKey(key))return;
 if(ambient){if(['Enter','Escape','ContextMenu'].includes(key)){sound('back');leaveAmbient();}return;}
 if(key==='Escape'){sound('back');if(active?.tagName==='INPUT'&&active.type==='search'){active.blur();$('[data-action="close"]',$('.scroll-panel')).focus();}else if(modal)closePanel();else $('.brand').focus();return;}
 if(key==='ContextMenu'){if(modal==='settings')closePanel();else openPanel('settings');return;}
 if(key==='Enter'){if(active?.tagName==='INPUT'&&active.type==='search'){controls().find(el=>el.dataset.action==='launch')?.focus();return;}if(active?.tagName==='SELECT'||active?.type==='range')return;active?.click();return;}
 if(!key.startsWith('Arrow'))return;
 const horizontal=key==='ArrowLeft'||key==='ArrowRight';const sign=key==='ArrowLeft'||key==='ArrowUp'?-1:1;
 if(modal==='settings'&&!horizontal){const all=controls();const index=all.indexOf(active);const next=all[Math.max(0,Math.min(all.length-1,index+sign))];if(next&&next!==active){next.focus({preventScroll:true});next.scrollIntoView({block:'nearest'});sound('ui');}return;}
 if(active?.tagName==='INPUT'&&active.type==='search'&&horizontal){if(fromNative){const pos=Math.max(0,Math.min(active.value.length,(active.selectionStart||0)+sign));active.setSelectionRange(pos,pos);}return;}
 if(active?.type==='range'&&horizontal){active.value=String(Math.max(0,Math.min(100,Number(active.value)+sign*5)));active.dispatchEvent(new Event('input',{bubbles:true}));return;}
 if(active?.tagName==='SELECT'){
  if(horizontal){active.selectedIndex=Math.max(0,Math.min(active.options.length-1,active.selectedIndex+sign));active.dispatchEvent(new Event('change',{bubbles:true}));sound('ui');return;}
 }
 const all=controls();if(!all.includes(active)){all[0]?.focus();return;}
 const r=active.getBoundingClientRect(),x=r.x+r.width/2,y=r.y+r.height/2;let best=null,score=Infinity;
 for(const next of all){if(next===active)continue;const c=next.getBoundingClientRect(),dx=c.x+c.width/2-x,dy=c.y+c.height/2-y;const main=(horizontal?dx:dy)*sign,cross=Math.abs(horizontal?dy:dx);if(main<5)continue;const overlap=horizontal?Math.min(r.bottom,c.bottom)-Math.max(r.top,c.top):Math.min(r.right,c.right)-Math.max(r.left,c.left);const rank=main+cross*3+(overlap>0?0:300);if(rank<score){best=next;score=rank;}}
 if(best){best.focus({preventScroll:true});best.scrollIntoView({block:'nearest',inline:'nearest',behavior:canMove()?'smooth':'auto'});sound(best.matches('.app-card')?'focus':'ui');requestAnimationFrame(()=>updateFocus(best));}
}
window.YoruRemote=remote;window.KonohaRemote=remote;
document.addEventListener('keydown',event=>{
 if(event.key==='Tab'){const all=controls();if(modal||ambient){const index=all.indexOf(document.activeElement);if(event.shiftKey&&index===0){event.preventDefault();all.at(-1)?.focus();}else if(!event.shiftKey&&index===all.length-1){event.preventDefault();all[0]?.focus();}}return;}
 if(event.target.type==='search'&&['ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;
 if(modal==='game'&&[' ','p','P'].includes(event.key)){event.preventDefault();remote(event.key);return;}
 if(['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Enter','Escape','ContextMenu'].includes(event.key)){event.preventDefault();if(!event.repeat||modal!=='game')remote(event.key);}
});
const canvas=$('#chakra-canvas'),ctx=canvas.getContext('2d'),particles=[],trails=[],bursts=[];
const techniqueCanvas=$('#technique-canvas'),techniqueContext=techniqueCanvas.getContext('2d');
let width=innerWidth,height=innerHeight;
function resize(){width=innerWidth;height=innerHeight;if(paused)return;const ratio=Math.min(devicePixelRatio||1,2);for(const surface of[canvas,techniqueCanvas]){surface.width=width*ratio;surface.height=height*ratio;surface.getContext('2d').setTransform(ratio,0,0,ratio,0,0);}focusCenter=null;trails.length=0;if(focusTarget?.isConnected)updateFocus(focusTarget);}
for(let i=0;i<28;i++)particles.push({x:(i*.137%1)*width,y:(i*.283%1)*height,size:3+i%4,phase:i*1.7,speed:9+i%6*3});
function hideFocus(){focusTarget=null;focusCenter=null;trails.length=0;bursts.length=0;techniques.resetMotion?.();techniqueContext.clearRect(0,0,width,height);$('#chakra-focus').hidden=true;$$('.app-card.is-focus').forEach(e=>e.classList.remove('is-focus'));}
function updateFocus(element){
 const card=element?.closest('.app-card');if(!card||ambient){hideFocus();return;}
 const rect=$('.app-emblem',card).getBoundingClientRect();if(rect.bottom<0||rect.top>height){hideFocus();return;}
 const center={x:rect.x+rect.width/2,y:rect.y+rect.height/2,r:Math.min(rect.width,rect.height)*.45};
 const changed=card!==focusTarget;
 if(prefs.technique==='gamabunta'){
  if(changed||!focusCenter){const siblings=$$('.app-card',card.parentElement),direction=changed&&focusTarget?.parentElement===card.parentElement?Math.sign(siblings.indexOf(card)-siblings.indexOf(focusTarget)):undefined;techniques.move?.(canMove()?focusCenter:null,center,performance.now(),direction);}
  else techniques.reanchor?.(focusCenter,center);
 }
 if(changed&&focusCenter&&canMove()&&(Math.abs(center.x-focusCenter.x)+Math.abs(center.y-focusCenter.y)>5))trails.push({a:focusCenter,b:center,life:.45});
 focusCenter=center;focusTarget=card;$$('.app-card.is-focus').forEach(e=>e.classList.remove('is-focus'));card.classList.add('is-focus');
 const ring=$('#chakra-focus');ring.hidden=false;Object.assign(ring.style,{left:(rect.x+rect.width*.05)+'px',top:(rect.y+rect.height*.03)+'px',width:rect.width*.9+'px',height:rect.height*.94+'px'});
 if(!canMove()||modal)drawTechniques(0);
 if(canMove()&&!paused&&['library','search'].includes(modal)&&!frame){lastTime=performance.now();frame=requestAnimationFrame(draw);}
}
function burst(){if(!focusCenter||!canMove())return;bursts.push({...focusCenter,life:.5});const ring=$('#chakra-focus');ring.classList.remove('confirming');void ring.offsetWidth;ring.classList.add('confirming');}
function drawTechniques(time){
 const c=techniqueContext;c.clearRect(0,0,width,height);
 if(focusCenter&&!ambient&&!paused)techniques.draw(c,prefs.technique,focusCenter.x,focusCenter.y,focusCenter.r*techniques.focusScale(prefs.technique),time,bursts.length?bursts.at(-1).life*2:0);
 for(const trail of trails)techniques.trail(c,prefs.technique,trail.a,trail.b,trail.life,time);
}
document.addEventListener('focusin',event=>updateFocus(event.target));
document.addEventListener('pointerover',event=>{const card=event.target.closest('.app-card,.technique-card');const moved=event.clientX!==pointerClientX||event.clientY!==pointerClientY;if(card&&card!==document.activeElement&&moved&&event.pointerType!=='touch'){card.focus({preventScroll:true});sound(card.matches('.app-card')?'focus':'ui');}});
document.addEventListener('pointermove',event=>{pointerClientX=event.clientX;pointerClientY=event.clientY;if(canMove()){pointerX=(event.clientX/width-.5)*20;pointerY=(event.clientY/height-.5)*12;pointerTime=performance.now();}});
document.addEventListener('scroll',()=>{if(focusTarget?.isConnected)updateFocus(focusTarget);},true);
function draw(time){
 frame=0;if(paused||!canMove())return;const dt=Math.min((time-lastTime)/1000||.016,.04);lastTime=time;
 // The archive keeps one animated focus aura; its scenery and the technique sheet stay still.
 if(modal){if(['library','search'].includes(modal)&&focusCenter){for(const list of[trails,bursts])for(let i=list.length-1;i>=0;i--){list[i].life-=dt;if(list[i].life<=0)list.splice(i,1);}drawTechniques(time);frame=requestAnimationFrame(draw);}return;}
 ctx.clearRect(0,0,width,height);
 const pointerWeight=Math.exp(-(time-pointerTime)/6000);const focusX=focusCenter?(focusCenter.x/width-.5)*7:0,focusY=focusCenter?(focusCenter.y/height-.5)*4:0;
 $('.village').style.setProperty('--px',(Math.sin(time/1000*Math.PI*2/48)*14+pointerX*pointerWeight+focusX).toFixed(3));
 $('.village').style.setProperty('--py',(Math.cos(time/1000*Math.PI*2/57)*8+pointerY*pointerWeight+focusY).toFixed(3));
 const theme=scenes.find(s=>s.id===prefs.scene),particleMode=theme.particles||'leaf',palette=theme.palette||(prefs.scene==='gaara'?['#e9bc83','#bfa27a']:prefs.scene==='sakura'?['#f3bdce','#ecc3d4']:prefs.scene==='kakashi'?['#c4d7de','#8ba9b6']:prefs.scene==='sasuke'?['#b3a8bf','#7c839d']:prefs.scene==='night'?['#9fdaed','#84b4c5']:prefs.scene==='dawn'?['#bed5a3','#8ebc99']:['#e9af67','#9bab72']);
 for(const [index,p] of particles.entries()){p.x-=p.speed*dt;p.y+=particleMode==='rain'?dt*(110+p.speed*2):particleMode==='snow'?dt*(12+p.speed*.2):Math.sin(time/1700+p.phase)*dt*9+dt*4;if(p.x<-20){p.x=width+20;p.y=(p.phase*.111%1)*height;}if(p.y>height+20)p.y=-20;ctx.save();ctx.translate(p.x,p.y);ctx.rotate(particleMode==='rain'?-.18:Math.sin(time/1300+p.phase)*1.7);ctx.globalAlpha=.2+Math.sin(time/2000+p.phase)*.1;ctx.fillStyle=palette[p.phase%3<1?0:1];ctx.beginPath();if(particleMode==='rain'){ctx.fillRect(0,0,.7,7+p.size);}else if(['snow','dust','ember'].includes(particleMode)){if(particleMode==='ember'){ctx.shadowColor=palette[0];ctx.shadowBlur=5;}ctx.arc(0,0,particleMode==='snow'?1.2+p.size*.13:particleMode==='dust'?.7:1,0,Math.PI*2);ctx.fill();}else if(prefs.scene==='night'&&index<6){ctx.shadowColor='#8dddf4';ctx.shadowBlur=9;ctx.arc(0,0,1.6,0,Math.PI*2);ctx.fill();}else{ctx.moveTo(-p.size,0);ctx.quadraticCurveTo(0,-p.size,p.size,0);ctx.quadraticCurveTo(0,p.size,-p.size,0);ctx.fill();}ctx.restore();}
 for(const list of[trails,bursts])for(let i=list.length-1;i>=0;i--){list[i].life-=dt;if(list[i].life<=0)list.splice(i,1);}
 drawTechniques(time);
 frame=requestAnimationFrame(draw);
}
function applyMotion(){document.body.classList.toggle('motion-off',!canMove()||paused);cancelAnimationFrame(frame);frame=0;ctx.clearRect(0,0,width,height);$('.village').classList.toggle('decor-paused',!!modal);if(canMove()&&!paused&&(!modal||(['library','search'].includes(modal)&&focusCenter))){lastTime=performance.now();frame=requestAnimationFrame(draw);}else drawTechniques(0);}
motionQuery.addEventListener('change',applyMotion);let resizeTimer;addEventListener('resize',()=>{resize();clearTimeout(resizeTimer);resizeTimer=setTimeout(()=>{if(paused)return;techniques.setActive?.(prefs.technique);setScene(prefs.scene,false,false);if(modal==='techniques')drawTechniquePreviews();},180);});
addEventListener('konoha-art-ready',()=>{if(!canMove()&&!paused)drawTechniques(0);});
window.YoruPause=()=>{if(paused)return;cancelLaunch();window.KonohaRunner?.trim();paused=true;ambience();cancelAnimationFrame(frame);frame=0;clearInterval(rotationTimer);clearTimeout(idleTimer);clearTimeout(sceneCleanupTimer);sceneVersion++;decodedScenes.clear();for(const surface of[canvas,techniqueCanvas])surface.width=surface.height=0;for(const layer of[$('#back-a'),$('#back-b')])layer.style.backgroundImage='none';techniques.releaseAll?.();document.body.classList.add('motion-off');};
let lastRefresh=-Infinity;
window.YoruRefresh=()=>{const now=performance.now();if(!paused&&now-lastRefresh<500)return;lastRefresh=now;const resumed=paused;paused=false;if(resumed){resize();setScene(prefs.scene,false,false);techniques.setActive?.(prefs.technique);if(modal==='techniques')drawTechniquePreviews();window.KonohaRunner?.restore();}readApps();applyTimers();applyMotion();ambience();preloadTechniqueAudio();};
window.KonohaTrimMemory=()=>{audioEngine.trim();decodedScenes.clear();if(paused){techniques.releaseAll?.();for(const surface of[canvas,techniqueCanvas])surface.width=surface.height=0;}else techniques.trim?.();};
document.addEventListener('visibilitychange',()=>{if(document.hidden)window.YoruPause();else window.YoruRefresh();});
document.addEventListener('pointerdown',resetIdle,{passive:true});
sceneButtons();setScene(prefs.scene,false);readApps();resize();setTechnique(prefs.technique,false);applyMotion();applyTimers();clock();setInterval(clock,10000);ambience();
requestAnimationFrame(()=>$('#dock .app-card')?.focus({preventScroll:true}));
window.KonohaReady=true;
