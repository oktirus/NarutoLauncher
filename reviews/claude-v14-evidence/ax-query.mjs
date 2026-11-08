// Claude v14 review: read Chromium AX node of .audio-invitation via raw CDP (read-only).
const ws=new WebSocket(process.argv[2]);let id=0;const pend=new Map();
const send=(method,params={},sessionId)=>new Promise((res,rej)=>{const i=++id;pend.set(i,{res,rej});ws.send(JSON.stringify({id:i,method,params,sessionId}));});
ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id&&pend.has(m.id)){const p=pend.get(m.id);pend.delete(m.id);m.error?p.rej(new Error(JSON.stringify(m.error))):p.res(m.result);}};
ws.onopen=async()=>{try{
 const {targetInfos}=await send('Target.getTargets');const t=targetInfos.find(x=>x.type==='page'&&x.url.includes('127.0.0.1:8767/prototype'));
 const {sessionId}=await send('Target.attachToTarget',{targetId:t.targetId,flatten:true});
 await send('DOM.enable',{},sessionId);await send('Accessibility.enable',{},sessionId);
 const {root}=await send('DOM.getDocument',{depth:0},sessionId);
 const {nodeId}=await send('DOM.querySelector',{nodeId:root.nodeId,selector:'.audio-invitation'},sessionId);
 const {nodes}=await send('Accessibility.getPartialAXTree',{nodeId,fetchRelatives:false},sessionId);
 const n=nodes.find(x=>x.role?.value==='button')||nodes[0];
 const ev=await send('Runtime.evaluate',{expression:"JSON.stringify({w:innerWidth,h:innerHeight,attr:document.querySelector('.audio-invitation').getAttribute('aria-label'),pressedAttr:document.querySelector('.audio-invitation').getAttribute('aria-pressed'),visible:getComputedStyle(document.querySelector('.audio-invitation'),'::after').content,ctx:(window.KonohaAudio||window.audioEngine||{}).context?.state})",returnByValue:true},sessionId);
 console.log(JSON.stringify({role:n.role?.value,name:n.name?.value,nameSources:(n.name?.sources||[]).filter(s=>s.value||s.attributeValue).map(s=>({type:s.type,attr:s.attribute,val:(s.value||s.attributeValue)?.value,superseded:s.superseded})),props:(n.properties||[]).map(p=>p.name+'='+p.value.value).join(','),dom:JSON.parse(ev.result.value)}));
 await send('Target.detachFromTarget',{sessionId});
}catch(err){console.error(err.message);process.exitCode=1}finally{ws.close();}};
