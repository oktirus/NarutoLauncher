"""Read-only process-attributed samples; never launch, stop, or reconfigure apps."""
from pathlib import Path
from datetime import datetime,timezone
import subprocess,re,json,time,statistics,sys
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'revisions/v25/device/memory';OUT.mkdir(exist_ok=True)
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';SERIAL='192.168.100.26:5555'
def shell(*args):return subprocess.check_output([str(ADB),'-s',SERIAL,'shell',*args],timeout=35).decode('utf8',errors='replace').strip()
assert shell('getprop','ro.product.model')=='LNVNB161216'
raw=shell('dumpsys','activity','processes');processes=[]
for block in re.split(r'(?m)(?=^\s+\*APP\* UID )',raw)[1:]:
 p=re.search(r'ProcessRecord\{\S+ (\d+):([^\s}]+)',block);packages=re.search(r'packageList=\{([^}]+)\}',block)
 if p and packages and 'ua.konoha.tv' in [s.strip() for s in packages[1].split(',')]:processes.append((int(p[1]),p[2]))
assert len(processes)>=2,processes
label=sys.argv[1];samples=[]
for i in range(3):
 rows=[]
 for pid,name in processes:
  raw=shell('dumpsys','meminfo',str(pid));(OUT/f'{label}-{i}-{pid}.txt').write_text(raw,encoding='utf8')
  match=re.search(r'TOTAL PSS:\s+(\d+)\s+TOTAL RSS:\s+(\d+)\s+TOTAL SWAP PSS:\s+(\d+)',raw);assert match
  rows.append(dict(pid=pid,name=name,pss_kib=int(match[1]),rss_kib=int(match[2]),swap_pss_kib=int(match[3])))
 samples.append(dict(time_utc=datetime.now(timezone.utc).isoformat(),processes=rows,pss_mib=sum(r['pss_kib'] for r in rows)/1024,rss_mib=sum(r['rss_kib'] for r in rows)/1024,swap_mib=sum(r['swap_pss_kib'] for r in rows)/1024));time.sleep(1)
result=dict(label=label,samples=samples,mean_pss_mib=statistics.mean(s['pss_mib'] for s in samples),range_pss_mib=[min(s['pss_mib'] for s in samples),max(s['pss_mib'] for s in samples)])
(OUT/f'{label}.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(result,ensure_ascii=False))
