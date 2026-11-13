"""Matched foreground sampling on the explicitly selected Lenovo TV.

No installation, data clearing, HOME changes, or power configuration changes.
Profiles are reported as found; this tool never changes preferences.
"""
from pathlib import Path
import argparse
import datetime
import json
import re
import statistics
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--round', choices=['web', 'native'], required=True)
parser.add_argument('--label', required=True)
args = parser.parse_args()
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGES = {'web': 'ua.konoha.tv', 'native': 'ua.konoha.tv.nativeparity'}
COMPONENTS = {kind: package + '/ua.konoha.tv.nativepreview.MainActivity'
              if kind == 'native' else package + '/.MainActivity'
              for kind, package in PACKAGES.items()}
OUT = args.output.resolve()
OUT.mkdir(parents=True, exist_ok=True)
journal = []


def adb(*command, binary=False):
    result = subprocess.run([str(ADB), '-s', args.serial, *command], capture_output=True, timeout=45)
    journal.append({'time': datetime.datetime.now().astimezone().isoformat(),
                    'command': ['adb', '-s', args.serial, *command], 'exit_code': result.returncode,
                    'stderr': result.stderr.decode('utf-8', errors='replace')})
    (OUT / (args.label + '-journal.json')).write_text(json.dumps(journal, indent=2), encoding='utf-8')
    result.check_returncode()
    return result.stdout if binary else result.stdout.decode('utf-8', errors='replace').strip()


def shell(*command):
    return adb('shell', *command)


def home():
    return shell('cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN',
                 '-c', 'android.intent.category.HOME')


def focus():
    return [line.strip() for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line]


def processes(package):
    raw = shell('dumpsys', 'activity', 'processes')
    rows = []
    for block in re.split(r'(?m)(?=^\s+\*APP\* UID )', raw)[1:]:
        proc = re.search(r'ProcessRecord\{\S+ (\d+):([^\s}]+)', block)
        packages = re.search(r'packageList=\{([^}]+)\}', block)
        if proc and packages and package in [p.strip() for p in packages[1].split(',')]:
            rows.append((int(proc[1]), proc[2]))
    return sorted(rows), raw


assert shell('getprop', 'ro.product.model') == 'LNVNB161216', 'Wrong physical target'
assert shell('getprop', 'ro.product.device') == 'x86_64_tv_go', 'Wrong device'
package = PACKAGES[args.round]
other = PACKAGES['native' if args.round == 'web' else 'web']
initial_home = home()
result = {'package': package, 'label': args.label, 'warmup_seconds': 30, 'samples': [],
          'home_before': initial_home, 'status': 'running'}
path = shell('pm', 'path', package).removeprefix('package:')
assert path.startswith('/data/app/'), 'Missing/ambiguous package APK'
result['apk_sha256'] = shell('sha256sum', path).split()[0]
result['version'] = [line.strip() for line in shell('dumpsys', 'package', package).splitlines()
                     if 'versionCode=' in line or 'versionName=' in line]
adb('shell', 'echo wake > /data/local/tmp/lenovo-standby/command')
shell('am', 'force-stop', other)
launch = shell('am', 'start', '-S', '-W', '-n', COMPONENTS[args.round])
assert 'Status: ok' in launch, launch
# Wait for asynchronous inventory/initial-focus restoration before navigating.
time.sleep(1)
keys = [20, 20, 20] + [21] * 6 if args.round == 'native' else [22, 21]
for key in keys:
    shell('input', 'keyevent', str(key))
print(f'{args.label}: foreground launched; warming 30 seconds', flush=True)
time.sleep(30)
if args.round == 'native':
    # Use the explicit Home -> scene -> first-card route after startup has settled.
    # Early key events can race the async inventory and delayed focus restore.
    for key in [4, 20, 20]:
        shell('input', 'keyevent', str(key))
    time.sleep(2)
rows, raw = processes(package)
assert rows, 'No launcher processes'
(OUT / (args.label + '-processes.txt')).write_text(raw, encoding='utf-8')
for index in range(5):
    assert any(package + '/' in line for line in focus()), 'Launcher lost foreground'
    current, _ = processes(package)
    assert current == rows, 'Process set changed during sampling'
    sample = {'processes': [], 'time': datetime.datetime.now().astimezone().isoformat()}
    for pid, name in rows:
        raw = shell('dumpsys', 'meminfo', str(pid))
        (OUT / f'{args.label}-{index}-{pid}-meminfo.txt').write_text(raw, encoding='utf-8')
        pss = re.search(r'TOTAL PSS:\s+(\d+)', raw)
        rss = re.search(r'TOTAL RSS:\s+(\d+)', raw)
        swap = re.search(r'TOTAL (SWAP PSS|SWAP \(KB\)):\s+(\d+)', raw)
        assert pss and rss and swap, 'Missing memory totals'
        sample['processes'].append({'pid': pid, 'name': name, 'pss_kib': int(pss[1]),
                                    'rss_kib': int(rss[1]), 'swap_kib': int(swap[2]), 'swap_kind': swap[1]})
    sample['pss_mib'] = sum(p['pss_kib'] for p in sample['processes']) / 1024
    sample['rss_mib'] = sum(p['rss_kib'] for p in sample['processes']) / 1024
    sample['swap_mib'] = sum(p['swap_kib'] for p in sample['processes']) / 1024
    result['samples'].append(sample)
    if index < 4:
        time.sleep(2)
(OUT / (args.label + '.png')).write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
for metric in ['pss_mib', 'rss_mib', 'swap_mib']:
    values = [sample[metric] for sample in result['samples']]
    result[metric + '_mean'] = statistics.mean(values)
    result[metric + '_min'] = min(values)
    result[metric + '_max'] = max(values)
result['focus_after'] = focus()
result['home_after'] = home()
assert result['home_after'] == initial_home, 'Default HOME changed unexpectedly'
result['power_after'] = [line.strip() for line in shell('dumpsys', 'power').splitlines()
                         if 'mWakefulness=' in line or 'mIsPowered=' in line]
result['standby_after'] = json.loads(shell('cat', '/data/local/tmp/lenovo-standby/status.json'))
assert not result['standby_after']['displayOff'], 'Display went to sleep'
result['status'] = 'PASS'
(OUT / (args.label + '.json')).write_text(json.dumps(result, indent=2), encoding='utf-8')
print(json.dumps({'label': args.label, 'pss_mib_mean': result['pss_mib_mean'],
                  'rss_mib_mean': result['rss_mib_mean'], 'processes': len(rows)}), flush=True)
