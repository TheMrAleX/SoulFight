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
    path = '/data/local/tmp/fightforge-ui.xml'
    adb('shell', 'rm', '-f', path)
    result = adb('shell', 'uiautomator', 'dump', path)
    with (out/'uiautomator.log').open('ab') as log:
        log.write(result + b'\n')
    try:
        data = adb('shell', 'cat', path)
    except subprocess.CalledProcessError:
        return None
    (out/'last-ui.xml').write_bytes(data)
    return ET.fromstring(data)

def tap(label):
    for _ in range(12):
        root = tree()
        if root is None:
            time.sleep(1)
            continue
        tutorial = next((n for n in root.iter('node') if n.get('package') == 'android' and (n.get('text') or '').upper() == 'GOT IT'), None)
        if tutorial is not None:
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', tutorial.get('bounds')))
            adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            continue
        for node in root.iter('node'):
            values=[node.get('text') or '',node.get('content-desc') or '']
            if label in [' '.join(text.split()) for text in values]:
                x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
                time.sleep(1)
                return
        time.sleep(1)
    raise AssertionError(f'Missing visible action: {label}')

def enter_text(text):
    root = tree()
    fields = [n for n in root.iter('node') if n.get('class') == 'android.widget.EditText']
    assert fields, 'No editable field visible'
    x1,y1,x2,y2=map(int,re.findall(r'\d+',fields[0].get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    adb('shell','input','text',text)
    info=adb('shell','dumpsys','input_method').decode()
    if 'mInputShown=true' in info or 'mIsInputViewShown=true' in info:
        adb('shell','input','keyevent','4')
    time.sleep(1)

def assert_toggle(label, expected):
    root=tree()
    parents={child:parent for parent in root.iter() for child in parent}
    for n in root.iter('node'):
        if n.get('text')==label:
            control=n
            while control is not None and control.get('checkable')!='true':
                control=parents.get(control)
            assert control is not None, f'Missing accessible toggle: {label}'
            assert (control.get('checked')=='true')==expected, f'{label}: incorrect checked state'
            return
    raise AssertionError(f'Missing accessible toggle label: {label}')

def run():
    adb('shell', 'am', 'force-stop', 'com.fightforge.game')
    adb('logcat', '-c')
    adb('shell', 'am', 'start', '-W', '-n', 'com.fightforge.game/.MainActivity')
    tap('TIENDA')
    tap('300 MONEDAS')
    tap('CANCELAR')
    (out/'shop.png').write_bytes(adb('exec-out','screencap','-p'))
    tap('‹ VOLVER')
    tap('UN JUGADOR')
    tap('CONTRA LA MÁQUINA')
    (out/'selection.png').write_bytes(adb('exec-out','screencap','-p'))
    tap('CONTINUAR')
    tap('PELEAR')
    time.sleep(3)
    image = adb('exec-out', 'screencap', '-p')
    (out/'combat.png').write_bytes(image)
    width, height = struct.unpack('>II', image[16:24])
    assert width > height, 'The game must be landscape'
    scale = min(width/1920, height/1080)
    x = (width-1920*scale)/2+960*scale
    density_text = adb("shell", "wm", "density").decode()
    density = int(re.findall(r"\d+", density_text)[-1])/160
    y = (height-1080*scale)/2+124*density
    adb('shell', 'input', 'tap', str(int(x)), str(int(y)))
    tap('LISTA DE MOVIMIENTOS')
    tap('CERRAR')
    tap('CONTINUAR')
    time.sleep(2)
    adb('shell', 'input', 'tap', str(int(x)), str(int(y)))
    tap('SALIR')
    tap('CREACIÓN')
    tap('PERSONAJES')
    tap('CREAR')
    enter_text('ARCADETEST')
    tap('FOTO')
    tap('FABRICAR')
    tap('LIENZO DE PÍXELES')
    tap('GUARDAR ANIMACIÓN')
    tap('ANIMACIONES')
    tap('QUIETO · 0 FRAMES')
    tap('FABRICAR')
    tap('LIENZO DE PÍXELES')
    tap('GUARDAR ANIMACIÓN')
    tap('GUARDAR')
    tap('‹ VOLVER')
    visible=False
    for _ in range(8):
        root=tree()
        if root is not None and any(n.get('text')=='ARCADETEST' for n in root.iter('node')):
            visible=True
            break
        scroll=next((n for n in root.iter('node') if n.get('scrollable')=='true'),None) if root is not None else None
        if scroll is not None:
            a,b,c,d=map(int,re.findall(r'\d+',scroll.get('bounds')))
            adb('shell','input','swipe',str((a+c)//2),str(d-20),str((a+c)//2),str(b+20),'500')
        time.sleep(1)
    assert visible, 'The character created through the editor was not persisted'
    tap('‹ VOLVER')
    tap('‹ VOLVER')
    tap('UN JUGADOR')
    tap('PRÁCTICA')
    tap('CONTINUAR')
    tap('PELEAR')
    time.sleep(2)
    adb('shell','input','tap',str(int(x)),str(int(y)))
    assert_toggle('HITBOXES',True)
    assert_toggle('ENTRADAS',True)
    (out/'practice-pause.png').write_bytes(adb('exec-out','screencap','-p'))
    tap('HITBOXES')
    assert_toggle('HITBOXES',False)
    tap('ENTRADAS')
    assert_toggle('ENTRADAS',False)
    tap('CONTINUAR')
    time.sleep(1)
    (out/'practice.png').write_bytes(adb('exec-out','screencap','-p'))
    adb('shell','input','tap',str(int(x)),str(int(y)))
    tap('SALIR')
    logs = adb('logcat', '-d', '-s', 'AndroidRuntime:E').decode()
    (out/'runtime.log').write_text(logs)
    assert 'Process: com.fightforge.game' not in logs, logs
    assert adb('shell', 'pidof', 'com.fightforge.game').strip(), 'The app exited'
    (out/'result.txt').write_text('PASS: menu → selection → setup → combat → pause → moves → resume → create character → paint photo and idle → save and list; shop confirmation/cancel; practice toggles; landscape; no app fatal exception.\n')
    print((out/'result.txt').read_text())

try:
    run()
except Exception:
    for name, args in [
        ('failure.png', ('exec-out', 'screencap', '-p')),
        ('logcat-full.txt', ('logcat','-d')),
        ('window.txt', ('shell','dumpsys','window')),
        ('graphics.txt', ('shell','dumpsys','gfxinfo','com.fightforge.game')),
        ('runtime.log', ('logcat', '-d', '-s', 'AndroidRuntime:E', 'ActivityTaskManager:I', 'ActivityManager:I')),
    ]:
        try:
            (out/name).write_bytes(adb(*args))
        except Exception as diagnostic_error:
            (out/(name+'.error.txt')).write_text(str(diagnostic_error))
    raise
