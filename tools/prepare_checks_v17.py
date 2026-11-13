from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=(root/'tools/browser_v16_checks.js').read_text(encoding='utf8').replace('../revisions/v16/asset-manifest.json','../revisions/v17/asset-manifest.json').replace('correct v16 bytes','correct v17 bytes')
s=s.replace("check(scene.id+' quote source',$('#quote-source').href===KonohaQuotes[scene.id].source&&$('#quote-source').title.includes('переклад'));","check(scene.id+' no quote source button',!$('#quote-source')); ")
(root/'tools/browser_v17_scene_checks.js').write_text(s,encoding='utf8',newline='\r\n')
