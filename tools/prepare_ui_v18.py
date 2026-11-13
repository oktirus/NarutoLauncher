from pathlib import Path
import hashlib,re
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v18';p=ROOT/'prototype/app.js';a=p.read_text(encoding='utf8')
old="Три настрої Наруто й '+scenes.filter(s=>s.hero).length+' шпалер героїв."
new="'+scenes.filter(s=>!s.hero).length+' сцени Наруто й '+scenes.filter(s=>s.hero).length+' шпалер героїв."
if old in a:a=a.replace(old,new,1);p.write_text(a,encoding='utf8',newline='\r\n')
s=(OUT/'before/prototype/index.html').read_text(encoding='utf8')
def url(name):return name+'?v='+hashlib.sha256((ROOT/'prototype'/name).read_bytes()).hexdigest()[:12]
for name in ['app.js','scenes.js','quotes.js']:s=re.sub(re.escape(name)+r'\?v=[a-zA-Z0-9]+',url(name),s)
s=s.replace('</head>','<link rel="stylesheet" href="'+url('revision18.css')+'">\n</head>')
s=s.replace('</defs></svg>','<symbol id="wallpapers" viewBox="0 0 24 24"><path d="M3 16V3h16M7 7h14v14H7Z"/><circle cx="16.5" cy="11.5" r="1.5"/><path d="m7 18 4-4 4 4 2-2 4 4"/></symbol>\n</defs></svg>')
assert s.count('id="wallpapers"')==1
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n');print('v18 icon, scene count and cache keys updated')
