(async()=>{
 const wait=ms=>new Promise(r=>setTimeout(r,ms));if(modal)closePanel();const previous=prefs.technique;setTechnique('rasengan',false);await techniques.ready;
 try{openPanel('library');await wait(80);$('#library-grid .app-card').focus();await wait(80);
 const before=techniqueCanvas.toDataURL(),world=$('.village').getAttribute('style');await wait(250);
 const checks=[{name:'archive focus technique stays animated',passed:techniqueCanvas.toDataURL()!==before},{name:'decorative scenery stays frozen under archive',passed:$('.village').getAttribute('style')===world}];
 window.YoruPause();await wait(50);window.YoruRefresh();await wait(200);const resumed=techniqueCanvas.toDataURL();await wait(180);checks.push({name:'archive focus animation resumes after external app',passed:frame!==0&&techniqueCanvas.toDataURL()!==resumed});
 closePanel();setTechnique('gamabunta',false);await techniques.ready;const cards=$$('#dock .app-card');cards[0].focus();await wait(720);cards[1].focus();await wait(100);let p=techniques.motionSnapshot();checks.push({name:'frog faces right while jumping right',passed:p.direction===1&&p.airborne&&p.spriteFlip===-1});await wait(720);p=techniques.motionSnapshot();checks.push({name:'frog lands facing right',passed:!p.airborne&&p.spriteFlip===1});cards[0].focus();await wait(800);p=techniques.motionSnapshot();checks.push({name:'frog lands facing left',passed:!p.airborne&&p.direction===-1&&p.spriteFlip===-1});
 if(checks.some(c=>!c.passed))throw Error(JSON.stringify(checks));return{passed:checks.length,checks};
 }finally{if(modal)closePanel();setTechnique(previous,false);}
})()
