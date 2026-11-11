import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=20)


def find(label, tap=False):
    until = time.monotonic() + 45
    while time.monotonic() < until:
        adb("shell", "uiautomator", "dump", "/sdcard/window.xml")
        root = ET.fromstring(adb("shell", "cat", "/sdcard/window.xml"))
        for node in root.iter("node"):
            if node.get("text", "").casefold() == label.casefold():
                if tap:
                    x, y, right, bottom = map(int, re.findall(r"\d+", node.get("bounds")))
                    adb("shell", "input", "tap", str((x + right)//2), str((y + bottom)//2))
                return
        time.sleep(1)
    raise RuntimeError("Contrôle absent : " + label)


find("Miruro", True)
find("Aucune source téléchargeable autorisée disponible actuellement.")
adb("shell", "input", "keyevent", "4")
find("Bandes-annonces Blender · Démonstration", True)
find("Télécharger")
Path("SaisonFlow-fiche.png").write_bytes(adb("exec-out", "screencap", "-p"))
find("Télécharger", True)
find("Ajouter à la file")
Path("SaisonFlow-selection.png").write_bytes(adb("exec-out", "screencap", "-p"))
find("Ajouter à la file", True)
find("2 sur 2 ajouté(s) à la file")
