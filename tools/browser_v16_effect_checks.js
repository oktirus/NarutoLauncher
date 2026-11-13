(async()=>{
 const results=[],check=(name,passed,detail='')=>{results.push({name,passed:!!passed,detail});if(!passed)throw Error(name+': '+JSON.stringify(detail));};
 const frame=document.createElement('iframe');frame.hidden=true;const loaded=new Promise((resolve,reject)=>{frame.onload=resolve;frame.onerror=reject;});frame.src='../revisions/v16/verification/render-before.html';document.body.append(frame);await loaded;
 try{
  const old=frame.contentWindow.KonohaTechniques;await Promise.all([old.ready,KonohaTechniques.ready]);old.resetMotion();KonohaTechniques.resetMotion();
  const canvas=document.createElement('canvas');canvas.width=400;canvas.height=300;const c=canvas.getContext('2d');
  const draw=(api,id,t)=>{c.clearRect(0,0,400,300);api.draw(c,id,200,150,76,t,0,true);return c.getImageData(0,0,400,300).data;};
  for(const id of KonohaTechniques.list.map(t=>t.id).filter(id=>id!=='chidori')){
   for(const t of [0,530,1900]){const a=draw(old,id,t),b=draw(KonohaTechniques,id,t);check(id+' unchanged frame '+t,a.every((v,i)=>v===b[i]));}
  }
  const raw={begin:c.beginPath.bind(c),move:c.moveTo.bind(c),line:c.lineTo.bind(c),stroke:c.stroke.bind(c)};let path=[],packets=[];
  c.beginPath=()=>{path=[];raw.begin();};c.moveTo=(x,y)=>{path.push([x,y]);raw.move(x,y);};c.lineTo=(x,y)=>{path.push([x,y]);raw.line(x,y);};
  c.stroke=()=>{if(Math.abs(c.lineWidth-90*.013)<1e-7&&c.strokeStyle==='#ecfbff'&&path.length===3)packets.push(path.slice());raw.stroke();};
  const radii=time=>{packets=[];c.clearRect(0,0,400,300);KonohaTechniques.draw(c,'chidori',200,150,90,time);return packets.map(p=>p.reduce((n,[x,y])=>n+Math.hypot(x,y),0)/p.length);};
  const from=radii(0),to=radii(750);check('22 traveling charge branches',from.length===22&&to.length===22);check('charge packet moves into center',to[0]<from[0]*.8,{from:from[0],to:to[0]});
  return {passed:results.length,results};
 }finally{frame.remove();document.querySelector('#dock .app-card').focus();}
})()
