"""Send exact UTF-8 JS through base64, avoiding PowerShell pipeline quoting."""
from pathlib import Path
import subprocess,base64,json,sys
cli=Path.home()/'AppData/Roaming/npm/node_modules/agent-browser/bin/agent-browser-win32-x64.exe'
session,source,output=sys.argv[1:]
script=Path(source).read_text(encoding='utf-8-sig').strip()
result=subprocess.run([str(cli),'--session',session,'--json','eval','-b',base64.b64encode(script.encode()).decode()],capture_output=True,encoding='utf8',timeout=120)
Path(output).write_text(result.stdout,encoding='utf8');print(result.stdout)
assert result.returncode==0,result.stderr
r=json.loads(result.stdout);assert r['success'],r;assert r['data']['result'],r
