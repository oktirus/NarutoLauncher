"""Install the authorized separate preview and measure matched foreground PSS.

No clear-data, HOME change, reboot or power/standby configuration changes.
"""
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
from zoneinfo import ZoneInfoNotFoundError
import hashlib
import json
import re
import shlex
import statistics
import subprocess
import time
import xml.etree.ElementTree as ET

try:
    KYIV = ZoneInfo('Europe/Kyiv')
except ZoneInfoNotFoundError:
    # Windows has the Kyiv DST rules without a Python tzdata installation.
    current = subprocess.check_output(['pwsh', '-NoProfile', '-Command',
        "[TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow, 'FLE Standard Time').ToString('o')"],
        text=True).strip()
    KYIV = datetime.fromisoformat(current).tzinfo

NATIVE = Path(__file__).resolve().parents[1]
ROOT = NATIVE.parent
SERIAL = '192.168.100.26:5555'
WEB = 'ua.konoha.tv'
NEW = 'ua.konoha.tv.nativepreview'
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
BROWSER = Path.home() / 'AppData/Roaming/npm/node_modules/agent-browser/bin/agent-browser-win32-x64.exe'
OUT = NATIVE / 'verification' / ('lenovo-native-' + datetime.now(KYIV).strftime('%Y%m%d-%H%M%S'))
OUT.mkdir(parents=True, exist_ok=False)
JOURNAL = []
SUMMARY = {'device': SERIAL, 'started_at': datetime.now(KYIV).isoformat(),
           'warmup_seconds': 30, 'samples_per_round': 5, 'rounds': [], 'status': 'running'}


def save():
    (OUT / 'journal.json').write_text(json.dumps(JOURNAL, ensure_ascii=False, indent=2), encoding='utf8')
    (OUT / 'results.json').write_text(json.dumps(SUMMARY, ensure_ascii=False, indent=2), encoding='utf8')


def run(*args, data=None, binary=False, timeout=90):
    result = subprocess.run([str(ADB), '-s', SERIAL, *args], input=data, capture_output=True, timeout=timeout)
    JOURNAL.append({'time': datetime.now(KYIV).isoformat(), 'command': ['adb', '-s', SERIAL, *args],
                    'exit_code': result.returncode,
                    'output': f'{len(result.stdout)} binary bytes' if binary else result.stdout.decode('utf8', errors='replace').strip(),
                    'error': result.stderr.decode('utf8', errors='replace').strip()})
    save()
    if result.returncode:
        raise RuntimeError('ADB failed: ' + ' '.join(args))
    return result.stdout if binary else result.stdout.decode('utf8', errors='replace').strip()


def shell(*args, **kwargs):
    return run('shell', '-T', shlex.join(args), **kwargs)


def home():
    return shell('cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')


def focus():
    raw = shell('dumpsys', 'window')
    return [line.strip() for line in raw.splitlines() if 'mCurrentFocus=' in line]


def associated(package):
    raw = shell('dumpsys', 'activity', 'processes')
    rows = []
    for block in re.split(r'(?m)(?=^\s+\*APP\* UID )', raw)[1:]:
        proc = re.search(r'ProcessRecord\{\S+ (\d+):([^\s}]+)', block)
        packages = re.search(r'packageList=\{([^}]+)\}', block)
        if proc and packages and package in [s.strip() for s in packages[1].split(',')]:
            rows.append((int(proc[1]), proc[2]))
    return rows, raw


def measure(package, label):
    rows, processes = associated(package)
    assert rows, ('No associated processes', package)
    (OUT / f'{label}-processes.txt').write_text(processes, encoding='utf8')
    samples = []
    for i in range(5):
        assert any(package + '/' in line for line in focus()), 'Measured app is not foreground'
        current, _ = associated(package)
        assert current == rows, 'Process set changed during sampling; inspect before retry'
        records = []
        for pid, name in rows:
            raw = shell('dumpsys', 'meminfo', str(pid))
            (OUT / f'{label}-{i}-{pid}-meminfo.txt').write_text(raw, encoding='utf8')
            m = re.search(r'TOTAL PSS:\s+(\d+)\s+TOTAL RSS:\s+(\d+)\s+TOTAL SWAP PSS:\s+(\d+)', raw)
            assert m, (pid, 'Missing App Summary')
            records.append({'pid': pid, 'name': name, 'pss_kib': int(m[1]), 'rss_kib': int(m[2]), 'swap_pss_kib': int(m[3])})
        samples.append({'time': datetime.now(KYIV).isoformat(), 'processes': records,
                        'pss_mib': sum(r['pss_kib'] for r in records) / 1024,
                        'swap_pss_mib': sum(r['swap_pss_kib'] for r in records) / 1024})
        if i < 4:
            time.sleep(2)
    result = {'package': package, 'label': label, 'samples': samples,
              'pss_mib_mean': statistics.mean(s['pss_mib'] for s in samples),
              'pss_mib_min': min(s['pss_mib'] for s in samples), 'pss_mib_max': max(s['pss_mib'] for s in samples),
              'swap_pss_mib_mean': statistics.mean(s['swap_pss_mib'] for s in samples)}
    SUMMARY['rounds'].append(result)
    save()
    print(label, f"{result['pss_mib_mean']:.2f} MiB PSS, {len(rows)} processes", flush=True)


def read_web_preferences():
    result = subprocess.run([str(BROWSER), '--session', 'konoha-native-ram', '--json', 'eval',
                             '({prefs:{...prefs},modal,ambient,viewport:{width:innerWidth,height:innerHeight}})'],
                            capture_output=True, text=True, encoding='utf8', timeout=45, check=True)
    message = json.loads(result.stdout)
    assert message['success'], 'Cannot inspect WebView preferences'
    state = message['data']['result']
    assert not state['modal'] and not state['ambient'], 'Baseline must be the home screen'
    SUMMARY['web_state'] = state
    save()
    result = subprocess.run([str(BROWSER), '--session', 'konoha-native-ram', '--json', 'close'],
                            capture_output=True, text=True, encoding='utf8', timeout=45, check=True)
    assert json.loads(result.stdout)['success']
    run('forward', '--remove', 'tcp:18912')
    return state['prefs']


def preferences_xml(prefs):
    root = ET.Element('map')
    for key in ('audio', 'ambience', 'motion'):
        ET.SubElement(root, 'boolean', name=key, value=str(prefs[key]).lower())
    for key in ('volume', 'musicVolume'):
        ET.SubElement(root, 'float', name=key, value=str(prefs[key]))
    for key in ('rotation', 'idle'):
        ET.SubElement(root, 'int', name=key, value=str(prefs[key]))
    for key in ('scene', 'technique', 'highlightSound', 'music'):
        ET.SubElement(root, 'string', name=key).text = prefs[key]
    if prefs['pins'] is not None:
        ET.SubElement(root, 'string', name='pins').text = json.dumps(prefs['pins'], ensure_ascii=False)
    return ET.tostring(root, encoding='utf-8', xml_declaration=True)


def snapshot(label):
    (OUT / (label + '.png')).write_bytes(run('exec-out', 'screencap', '-p', binary=True))


def main():
    installed = False
    original_home = None
    try:
        assert shell('getprop', 'ro.product.model') == 'LNVNB161216'
        assert shell('getprop', 'ro.product.device') == 'x86_64_tv_go'
        assert shell('getprop', 'ro.build.version.sdk') == '34'
        original_home = home()
        assert original_home.splitlines()[-1] == WEB + '/.MainActivity', original_home
        assert not shell('pm', 'list', 'packages', NEW), 'Native package already exists; back it up before an update'
        SUMMARY['before'] = {'home': original_home, 'focus': focus(),
                             'power': shell('dumpsys', 'power'),
                             'standby': json.loads(shell('cat', '/data/local/tmp/lenovo-standby/status.json')),
                             'web_package': shell('dumpsys', 'package', WEB)}
        assert not SUMMARY['before']['standby']['displayOff'], 'Display is off'
        manifest = json.loads((NATIVE / 'releases/build-manifest.json').read_text(encoding='utf-8-sig'))
        apk = NATIVE / 'releases' / manifest['file']
        assert hashlib.sha256(apk.read_bytes()).hexdigest() == manifest['sha256'].lower()
        SUMMARY['candidate'] = manifest
        old_path = shell('pm', 'path', WEB).removeprefix('package:')
        old_apk = run('exec-out', 'cat', old_path, binary=True)
        (OUT / 'web-v26-before.apk').write_bytes(old_apk)
        SUMMARY['web_apk_sha256'] = hashlib.sha256(old_apk).hexdigest()
        prefs = read_web_preferences()
        save()
        print('Identity and backups verified; installing separate Native Preview', flush=True)
        assert 'Success' in run('install', str(apk), timeout=180)
        installed = True
        native_path = shell('pm', 'path', NEW).removeprefix('package:')
        assert shell('sha256sum', native_path).split()[0].upper() == manifest['sha256']
        shell('run-as', NEW, 'mkdir', '-p', 'shared_prefs')
        xml = preferences_xml(prefs)
        shell('run-as', NEW, 'sh', '-c', 'cat > shared_prefs/konoha_preferences_v1.xml', data=xml)
        assert run('exec-out', 'run-as', NEW, 'cat', 'shared_prefs/konoha_preferences_v1.xml', binary=True) == xml
        SUMMARY['native_preferences_matched'] = True
        assert home() == original_home
        # ABBA order; each launcher starts in a new process with the same settings.
        for index, package in enumerate((WEB, NEW, NEW, WEB), 1):
            other = NEW if package == WEB else WEB
            shell('am', 'force-stop', other)
            launch = shell('am', 'start', '-S', '-W', '-n', package + '/.MainActivity')
            assert 'Status: ok' in launch, launch
            print(f'Round {index}/4: {package}; warming 30 seconds', flush=True)
            time.sleep(30)
            label = f'round-{index}-' + ('native' if package == NEW else 'web-v26')
            measure(package, label)
            snapshot(label)
        SUMMARY['aggregate'] = {}
        for package in (WEB, NEW):
            values = [s['pss_mib'] for r in SUMMARY['rounds'] if r['package'] == package for s in r['samples']]
            SUMMARY['aggregate'][package] = {'pss_mib_mean': statistics.mean(values),
                                              'pss_mib_min': min(values), 'pss_mib_max': max(values), 'samples': len(values)}
        old = SUMMARY['aggregate'][WEB]['pss_mib_mean']
        new = SUMMARY['aggregate'][NEW]['pss_mib_mean']
        SUMMARY['savings'] = {'pss_mib': old - new, 'percent': (old - new) / old * 100}
        SUMMARY['status'] = 'PASS'
    except BaseException as error:
        SUMMARY['status'] = 'FAILED'
        SUMMARY['failure'] = str(error)
        raise
    finally:
        # Leave the authorized newly installed preview open, without changing HOME.
        if installed:
            SUMMARY['final_launch'] = shell('am', 'start', '-W', '-n', NEW + '/.MainActivity')
            time.sleep(3)
            SUMMARY['final_focus'] = focus()
            SUMMARY['final_power'] = shell('dumpsys', 'power')
            SUMMARY['final_standby'] = json.loads(shell('cat', '/data/local/tmp/lenovo-standby/status.json'))
            SUMMARY['final_native_package'] = shell('dumpsys', 'package', NEW)
            SUMMARY['fatal_log'] = shell('logcat', '-d', '-b', 'crash', '-v', 'threadtime', '-t', '150')
            snapshot('native-final')
        if original_home:
            SUMMARY['home_unchanged'] = home() == original_home
            assert SUMMARY['home_unchanged'], 'Unexpected HOME change'
        save()
        print('Evidence:', OUT, flush=True)
    print(json.dumps({'aggregate': SUMMARY['aggregate'], 'savings': SUMMARY['savings'],
                      'home_unchanged': SUMMARY['home_unchanged']}, indent=2), flush=True)


if __name__ == '__main__':
    main()
