#!/usr/bin/env python3
"""Genera N fotos JPEG pequeñas (480x360) con EXIF de fecha repartido en ~10 años, para medir rendimiento con una
biblioteca grande real. Uso: seed-perf.py <carpeta_salida> [N=10000]. Después: tar + adb push a /sdcard/DCIM/LuminaPerf
(solo emulador, nunca el móvil real) y `content call --uri content://media/external_primary --method scan_volume --arg external_primary`.
Limpieza: borrar /sdcard/DCIM/LuminaPerf y repetir el escaneo."""
import os, random, datetime as dt, sys
from multiprocessing import Pool
from PIL import Image, ImageDraw
OUT=sys.argv[1]; N=int(sys.argv[2]) if len(sys.argv)>2 else 10000
now=dt.datetime(2026,11,13,12,0)
def make(i):
    rnd=random.Random(i)
    when=now-dt.timedelta(days=int(rnd.triangular(0,3650,300)),minutes=rnd.randint(0,1439))
    img=Image.new("RGB",(480,360)); d=ImageDraw.Draw(img)
    hue=rnd.random()
    for y in range(0,360,12):
        c=tuple(int(40+180*abs(((hue+y/360*.6+k/3)%1)-.5)*2) for k in range(3)); d.rectangle([0,y,480,y+12],fill=c)
    d.ellipse([140,100,340,260],outline=(255,255,255),width=6); d.text((20,20),f"#{i}",fill=(255,255,255))
    ex=img.getexif(); ex[306]=when.strftime("%Y:%m:%d %H:%M:%S"); ex.get_ifd(0x8769)[36867]=when.strftime("%Y:%m:%d %H:%M:%S")
    p=os.path.join(OUT,f"PERF_{when:%Y%m%d_%H%M%S}_{i}.jpg"); img.save(p,quality=70,exif=ex)
    os.utime(p,(when.timestamp(),when.timestamp()))
if __name__=="__main__":
    os.makedirs(OUT,exist_ok=True)
    with Pool(8) as p: p.map(make,range(N),chunksize=200)
    print("ok",len(os.listdir(OUT)))
