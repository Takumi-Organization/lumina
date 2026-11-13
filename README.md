# Lumina

Galería multimedia para Android (Kotlin, Compose, MVVM). Diseño y fases: [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

## Estado
- Fase 1: cuadrícula cronológica por días desde MediaStore, miniaturas del sistema, skeleton, estados vacío/sin permiso.
- Fase 2: scrubber temporal (mes/año), selección múltiple (pulsación larga, «elegir día»), favoritos locales + filtro, pellizco para 2–7 columnas, textos en es/en.
- Fase 3: visor (paginador, zoom con pellizco/doble toque, transición compartida, controles que se ocultan, información EXIF y ubicación, compartir, papelera del sistema, favoritos). Vídeo: póster + reproductor del sistema (provisional hasta la fase 4).
- Fase 4: vídeo integrado (Media3 ExoPlayer): reproducir/pausa, progreso, volumen/silencio, doble toque ±10 s, girar a horizontal (modo inmersivo). Pendiente de revisión visual en horizontal.
- Fase 5: álbumes. Manuales (Room v2, migración 1→2): crear/editar nombre y descripción, portada, favoritos, ocultos, añadir desde la selección, quitar, reordenar y orden Manual/fecha/nombre. Automáticos (recientes, cámara, capturas, descargas, fotos, vídeos, favoritas) y carpetas del dispositivo. Pestañas Biblioteca/Álbumes.
- Fase 6: editor no destructivo (desde el visor, solo imágenes): exposición, contraste, saturación, temperatura, sombras, luces, nitidez, viñeta; recorte centrado por proporción, giro 90° y espejo; deshacer/rehacer, restablecer, comparar con el original (mantener pulsado) y guardar copia en Pictures/Lumina (nunca sobrescribe). Vista previa y exportación comparten el mismo pipeline en CPU (`PixelImage`), exportación hasta 4096 px de lado mayor.

Estado de verificación: ver «Verificación» al final.

Pruebas: `./gradlew :app:testDebugUnitTest` y, con emulador, `ANDROID_SERIAL=emulator-5556 ./gradlew :app:connectedDebugAndroidTest` (fijar el serial: si hay un móvil conectado, Gradle intenta instalar también ahí).

## Probar
```
./gradlew :app:testDebugUnitTest :app:assembleDebug
emulator -avd Pixel_API36 -read-only -port 5556 -no-window &   # propio, para no pisar otras sesiones
python3 tools/seed-media.py emulator-5556 120   # siembra fotos/vídeos sintéticos (solo emulador)
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
```
