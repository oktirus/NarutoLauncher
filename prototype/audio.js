'use strict';
/* Complete cue playback and sample-clocked overlapping music loops. */
window.KonohaAudio=(()=>{
 const curves={in:new Float32Array(129),out:new Float32Array(129)};
 for(let i=0;i<129;i++){curves.in[i]=Math.sin(i/128*Math.PI/2);curves.out[i]=Math.cos(i/128*Math.PI/2);}
 function scheduleLoop(context,destination,buffer,start,fade){
  const source=context.createBufferSource(),gain=context.createGain();source.buffer=buffer;source.connect(gain);gain.connect(destination);
  gain.gain.setValueAtTime(0,start);gain.gain.setValueCurveAtTime(curves.in,start,fade);
  gain.gain.setValueAtTime(1,start+fade);gain.gain.setValueCurveAtTime(curves.out,start+buffer.duration-fade,fade);
  source.start(start);return {source,gain,start,end:start+buffer.duration};
 }
 class Engine{
  constructor(){this.context=null;this.cache=new Map();this.voices=new Set();this.musicNodes=new Set();this.pendingCues=new Map();this.events=[];this.state={audio:false,ambience:true,paused:false,volume:.45,musicVolume:.22,music:'1584791975'};this.unlocked=false;this.musicURL='';this.musicToken=0;this.lifecycle=0;this.timer=0;this.buffer=null;this.fade=3;this.nextTime=0;this.onStateChange=null;}
  ensure(){
   if(this.context)return this.context;
   const context=new AudioContext();this.context=context;this.fx=context.createGain();this.music=context.createGain();
   const limiter=context.createDynamicsCompressor();limiter.threshold.value=-8;limiter.knee.value=10;limiter.ratio.value=5;limiter.attack.value=.005;limiter.release.value=.18;
   this.fx.connect(limiter);this.music.connect(limiter);limiter.connect(context.destination);this.setVolumes();
   context.addEventListener('statechange',()=>{this.unlocked=context.state==='running';this.onStateChange?.(this.unlocked);if(this.unlocked)this.configure(this.state).catch(()=>{});});
   return context;
  }
  log(event){this.events.push({...event,time:this.context?.currentTime||0});if(this.events.length>160)this.events.shift();}
  setVolumes(){if(!this.context)return;const now=this.context.currentTime;this.fx.gain.setTargetAtTime(this.state.volume,now,.035);this.music.gain.setTargetAtTime(this.state.musicVolume,now,.08);}
  async load(url){
   this.ensure();if(!this.cache.has(url)){const promise=fetch(url).then(r=>{if(!r.ok)throw Error('Audio HTTP '+r.status);return r.arrayBuffer();}).then(data=>this.context.decodeAudioData(data));this.cache.set(url,promise);promise.catch(()=>this.cache.delete(url));}return this.cache.get(url);
  }
  async unlock(){
   const context=this.ensure();let timer;
   // A native bridge call may lack browser user activation. Never latch a pending resume.
   try{await Promise.race([context.resume(),new Promise(resolve=>{timer=setTimeout(resolve,120);})]);}
   finally{clearTimeout(timer);this.unlocked=context.state==='running';this.onStateChange?.(this.unlocked);}
   if(this.unlocked)await this.configure(this.state);return this.unlocked;
  }
  async configure(next){
   this.state={...this.state,...next};this.setVolumes();
   if(!this.state.audio||this.state.paused){this.stopCues('mute-or-pause');this.stopMusic();return;}
   if(!this.unlocked||this.context?.state!=='running')return;
   if(this.state.ambience)await this.startMusic('../assets/audio/naruto/'+this.state.music+'.m4a');else this.stopMusic();
  }
  async playCue(cue,type='focus'){
   if(!this.state.audio||this.state.paused||!this.unlocked||this.context?.state!=='running')return false;
   // Deduplicate both loading and sounding voices; a repeated OK never creates a chorus.
   const key=type==='focus'?'focus':type+':'+cue,ticket={};
   if(this.pendingCues.has(key)||[...this.voices].some(v=>v.type===type&&(type==='focus'||v.cue===cue)))return false;
   const lifecycle=this.lifecycle;this.pendingCues.set(key,ticket);
   try{
    const variant=window.KonohaCueSources?.[cue]?.[type],url=variant?.url||'../assets/audio/naruto/full-'+cue+'.wav?v=4';
    const buffer=await this.load(url);
    if(lifecycle!==this.lifecycle||!this.state.audio||this.state.paused||this.voices.size>=6)return false;
    const source=this.context.createBufferSource(),gain=this.context.createGain();source.buffer=buffer;gain.gain.value=variant?.gain??(type==='focus'?.28:.65);source.connect(gain);gain.connect(this.fx);
    const voice={source,gain,cue,type,url,start:this.context.currentTime,duration:buffer.duration,stopped:false};this.voices.add(voice);
    source.onended=()=>{this.voices.delete(voice);source.disconnect();gain.disconnect();this.log({event:voice.stopped?'cue-stopped':'cue-ended',cue,type,duration:voice.duration,elapsed:this.context.currentTime-voice.start});};
    source.start();this.log({event:'cue-started',cue,type,url,duration:buffer.duration,concurrency:this.voices.size});return true;
   }finally{if(this.pendingCues.get(key)===ticket)this.pendingCues.delete(key);}
  }
  stopCues(reason){this.lifecycle++;for(const voice of this.voices){voice.stopped=true;voice.reason=reason;try{voice.source.stop();}catch(_){}}this.pendingCues.clear();}
  async startMusic(url){
   if(this.musicURL===url&&(this.timer||this.loadingMusic))return;
   this.stopMusic();this.musicURL=url;const token=this.musicToken;this.loadingMusic=true;
   try{
    const buffer=await this.load(url);if(token!==this.musicToken||!this.state.audio||this.state.paused||!this.state.ambience)return;
    this.buffer=buffer;this.fade=Math.min(3.5,buffer.duration/4);this.nextTime=this.context.currentTime+.04;this.tick();this.timer=setInterval(()=>this.tick(),250);this.log({event:'music-started',url,duration:buffer.duration,crossfade:this.fade});
   }finally{if(token===this.musicToken)this.loadingMusic=false;}
  }
  tick(){
   if(!this.buffer||this.state.paused)return;
   const horizon=this.context.currentTime+this.buffer.duration*1.12;
   while(this.nextTime<horizon){
    const node=scheduleLoop(this.context,this.music,this.buffer,this.nextTime,this.fade);this.musicNodes.add(node);
    node.source.onended=()=>{this.musicNodes.delete(node);node.source.disconnect();node.gain.disconnect();};
    this.nextTime+=this.buffer.duration-this.fade;
   }
  }
  stopMusic(){this.musicToken++;clearInterval(this.timer);this.timer=0;this.loadingMusic=false;for(const node of this.musicNodes){try{node.source.stop();}catch(_){}}this.musicNodes.clear();this.musicURL='';this.buffer=null;}
  snapshot(){return {unlocked:this.unlocked,state:{...this.state},context:this.context?.state||'not-created',cues:[...this.voices].map(v=>({cue:v.cue,type:v.type,url:v.url,start:v.start,duration:v.duration})),music:this.musicURL,crossfade:this.fade,musicNodes:[...this.musicNodes].map(n=>({start:n.start,end:n.end})),events:[...this.events]};}
 }
 return {Engine,scheduleLoop};
})();
