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

expected = {
    "b2ded9ae5a20fa36ca8cec49ef923bffb5e1a51d9e8c1f8336273d2fa9d35ff0",
    "b670602fa00934ca27c4351bb0efe7ea7a07fae57284e44226025eeed7c51254",
}
folder = "/sdcard/Android/data/com.saisonflow.app/files/Movies"
deadline = time.monotonic() + 150
while time.monotonic() < deadline:
    listing = adb("shell", "find", folder, "-name", "*.mp4").decode().splitlines()
    if len(listing) == 2:
        actual = {adb("shell", "sha256sum", path).decode().split()[0] for path in listing}
        if actual == expected:
            print("Deux fichiers MP4 complets, SHA-256 identiques aux originaux W3C.")
            break
    time.sleep(3)
else:
    raise RuntimeError("Intégrité des deux téléchargements non confirmée")
