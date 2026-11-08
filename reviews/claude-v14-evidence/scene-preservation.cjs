// Claude v14 review: compare scene entries current vs v14/before and verify runtime file hash prefixes (read-only)
const fs=require('fs'),vm=require('vm'),crypto=require('crypto'),path=require('path');
const root=path.resolve(__dirname,'../..');
const load=f=>{const ctx={window:{}};ctx.self=ctx.window;vm.createContext(ctx);vm.runInContext(fs.readFileSync(f,'utf8'),ctx);const w=ctx.window;return w.KonohaScenes||w.scenes||Object.values(w).find(Array.isArray);};
const cur=load(path.join(root,'prototype/scenes.js')),bef=load(path.join(root,'revisions/v14/before/prototype/scenes.js'));
console.log('count',cur.length,bef.length);
for(const s of cur){const b=bef.find(x=>x.id===s.id);const same=JSON.stringify(s)===JSON.stringify(b);
 const [rel,q]=s.image.split('?v=');const f=path.join(root,'assets/runtime',rel);const h=fs.existsSync(f)?crypto.createHash('sha256').update(fs.readFileSync(f)).digest('hex'):'MISSING';
 console.log(s.id.padEnd(9),'entryIdentical='+same,rel.padEnd(28),'sha256='+h.slice(0,16),'cacheKeyMatch='+(q?h.startsWith(q):'n/a'));}
