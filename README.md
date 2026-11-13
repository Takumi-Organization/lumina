# Lumina

Galería multimedia para Android (Kotlin, Compose, MVVM). Diseño y fases: [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

## Estado
- Fase 1: cuadrícula cronológica por días desde MediaStore, miniaturas del sistema, skeleton, estados vacío/sin permiso.
- Fase 2: scrubber temporal (mes/año), selección múltiple (pulsación larga, «elegir día»), favoritos locales + filtro, pellizco para 2–7 columnas, textos en es/en.
- Fase 3: visor (paginador, zoom con pellizco/doble toque, transición compartida, controles que se ocultan, información EXIF y ubicación, compartir, papelera del sistema, favoritos). Vídeo: póster + reproductor del sistema (provisional hasta la fase 4).
- Fase 4: vídeo integrado (Media3 ExoPlayer): reproducir/pausa, progreso, volumen/silencio, doble toque ±10 s, girar a horizontal (modo inmersivo). Pendiente de revisión visual en horizontal.
- Fase 5: álbumes. Manuales (Room v2, migración 1→2): crear/editar nombre y descripción, portada, favoritos, ocultos, añadir desde la selección, quitar, reordenar y orden Manual/fecha/nombre. Automáticos (recientes, cámara, capturas, descargas, fotos, vídeos, favoritas) y carpetas del dispositivo. Pestañas Biblioteca/Álbumes.
- Fase 6: editor no destructivo (desde el visor, solo imágenes): exposición, contraste, saturación, temperatura, sombras, luces, nitidez, viñeta; recorte centrado por proporción, giro 90° y espejo; deshacer/rehacer, restablecer, comparar con el original (mantener pulsado) y guardar copia en Pictures/Lumina (nunca sobrescribe). Vista previa y exportación comparten el mismo pipeline en CPU (`PixelImage`), exportación hasta 4096 px de lado mayor.
- Fase 7: búsqueda por metadatos (nombre, carpeta, tipo, mes, año, favoritas, capturas; sin acentos, es/en) tras la interfaz `SearchProvider`. No hay reconocimiento de imagen: un futuro proveedor se enchufaría sin tocar la UI.
- Fase 8: ajustes (DataStore): tema, animaciones (y escala de animación del sistema), vibración háptica, columnas por defecto, calidad de miniaturas, orden por defecto de álbumes, autoplay de vídeo, proteger pantalla (FLAG_SECURE), resumen de almacenamiento y vaciar caché de miniaturas.

- Fase 9: APK release (R8 + recursos reducidos) firmado con la clave de depuración, mediciones de dominio y de lista grande, corrección de errores de lint (opt-in de Media3, configuración en composables).

Pruebas: `./gradlew :app:testDebugUnitTest` y, con emulador, `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest` (fijar el serial: si hay un móvil conectado, Gradle intenta instalar también ahí).

## Probar
```
./gradlew :app:testDebugUnitTest :app:assembleDebug
emulator -avd Pixel_API36 -read-only -port 5556 -no-window &   # propio, para no pisar otras sesiones
python3 tools/seed-media.py emulator-5556 120   # siembra fotos/vídeos sintéticos (solo emulador)
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
```

## Verificación (2026-11-13)

Verificado:
- `testDebugUnitTest`: 78 tests JVM en verde (dominio, líneas de tiempo, álbumes automáticos/manuales, búsqueda, **píxeles del pipeline del editor**, deshacer/rehacer, DataStore sobre archivo real, resumen de almacenamiento, rendimiento de dominio).
- `connectedDebugAndroidTest` en el emulador compartido `emulator-5554` (Pixel_API36): 31 tests instrumentados en verde: Room (favoritos, álbumes, **migración 1→2 contra `schemas/2.json`**), pantallas de galería/visor/vídeo previas, pantallas de álbumes/búsqueda/ajustes, y **guardado real de una copia editada en MediaStore** (`Pictures/Lumina`, dimensiones tras girar, original intacto).
- `assembleDebug`, `assembleRelease` (R8) y `lintDebug` sin errores; `apksigner verify` OK (firma «Android Debug»).
- Mediciones (no son frames en un móvil):
  - JVM, 50 000 elementos: línea de tiempo 7 ms, resumen de álbumes 12 ms, búsqueda 25 ms; editor completo sobre 12 MP (4000×3000, todos los ajustes + giro) ≈ 1,0 s en un hilo.
  - Emulador, 12 000 elementos: construir la línea de tiempo 69 ms; tres saltos de scroll de punta a punta (12 251 filas) 581 ms; heap 16 → 23 MB. Las URIs eran falsas, así que **no incluye decodificación de miniaturas**.

NO verificado (déjalo para una pasada con dispositivo):
- Instalación y uso manual en el emulador/móvil: no se hicieron capturas de pantalla ni se recorrió la interfaz a mano (`adb install`/`pm grant` fueron denegados en esta sesión). Por tanto **no se ha visto** cómo queda el aspecto visual de álbumes, editor, búsqueda ni ajustes, ni el flujo completo dentro de `LuminaHost` (pestañas, diálogos, visor desde un álbum o desde la búsqueda).
- El **APK release no se ha ejecutado**: R8 reduce el código y podría romper algo por reflexión (Room, DataStore, Coil, Media3). Hay que lanzarlo una vez.
- Rendimiento real con 10 000+ fotos reales (miniaturas, memoria, frames, jank) y en el móvil físico: no medido.
- Editor: vista previa a 1280 px en CPU (sin `RenderEffect`/GPU); el recorte es centrado por proporción (sin arrastrar esquinas); no conserva EXIF en la copia; exportación limitada a 4096 px de lado mayor (`largeHeap`). Fotos de >12 MP o con poca RAM no probadas.
- «Proteger pantalla» (FLAG_SECURE), escala de animación del sistema y alta/baja calidad de miniaturas: implementados, sin comprobar a ojo.
- Álbumes ocultos: solo se ocultan de la lista de álbumes (las fotos siguen en la biblioteca); no es una caja fuerte.
- Favoritos y álbumes son locales a Lumina (Room): no se sincronizan con otras apps ni sobreviven a borrar los datos de la app.
- APK release firmado con clave de depuración: sirve para probar, no para publicar.
