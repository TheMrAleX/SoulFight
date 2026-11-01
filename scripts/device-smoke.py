#!/usr/bin/env python3
"""Exercise the actual Android navigation into combat and back to pause."""
from pathlib import Path
import re
import os
import struct
import subprocess
import time
import xml.etree.ElementTree as ET

out = Path('/tmp/fightforge-device-evidence')
out.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], timeout=int(os.environ.get("ADB_TIMEOUT", "30")))

def tree():
    adb('shell', 'uiautomator', 'dump', '/sdcard/fightforge-ui.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/fightforge-ui.xml'))

def tap(label):
    for _ in range(12):
        root = tree()
        for node in root.iter('node'):
            if node.get('text') == label:
                x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
                time.sleep(1)
                return
        time.sleep(1)
    raise AssertionError(f'Missing visible action: {label}')

adb('shell', 'am', 'force-stop', 'com.fightforge.game')
adb('logcat', '-c')
adb('shell', 'am', 'start', '-W', '-n', 'com.fightforge.game/.MainActivity')
tap('UN JUGADOR')
tap('CONTRA LA MÁQUINA')
tap('CONTINUAR')
tap('PELEAR')
time.sleep(3)
image = adb('exec-out', 'screencap', '-p')
(out/'combat.png').write_bytes(image)
width, height = struct.unpack('>II', image[16:24])
assert width > height, 'The game must be landscape'
scale = min(width/1920, height/1080)
x = (width-1920*scale)/2+960*scale
y = (height-1080*scale)/2+220*scale
adb('shell', 'input', 'tap', str(int(x)), str(int(y)))
tap('LISTA DE MOVIMIENTOS')
tap('CERRAR')
tap('CONTINUAR')
time.sleep(2)
logs = adb('logcat', '-d', '-s', 'AndroidRuntime:E').decode()
(out/'runtime.log').write_text(logs)
assert 'Process: com.fightforge.game' not in logs, logs
assert adb('shell', 'pidof', 'com.fightforge.game').strip(), 'The app exited'
(out/'result.txt').write_text('PASS: menu → selection → setup → combat → pause → moves → resume; landscape; no app fatal exception.\n')
print((out/'result.txt').read_text())
