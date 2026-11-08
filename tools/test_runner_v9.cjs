const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const context={window:{},Math};vm.createContext(context);vm.runInContext(fs.readFileSync(__dirname+'/../prototype/runner.js','utf8'),context);
const {Run}=context.window.KonohaRunner;const checks=[];function check(name,test){test();checks.push({name,passed:true});}
for(const hero of ['naruto','gamabunta']){
 check(hero+' waits for start',()=>{const r=new Run(hero);r.step(1);assert.equal(r.distance,0);assert.equal(r.jump(),false);});
 check(hero+' jump peaks and lands once',()=>{const r=new Run(hero);r.start();assert.equal(r.jump(),true);r.step(.2);assert(r.y>100);assert.equal(r.jump(),false);for(let i=0;i<4;i++)r.step(.2);assert.equal(r.y,0);assert.equal(r.vy,0);});
 check(hero+' duck expires',()=>{const r=new Run(hero);r.start();const standing=r.box().h;r.crouch();assert(r.box().h<standing);for(let i=0;i<5;i++)r.step(.2);assert.equal(r.duck,0);assert.equal(r.box().h,standing);});
 check(hero+' pauses all physics',()=>{const r=new Run(hero);r.start();r.jump();r.step(.1);r.status='paused';const before=JSON.stringify(r.snapshot());r.step(.2);assert.equal(JSON.stringify(r.snapshot()),before);});
 for(const speed of [360,500,760])for(const kind of ['log','stake','shuriken']){
  const distance=(speed-360)/.012;
  const fixture=()=>{const r=new Run(hero);r.start();r.distance=distance;r.next=100;r.speed=speed;const w=kind==='log'?64:kind==='stake'?40:42,h=kind==='log'?48:kind==='stake'?74:34;r.obstacles=[{kind,x:r.box().x+r.box().w+speed*.27,y:kind==='shuriken'?344:440-h,w,h,passed:false}];return r;};
  check(`${hero} ${kind} collision at ${speed}`,()=>{const r=fixture();for(let i=0;i<120&&r.status==='running';i++)r.step(1/120);assert.equal(r.status,'over');});
  check(`${hero} ${kind} avoidable at ${speed}`,()=>{const r=fixture();if(kind==='shuriken')r.crouch();else r.jump();for(let i=0;i<120&&r.status==='running';i++)r.step(1/120);assert.equal(r.status,'running');assert.equal(r.cleared,1);});
 }
 check(hero+' restart clears all state',()=>{const r=new Run(hero);r.start();r.step(.2);r.status='over';r.start();assert.equal(r.distance,0);assert.equal(r.obstacles.length,0);assert.equal(r.y,0);assert.equal(r.status,'running');});
 check(hero+' deterministic at 30/60/120 Hz',()=>{const simulate=hz=>{const r=new Run(hero,()=>.5);r.start();r.next=100;for(let i=0;i<hz*2;i++)r.step(1/hz);return r;};const a=simulate(30),b=simulate(60),c=simulate(120);assert(Math.abs(a.distance-b.distance)<.001);assert(Math.abs(c.distance-b.distance)<.001);});
}
check('speed progression capped',()=>{const r=new Run();r.start();r.distance=1e7;r.next=100;r.step(.1);assert.equal(r.speed,760);});
check('invalid hero recovers',()=>assert.equal(new Run('bad').hero,'naruto'));
const pose=context.window.KonohaRunner.poseFor;
for(const [time,frame]of [[.01,0],[.23,1],[.41,2],[.63,3]])check('Naruto slide phase '+frame,()=>{const p=pose({hero:'naruto',status:'running',speed:360,elapsed:9,y:0,duck:.72-time});assert.equal(p.mode,'slide');assert.equal(p.frame,frame);});
check('slide keeps .72 second window',()=>{const r=new Run();r.start();r.crouch();assert.equal(r.duck,.72);});
check('pause freezes slide phase',()=>{const r=new Run();r.start();r.crouch();r.step(.25);r.status='paused';const before=JSON.stringify(pose(r));r.step(.2);assert.equal(JSON.stringify(pose(r)),before);});
check('slide automatically returns to run',()=>{const r=new Run();r.start();r.crouch();for(let k=0;k<4;k++)r.step(.2);assert.equal(r.duck,0);assert.equal(pose(r).mode,'run');});
for(const [time,frame]of [[.01,0],[.23,1],[.41,2],[.63,3]])check('Gamabunta slide phase '+frame,()=>{const p=pose({hero:'gamabunta',status:'running',speed:360,elapsed:9,y:0,duck:.72-time});assert.equal(p.mode,'slide');assert.equal(p.frame,frame);});
check('Gamabunta pause freezes slide',()=>{const r=new Run('gamabunta');r.start();r.crouch();r.step(.23);r.status='paused';const a=JSON.stringify(pose(r));r.step(.25);assert.equal(JSON.stringify(pose(r)),a);});
check('Gamabunta slide resumes run',()=>{const r=new Run('gamabunta');r.start();r.crouch();for(let i=0;i<4;i++)r.step(.2);assert.equal(pose(r).mode,'run');});
check('Gamabunta airborne retains jump',()=>assert.equal(pose({hero:'gamabunta',status:'running',speed:360,elapsed:0,y:80,duck:0}).frame,4));
check('airborne Naruto retains jump',()=>assert.equal(pose({hero:'naruto',status:'running',speed:360,elapsed:0,y:80,duck:0}).frame,4));
fs.writeFileSync(__dirname+'/../revisions/v9/verification/runner-physics.json',JSON.stringify({passed:checks.length,checks},null,2));console.log(`${checks.length} runner physics and slide checks passed`);
