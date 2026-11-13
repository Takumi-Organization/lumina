#!/usr/bin/env python3
"""Genera fotos sintéticas con EXIF DateTimeOriginal repartido en varios años y las empuja
al emulador (DCIM/Camera). Uso: seed-media.py <serial> [n=120]. NUNCA apuntar al móvil real."""
import random, subprocess, sys, tempfile, os, datetime as dt
from PIL import Image, ImageDraw
serial, n = sys.argv[1], int(sys.argv[2]) if len(sys.argv) > 2 else 120
random.seed(7)
tmp = tempfile.mkdtemp(prefix="lumina-seed-")
adb = lambda *a: subprocess.run(["adb", "-s", serial, *a], check=True, capture_output=True)
adb("shell", "mkdir", "-p", "/sdcard/DCIM/Camera", "/sdcard/Pictures/Screenshots")
now = dt.datetime.now()
for i in range(n):
    when = now - dt.timedelta(days=int(random.triangular(0, 1800, 20)), hours=random.randint(0, 20), minutes=random.randint(0, 59))
    w, h = random.choice([(1600, 1200), (1200, 1600), (1920, 1080), (1000, 1000)])
    hue = random.random()
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(0, h, 8):  # bandas de color, sin degradado continuo
        c = tuple(int(40 + 180 * abs(((hue + y / h * .6 + k / 3) % 1) - .5) * 2) for k in range(3))
        d.rectangle([0, y, w, y + 8], fill=c)
    d.ellipse([w * .3, h * .3, w * .7, h * .7], outline=(255, 255, 255), width=12)
    d.text((40, 40), f"#{i} {when:%Y-%m-%d}", fill=(255, 255, 255))
    ex = img.getexif(); ex[306] = when.strftime("%Y:%m:%d %H:%M:%S")
    sub = ex.get_ifd(0x8769); sub[36867] = when.strftime("%Y:%m:%d %H:%M:%S")
    shot = i % 15 == 0
    name = f"{'Screenshot' if shot else 'IMG'}_{when:%Y%m%d_%H%M%S}_{i}.jpg"
    p = os.path.join(tmp, name); img.save(p, quality=85, exif=ex)
    os.utime(p, (when.timestamp(), when.timestamp()))
    adb("push", p, ("/sdcard/Pictures/Screenshots/" if shot else "/sdcard/DCIM/Camera/") + name)
for j in range(3):
    when = now - dt.timedelta(days=j * 40 + 1)
    p = os.path.join(tmp, f"VID_{when:%Y%m%d}_{j}.mp4")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", f"testsrc=size=1280x720:rate=30:duration={8 + j * 7}",
                    "-f", "lavfi", "-i", "sine=frequency=440:duration=15", "-shortest", "-pix_fmt", "yuv420p",
                    "-metadata", f"creation_time={when:%Y-%m-%dT%H:%M:%S}", p], check=True)
    os.utime(p, (when.timestamp(), when.timestamp()))
    adb("push", p, f"/sdcard/DCIM/Camera/{os.path.basename(p)}")
adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file:///sdcard/DCIM/Camera")
print("sembrado", n, "fotos y 3 vídeos en", serial)
