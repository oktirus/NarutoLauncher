from pathlib import Path
import re,hashlib,json,shutil
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v19';OUT.mkdir(exist_ok=True)
if not (OUT/'before').exists():
 shutil.copytree(ROOT/'prototype',OUT/'before/prototype');shutil.copy2(ROOT/'README.md',OUT/'before/README.md')
 (OUT/'before-hashes.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'prototype').rglob('*') if p.is_file() and p.name!='revision19.css'},indent=2),encoding='utf8')
(OUT/'verification').mkdir(exist_ok=True)
s=(OUT/'before/prototype/index.html').read_text(encoding='utf8')
s,n=re.subn(r'<button class="glyph-button" data-action="search".*?</button>','',s,count=1);assert n==1
s,n=re.subn(r'<div class="shinobi-badge".*?</div>','',s,count=1);assert n==1
h=hashlib.sha256((ROOT/'prototype/revision19.css').read_bytes()).hexdigest()[:12]
s=s.replace('</head>','<link rel="stylesheet" href="revision19.css?v='+h+'">\n</head>')
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n');print('Full-width game stylesheet linked; search and badge removed')
