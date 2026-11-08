// Test-only initialization; isolated browser session, never loaded by the product.
localStorage.setItem('konoha.preferences.v1',JSON.stringify({scene:'dusk',audio:true,ambience:true,volume:.25,motion:true,rotation:0,idle:0,pins:null}));
window.__audioCalls=[];
HTMLMediaElement.prototype.play=function(){window.__audioCalls.push(this.currentSrc||this.src);return Promise.resolve();};
