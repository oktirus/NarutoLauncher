from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=(root/'tools/browser_v17_scene_checks.js').read_text(encoding='utf8').replace('../revisions/v17/asset-manifest.json','../revisions/v18/asset-manifest.json').replace('correct v17 bytes','correct v18 bytes').replace("'17 scenes',scenes.length===17","'18 scenes',scenes.length===18").replace("$$('.scene-card').length===17","$$('.scene-card').length===18")
s=s.replace("check('10 techniques',techniques.list.length===10);","check('10 techniques',techniques.list.length===10);check('gallery glyph replaces chevron',$('.hero-wallpapers use').getAttribute('href')==='#wallpapers'&&getComputedStyle($('.hero-wallpapers'),'::after').content==='none');")
s=s.replace("check(scene.id+' picker above apps',","check(scene.id+' picker stays inside viewport',$('.hero-wallpapers').getBoundingClientRect().right<=innerWidth);check(scene.id+' picker above apps',")
s=s.replace("click('[data-action=\"settings\"]');check('settings has", "click('[data-action=\"settings\"]');check('settings counts four Naruto scenes',$('#panel-content').textContent.includes('4 сцени Наруто'));check('settings has")
(root/'tools/browser_v18_scene_checks.js').write_text(s,encoding='utf8',newline='\r\n');print('v18 browser checks prepared')
