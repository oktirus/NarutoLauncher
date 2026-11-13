"""Assemble a portable, allowlisted handoff without private device data or caches."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NAME = 'NarutoLauncher-Native-Parity-20261110'
DEST = ROOT / 'handoff' / NAME


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf8')


def project(source, target):
    for file in source.rglob('*'):
        relative = file.relative_to(source)
        if not file.is_file() or any(p in {'.gradle', 'build', '.idea', '__pycache__', '.git'} for p in relative.parts):
            continue
        if relative.parts[0] in {'verification', 'reviews', 'releases', 'tools'}:
            continue
        if file.name in {'local.properties', 'README.md', 'Build.ps1', '.DS_Store'} or file.suffix.lower() in {'.jks', '.keystore', '.iml'}:
            continue
        if relative.as_posix().startswith('app/src/main/assets/site/'):
            continue
        output = target / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, output)


def prepare():
    assert not DEST.exists(), 'Use the existing draft; do not overwrite it'
    DEST.mkdir(parents=True)
    web = json.loads((ROOT / 'releases/build-manifest.json').read_text(encoding='utf-8-sig'))
    apk = ROOT / 'releases' / web['file']
    assert sha(apk).upper() == web['sha256']
    canonical = []
    source_guard = {}
    with zipfile.ZipFile(apk) as archive:
        for entry in sorted(archive.namelist()):
            if not entry.startswith('assets/site/') or entry.endswith('/'):
                continue
            relative = entry.removeprefix('assets/site/')
            assert '..' not in Path(relative).parts
            data = archive.read(entry)
            original = ROOT / relative
            assert original.is_file() and original.read_bytes() == data, relative
            output = DEST / 'NarutoLauncher' / relative
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(data)
            digest = hashlib.sha256(data).hexdigest()
            canonical.append({'file': relative, 'bytes': len(data), 'sha256': digest})
            source_guard[relative] = digest
    assert len(canonical) == 199
    dump(DEST / 'catalogs/chromium-site-manifest.json', canonical)
    project(ROOT / 'android', DEST / 'NarutoLauncher/android')
    project(ROOT / 'native', DEST / 'NarutoLauncher/native')
    for source in (ROOT / 'android', ROOT / 'native/app/src'):
        for path in source.rglob('*'):
            if path.is_file() and not any(x in {'.gradle', 'build', 'site'} for x in path.relative_to(source).parts):
                if path.name != 'local.properties':
                    source_guard[path.relative_to(ROOT).as_posix()] = sha(path)
    (DEST / 'LICENSES').mkdir()
    for name in ('OFL-Caveat.txt', 'OFL-NunitoSans.txt'):
        shutil.copy2(ROOT / 'assets/fonts' / name, DEST / 'LICENSES' / name)
    shutil.copy2(ROOT / 'LICENSE-cleanyourtv', DEST / 'LICENSES/LICENSE-cleanyourtv')
    refs = json.loads((ROOT / 'revisions/v3/reference-manifest.json').read_text())
    for row in refs:
        if row.get('file'):
            source = ROOT / row['file']
            assert sha(source) == row['sha256'], row['file']
            output = DEST / 'supplemental' / row['file']
            output.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, output)
    dump(DEST / 'supplemental/revisions/v3/reference-manifest.json', refs)
    (DEST / 'artifacts').mkdir()
    shutil.copy2(apk, DEST / 'artifacts' / apk.name)
    native = json.loads((ROOT / 'native/releases/build-manifest.json').read_text(encoding='utf-8-sig'))
    native_apk = ROOT / 'native/releases' / native['file']
    assert sha(native_apk).upper() == native['sha256']
    shutil.copy2(native_apk, DEST / 'artifacts' / native_apk.name)
    for label, manifest in [('chromium-v26', web), ('native-preview', native)]:
        dump(DEST / 'artifacts' / (label + '-manifest.json'), {k: v for k, v in manifest.items() if k not in {'build_staging'}})
    shutil.copy2(ROOT / 'native/verification/asset-manifest.json', DEST / 'catalogs/native-asset-map.json')
    (DEST / 'NarutoLauncher/tools').mkdir()
    for name in ('test_runner_v9.cjs', 'browser_v26_checks.js', 'browser_v26_navigation_checks.js',
                 'browser_v24_launch_checks.js', 'browser_v24_launch_edge_checks.js', 'browser_v17_order_checks.js'):
        shutil.copy2(ROOT / 'tools' / name, DEST / 'NarutoLauncher/tools' / name)
    verification = DEST / 'NarutoLauncher/native/verification'
    verification.mkdir(parents=True)
    shutil.copy2(ROOT / 'native/verification/audio_regression_probe.py', verification / 'audio_regression_probe.py')
    (DEST / 'evidence/legacy-tests').mkdir(parents=True)
    for path in (ROOT / 'native/verification').glob('TEST-*.xml'):
        # Keep only names/counts; raw Robolectric logs contain local host paths.
        suite = ET.parse(path).getroot()
        dump(DEST / 'evidence/legacy-tests' / (path.stem + '.json'),
             {'suite': suite.get('name'), 'tests': suite.get('tests'), 'failures': suite.get('failures'),
              'errors': suite.get('errors'), 'case_names': [c.get('name') for c in suite.findall('testcase')]})
    live = ROOT / 'native/verification/lenovo-native-20261109-215405'
    (DEST / 'evidence/device').mkdir(parents=True)
    for src, dst in [('round-1-web-v26.png', 'CHROMIUM-v26-1366x768.png'),
                     ('native-after-navigation.png', 'NATIVE-preview-NOT-a-visual-reference.png')]:
        shutil.copy2(live / src, DEST / 'evidence/device' / dst)
    (DEST / 'evidence/native-software-render').mkdir()
    for path in (ROOT / 'native/verification/native-ui').glob('*.png'):
        shutil.copy2(path, DEST / 'evidence/native-software-render' / path.name)
    dump(DEST / 'SOURCE_OF_TRUTH.json', {
        'date': '2026-11-10', 'visual_authority': 'NarutoLauncher/prototype + assets, verified against the Chromium v26 APK',
        'chromium_apk_sha256': sha(apk), 'native_apk_sha256': sha(native_apk),
        'native_preview_is_visual_authority': False, 'canonical_site_files': len(canonical),
        'supplemental_references': len(refs),
        'known_reference_packaging_gap': 'references panel requests revisions/v3/reference-manifest.json, absent from v26 APK and disallowed by its origin router; supplemental/ preserves the 33 original reference files without modifying canonical site',
        'historical_device_capture': {'physical_pixels': [1366, 768], 'reported_css_viewport': [963, 541],
                                      'dpr': None, 'dpr_note': 'Not recorded; do not infer an exact DPR by dividing rounded innerWidth',
                                      'captured_at': '2026-11-09', 'live_test_on_recipient_machine': False},
        'historical_home_pss_mib': {'web_v26_mean': 232.30439453125, 'native_after_navigation_mean': 61.480859375,
                                     'audio': True, 'ambience': False, 'note': 'Home only; not a visual-parity acceptance result'},
        'origin': 'Uncommitted working-tree snapshot, NOT git HEAD. Every canonical web file equals the v26 APK byte-for-byte.'
    })
    dump(DEST / 'catalogs/original-source-guard.json', source_guard)
    catalog = []
    try:
        from PIL import Image
    except ImportError:
        Image = None
    for row in canonical:
        path = DEST / 'NarutoLauncher' / row['file']
        item = dict(row)
        if Image and path.suffix.lower() in {'.png', '.jpg', '.webp'}:
            with Image.open(path) as im:
                item.update(width=im.width, height=im.height, mode=im.mode, format=im.format)
        catalog.append(item)
    dump(DEST / 'catalogs/assets-and-source.json', catalog)
    html = (ROOT / 'prototype/index.html').read_text(encoding='utf8')
    dump(DEST / 'catalogs/load-order.json', {
        'stylesheets': re.findall(r'<link[^>]+rel="stylesheet"[^>]+href="([^"]+)"', html),
        'scripts': re.findall(r'<script[^>]+src="([^"]+)"', html),
        'note': 'Order and later @media overrides are significant; do not port style.css in isolation.'
    })
    print('Draft:', DEST)


def seal():
    guard = json.loads((DEST / 'catalogs/original-source-guard.json').read_text(encoding='utf8'))
    assert all((ROOT / p).is_file() and sha(ROOT / p) == digest for p, digest in guard.items()), 'Original source changed during handoff assembly'
    required = ('README_START_HERE.md', 'PROMPT_FOR_AGENT_UK.md', 'AGENTS.md', 'docs/TZ_CHROMIUM_V26_UK.md',
                'docs/MIGRATION_MAP_UK.md', 'docs/VISUAL_ACCEPTANCE_UK.md', 'docs/KNOWN_NATIVE_GAPS_UK.md',
                'docs/BUILD_AND_RUN.md', 'tools/verify_handoff.py', 'tools/build.py', 'tools/serve_reference.py',
                'tools/capture_reference.py', 'tools/image_diff.py', 'catalogs/reference-state.json')
    for name in required:
        assert (DEST / name).is_file(), name
    blocked = {'.git', '.gradle', '.idea', 'node_modules', '__pycache__', 'audio-probe-stage'}
    rows = []
    for path in sorted(DEST.rglob('*')):
        if not path.is_file() or path.name == 'HANDOFF_MANIFEST.json':
            continue
        relative = path.relative_to(DEST)
        assert not (set(relative.parts) & blocked), relative
        assert path.name not in {'local.properties', '.env', 'settings.json'}, relative
        assert path.suffix.lower() not in {'.jks', '.keystore', '.dpapi', '.pfx', '.pem', '.pyc'}, relative
        rows.append({'file': relative.as_posix(), 'bytes': path.stat().st_size, 'sha256': sha(path)})
    dump(DEST / 'HANDOFF_MANIFEST.json', {'format': 1, 'date': '2026-11-10', 'original_source_unchanged': True, 'files': rows})
    output = DEST.with_suffix('.zip')
    assert not output.exists(), 'Do not overwrite a previously delivered archive'
    with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(DEST.rglob('*')):
            if path.is_file():
                archive.write(path, NAME + '/' + path.relative_to(DEST).as_posix())
    with zipfile.ZipFile(output) as archive:
        assert archive.testzip() is None
        for row in rows:
            assert hashlib.sha256(archive.read(NAME + '/' + row['file'])).hexdigest() == row['sha256']
    output.with_suffix('.zip.sha256').write_text(sha(output).upper() + '  ' + output.name + '\n', encoding='utf8')
    print(json.dumps({'archive': str(output), 'bytes': output.stat().st_size, 'sha256': sha(output), 'files': len(rows) + 1}, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('phase', choices=['prepare', 'seal'])
    args = parser.parse_args()
    prepare() if args.phase == 'prepare' else seal()
