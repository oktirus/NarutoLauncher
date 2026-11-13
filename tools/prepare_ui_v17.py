"""Apply v17's explicit markup removals and content-derived cache keys."""
from pathlib import Path
import re,hashlib
ROOT=Path(__file__).resolve().parents[1];s=(ROOT/'revisions/v17/before/prototype/index.html').read_text(encoding='utf8')
s=re.sub(r'<p class="hero-kicker">.*?</p>','',s,count=1)
s=re.sub(r'<a id="quote-source".*?</a>','',s,count=1)
s=s.replace('<h2 id="apps-title">Техніки</h2>','<h2 id="apps-title">Сувої розваг</h2>')
def url(name):return name+'?v='+hashlib.sha256((ROOT/'prototype'/name).read_bytes()).hexdigest()[:12]
for name in ['app.js','scenes.js','revision17.css']:s=re.sub(re.escape(name)+r'\?v=[a-zA-Z0-9]+',url(name),s)
s=s.replace('</head>','<link rel="stylesheet" href="'+url('revision17.css')+'">\n</head>')
assert 'hero-kicker' not in s and 'quote-source' not in s and 'Сувої розваг' in s
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n');print('v17 markup and content cache keys updated')
