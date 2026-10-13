"""ADB helper for genuine runtime captures and accessibility-based navigation."""
import os
import json
import hashlib
from datetime import datetime, timezone
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe'
SERIAL = os.environ.get('CIVORA_ADB_SERIAL', 'emulator-5554')
ROOT = Path(__file__).resolve().parent

def adb(*args):
    return subprocess.check_output([str(ADB), '-s', SERIAL, *args], text=True, encoding='utf-8', errors='replace', timeout=45).strip()

def nodes():
    for attempt in range(3):
        result = adb('shell', 'uiautomator', 'dump', '/sdcard/civora-ui.xml')
        if 'dumped to' in result:
            return list(ET.fromstring(adb('shell', 'cat', '/sdcard/civora-ui.xml')).iter('node'))
        if attempt < 2:
            time.sleep(1)
    raise RuntimeError('Fresh UI hierarchy unavailable; refusing stale navigation evidence')

def tap(label):
    matches = [n for n in nodes() if n.get('text') == label or n.get('content-desc') == label]
    if not matches:
        raise ValueError(f'No visible control: {label}')
    n = matches[-1]
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

command = sys.argv[1]
if command == 'sequence':
    for action in json.loads(sys.stdin.read()):
        subprocess.run([sys.executable, __file__, *action], check=True)
    sys.exit(0)
if command == 'list':
    for n in nodes():
        if n.get('text') or n.get('content-desc'):
            print(n.get('text') or n.get('content-desc'), n.get('bounds'))
elif command == 'tap':
    tap(sys.argv[2])
elif command == 'input':
    tap(sys.argv[2])
    time.sleep(0.5)  # Let the focused Compose field/IME settle before typing.
    adb('shell', 'input', 'keycombination', '113', '29')  # Ctrl+A: replace, never append.
    adb('shell', 'input', 'text', sys.argv[3].replace(' ', '%s'))
    time.sleep(0.5)
elif command in ('capture', 'capture-v2', 'capture-final'):
    time.sleep(1)
    visible = nodes()
    if not any(n.get('package') == 'com.sachinkanna.civora' for n in visible):
        raise ValueError('Civora is not visible; refusing to label another app as Civora')
    if not re.fullmatch(r'[A-Za-z0-9_-]+\.png',sys.argv[2]):
        raise ValueError('Use a simple PNG filename')
    directory = 'final/screenshots' if command == 'capture-final' else 'v2/screenshots' if command == 'capture-v2' else 'screenshots'
    destination = ROOT / directory / sys.argv[2]
    destination.parent.mkdir(parents=True, exist_ok=True)
    adb('shell', 'screencap', '-p', '/sdcard/civora-capture.png')
    adb('pull', '/sdcard/civora-capture.png', str(destination))
    if command == 'capture-final':
        # Keep provenance separate from the unmodified Android screenshot.
        manifest_path = ROOT / 'final/capture-manifest.json'
        manifest = json.loads(manifest_path.read_text(encoding='utf-8-sig')) if manifest_path.exists() else {}
        candidate = json.loads((ROOT / 'final/artifact-hashes.json').read_text(encoding='utf-8-sig'))
        manifest[sys.argv[2]] = {
            'capturedAt': datetime.now(timezone.utc).isoformat(),
            'sha256': hashlib.sha256(destination.read_bytes()).hexdigest(),
            'candidateSourceCommit': candidate['sourceCommit'],
            'deviceKind': 'emulator' if SERIAL.startswith('emulator-') else 'physical-android',
            'packageVerified': 'com.sachinkanna.civora',
        }
        manifest_path.write_text(json.dumps(manifest, indent=2)+'\n',encoding='utf-8')
    print(destination)
elif command == 'back':
    adb('shell', 'input', 'keyevent', '4')
elif command == 'hide-keyboard':
    if 'mInputShown=true' in adb('shell', 'dumpsys', 'input_method'):
        adb('shell', 'input', 'keyevent', '4')
else:
    raise ValueError(command)
