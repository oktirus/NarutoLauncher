"""Focused follow-up to the installation benchmark: load the selected focus art."""
from pathlib import Path
import hashlib
import json
import re
import statistics
import subprocess
import time
import sys

OUT = Path(sys.argv[1]).resolve()
BASE = json.loads((OUT / 'results.json').read_text(encoding='utf8'))
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL = '192.168.100.26:5555'
PACKAGE = 'ua.konoha.tv.nativepreview'
result = {'kind': 'foreground home after D-pad input', 'samples': [], 'warmup_seconds': 30}


def adb(*args):
    process = subprocess.run([str(ADB), '-s', SERIAL, *args], capture_output=True, timeout=60, check=True)
    return process.stdout


def shell(*args):
    return adb('shell', *args).decode('utf8', errors='replace').strip()


def save():
    (OUT / 'post-navigation.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf8')


assert shell('getprop', 'ro.product.model') == 'LNVNB161216'
assert shell('getprop', 'ro.product.device') == 'x86_64_tv_go'
focus = lambda: [line.strip() for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line]
assert any(PACKAGE + '/' in line for line in focus())
path = shell('pm', 'path', PACKAGE).removeprefix('package:')
assert shell('sha256sum', path).split()[0].upper() == BASE['candidate']['sha256']
result['command'] = ['adb', '-s', SERIAL, 'shell', 'input', 'keyevent', '21']
shell('input', 'keyevent', '21')
time.sleep(2)
(OUT / 'native-after-navigation.png').write_bytes(adb('exec-out', 'screencap', '-p'))
print('D-pad focus moved; warming 30 seconds', flush=True)
time.sleep(30)
for index in range(5):
    assert any(PACKAGE + '/' in line for line in focus())
    pid = shell('pidof', PACKAGE)
    assert pid and ' ' not in pid
    raw = shell('dumpsys', 'meminfo', pid)
    (OUT / f'native-after-navigation-{index}-{pid}-meminfo.txt').write_text(raw, encoding='utf8')
    m = re.search(r'TOTAL PSS:\s+(\d+)\s+TOTAL RSS:\s+(\d+)\s+TOTAL SWAP PSS:\s+(\d+)', raw)
    assert m
    result['samples'].append({'pid': int(pid), 'pss_mib': int(m[1]) / 1024, 'rss_mib': int(m[2]) / 1024,
                              'swap_pss_mib': int(m[3]) / 1024})
    save()
    if index < 4:
        time.sleep(2)
result['pss_mib_mean'] = statistics.mean(s['pss_mib'] for s in result['samples'])
result['pss_mib_min'] = min(s['pss_mib'] for s in result['samples'])
result['pss_mib_max'] = max(s['pss_mib'] for s in result['samples'])
web = BASE['aggregate']['ua.konoha.tv']['pss_mib_mean']
result['savings_pss_mib'] = web - result['pss_mib_mean']
result['savings_percent'] = result['savings_pss_mib'] / web * 100
result['final_focus'] = focus()
result['final_home'] = shell('cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
assert result['final_home'] == BASE['before']['home']
result['final_power'] = shell('dumpsys', 'power')
result['final_standby'] = json.loads(shell('cat', '/data/local/tmp/lenovo-standby/status.json'))
result['status'] = 'PASS'
(OUT / 'native-final.png').write_bytes(adb('exec-out', 'screencap', '-p'))
save()
print(json.dumps({k: v for k, v in result.items() if k in ('status', 'pss_mib_mean', 'pss_mib_min', 'pss_mib_max', 'savings_pss_mib', 'savings_percent')}, indent=2), flush=True)
