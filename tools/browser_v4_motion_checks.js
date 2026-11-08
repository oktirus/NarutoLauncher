(async()=>{
 const results=[],check=(name,ok,detail='')=>{results.push({name,passed:!!ok,detail});if(!ok)throw Error(name+': '+detail);};
 await techniques.ready;
 const a={x:210,y:300,r:60},b={x:430,y:300,r:60},at=1000;
 techniques.resetMotion();techniques.move(null,a,at);techniques.move(a,b,at);
 let p=techniques.motionSnapshot(at+180);
 check('rightward jump faces right despite left-facing source',p.airborne&&p.direction===1&&p.spriteFlip===-1);
 check('no dotted plume during flight',p.dust.length===0);
 techniques.resetMotion();techniques.move(null,a,at);techniques.move(a,b,at);techniques.move(b,a,at+.1);
 check('immediate reverse input faces the new target before one pixel of travel',techniques.motionSnapshot(at+.2).direction===-1);
 techniques.resetMotion();techniques.move(null,b,at);techniques.move(b,a,at);p=techniques.motionSnapshot(at+180);
 check('leftward jump faces left without mirroring',p.airborne&&p.direction===-1&&p.spriteFlip===1);
 const land=at+370,early=techniques.motionSnapshot(land+80),later=techniques.motionSnapshot(land+480);
 check('landing emits fourteen organic puffs',early.dust.length===14);
 const spread=v=>v.dust.reduce((sum,d)=>sum+Math.hypot(d.x-v.x,d.y-v.y),0)/v.dust.length;
 check('dust expands away from the landing point',spread(later)>spread(early)*1.6);
 check('dust spreads to both sides and above/below ground center',early.dust.some(d=>d.x<early.x)&&early.dust.some(d=>d.x>early.x)&&early.dust.some(d=>d.y<early.y)&&early.dust.some(d=>d.y>early.y));
 check('dust gradually loses opacity',later.dust[0].alpha<early.dust[0].alpha*.5);
 check('dust fully disappears without another input',techniques.motionSnapshot(land+900).dust.length===0);
 techniques.resetMotion();techniques.move(null,a,at);techniques.move(a,b,at);techniques.motionSnapshot(land+80);techniques.move(b,a,land+80);
 check('old landing plume remains while next jump takes off',techniques.motionSnapshot(land+100).dust.length===14);
 techniques.resetMotion();check('leaving apps clears all jump history',techniques.motionSnapshot()===null);
 check('old undersized extent division removed',!drawTechniques.toString().includes('/extent'));
 for(const t of techniques.list.filter(t=>t.id!=='gamabunta'))check(t.id+' wraps a larger area',techniques.focusScale(t.id)>=1.3);
 const c=document.createElement('canvas');c.width=360;c.height=260;const ctx=c.getContext('2d'),signatures=[];
 for(const t of [0,40,80,160,300,600]){ctx.clearRect(0,0,360,260);techniques.draw(ctx,'chidori',180,130,80,t,0,true);const data=ctx.getImageData(0,0,360,260).data;let h=0;for(let k=0;k<data.length;k+=13)h=Math.imul(h^data[k],16777619);signatures.push(h);}
 check('Chidori redraws fine branching detail across 24Hz charge phases',new Set(signatures).size===6);
 const raster=await fetch('../assets/effects/chidori.webp').then(r=>r.arrayBuffer());check('painted Chidori detail retained',raster.byteLength>5000);
 check('readiness and activation registry covers all ten techniques',techniques.list.every(t=>KonohaCueSources[t.cue]?.focus&&KonohaCueSources[t.cue]?.confirm));
 check('focus always resolves to a different asset from OK',techniques.list.every(t=>KonohaCueSources[t.cue].focus.url!==KonohaCueSources[t.cue].confirm.url));
 check('Gaara has a Sand Village caption',scenes.find(s=>s.id==='gaara').village==='砂隠れの里');
 setTechnique('gamabunta');document.querySelector('#dock .app-card').focus();updateFocus(document.activeElement);
 return {passed:results.length,total:results.length,results};
})()
