"""ADB helper for genuine runtime captures and accessibility-based navigation."""
import os
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = Path(os.environ['LOCALAPPDATA']) / 'Android/Sdk/platform-tools/adb.exe'
ROOT = Path(__file__).resolve().parent

def adb(*args):
    return subprocess.check_output([str(ADB), *args], text=True, encoding='utf-8', errors='replace').strip()

def nodes():
    result = adb('shell', 'uiautomator', 'dump', '/sdcard/civora-ui.xml')
    if 'dumped to' not in result:
        raise RuntimeError('UI hierarchy unavailable; refusing stale navigation evidence')
    return list(ET.fromstring(adb('shell', 'cat', '/sdcard/civora-ui.xml')).iter('node'))

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
    adb('shell', 'input', 'text', sys.argv[3].replace(' ', '%s'))
elif command == 'capture':
    time.sleep(1)
    visible = nodes()
    if not any(n.get('package') == 'com.sachinkanna.civora' for n in visible):
        raise ValueError('Civora is not visible; refusing to label another app as Civora')
    destination = ROOT / 'screenshots' / sys.argv[2]
    destination.parent.mkdir(exist_ok=True)
    adb('shell', 'screencap', '-p', '/sdcard/civora-capture.png')
    adb('pull', '/sdcard/civora-capture.png', str(destination))
    print(destination)
elif command == 'back':
    adb('shell', 'input', 'keyevent', '4')
elif command == 'hide-keyboard':
    if 'mInputShown=true' in adb('shell', 'dumpsys', 'input_method'):
        adb('shell', 'input', 'keyevent', '4')
else:
    raise ValueError(command)
