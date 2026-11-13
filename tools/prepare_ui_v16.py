"""Remove only explicitly redundant UI paths from the frozen v15 markup."""
from pathlib import Path
import re,hashlib
ROOT=Path(__file__).resolve().parents[1];before=ROOT/'revisions/v16/before/prototype/index.html'
s=before.read_text(encoding='utf8')
s=re.sub(r'<link rel="stylesheet" href="revision(?:13|15)\.css\?v=\d+">','',s)
def url(name):return name+'?v='+hashlib.sha256((ROOT/'prototype'/name).read_bytes()).hexdigest()[:12]
for name in ['scenes.js','app.js','technique-art.js']:s=re.sub(re.escape(name)+r'\?v=\d+',url(name),s)
s=s.replace('</head>','<link rel="stylesheet" href="'+url('revision16.css')+'">\n</head>')
# quotes load before app.js in deferred document order.
tag='<script src="'+url('app.js')+'" defer></script>'
s=s.replace(tag,'<script src="'+url('quotes.js')+'" defer></script>'+tag)
s=re.sub(r' <button class="brand".*?</button>', ' <button class="brand" data-action="home" aria-label="Naruto — головний екран"><img class="naruto-logo" src="../assets/runtime/v16/naruto-logo.webp" alt="Naruto"></button>',s,count=1)
s=re.sub(r' <nav aria-label="Головна навігація">.*?</nav>\n','',s,count=1)
s=s.replace('<button class="glyph-button" data-action="search"','<button class="glyph-button game-shortcut" data-action="game" aria-label="Мінігра — Шлях шинобі" title="Мінігра"><svg><use href="#gamepad"/></svg></button><button class="glyph-button" data-action="search"',1)
s=re.sub(r'<div class="hero-actions">.*?</div>', '<div class="quote-attribution"><span id="quote-author"></span><a id="quote-source" href="#" target="_blank" rel="noopener noreferrer" aria-label="Джерело цитати">↗</a></div><div id="scene-buttons" class="scene-buttons"></div>',s,count=1)
s=re.sub(r'<aside class="scene-caption">.*?</aside>','',s,count=1)
s=re.sub(r' <section class="scene-strip".*?</section>\n','',s,count=1)
s=s.replace('<button class="text-button" data-action="library">Весь архів <svg><use href="#arrow"/></svg></button>','')
s=re.sub(r'<span class="footer-quote">.*?</span>','',s,count=1)
s=s.replace('<symbol id="search"','<symbol id="gamepad" viewBox="0 0 24 24"><path d="M7 7h10q3 0 4 4l1 7q0 3-3 2l-4-3H9l-4 3q-3 1-3-2l1-7q1-4 4-4Z"/><path d="M7 10v6m-3-3h6m6-2v1m3 2v1"/></symbol>\n<symbol id="search"',1)
assert 'audio-invitation' not in s and 'nav-tab' not in s and 'scene-caption' not in s
(ROOT/'prototype/index.html').write_text(s,encoding='utf8',newline='\r\n')
print('One scene picker, one archive icon, upper game icon; duplicate controls removed')
