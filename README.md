# Lumina

Galería multimedia para Android (Kotlin, Compose, MVVM). Diseño y fases: [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

## Estado
- Fase 1: cuadrícula cronológica por días desde MediaStore, miniaturas del sistema, skeleton, estados vacío/sin permiso.
- Fase 2: scrubber temporal (mes/año), selección múltiple (pulsación larga, «elegir día»), favoritos locales + filtro, pellizco para 2–7 columnas, textos en es/en.
- Fase 3: visor (paginador, zoom con pellizco/doble toque, transición compartida, controles que se ocultan, información EXIF y ubicación, compartir, papelera del sistema, favoritos). Vídeo: póster + reproductor del sistema (provisional hasta la fase 4).
- Fase 4: vídeo integrado (Media3 ExoPlayer): reproducir/pausa, progreso, volumen/silencio, doble toque ±10 s, girar a horizontal (modo inmersivo). Pendiente de revisión visual en horizontal.
- Fase 5: álbumes. Manuales (Room v2, migración 1→2): crear/editar nombre y descripción, portada, favoritos, ocultos, añadir desde la selección, quitar, reordenar y orden Manual/fecha/nombre. Automáticos (recientes, cámara, capturas, descargas, fotos, vídeos, favoritas) y carpetas del dispositivo. Pestañas Biblioteca/Álbumes.
- Fase 6: editor no destructivo (desde el visor, solo imágenes): exposición, contraste, saturación, temperatura, sombras, luces, nitidez, viñeta; recorte libre con agarres (o por proporción), giro 90° y espejo; deshacer/rehacer, restablecer, comparar con el original (mantener pulsado) y guardar copia en Pictures/Lumina con el EXIF del original (nunca sobrescribe). Vista previa y exportación comparten el mismo pipeline en CPU (`PixelImage`), exportación hasta 4096 px de lado mayor.
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

## Verificación (2026-11-14)

Todo lo siguiente se ejecutó en el emulador compartido `emulator-5554` (Pixel_API36, Android 16) o en JVM; no hay prueba en móvil físico.

Tests automáticos
- `testDebugUnitTest`: 89 tests JVM (dominio, líneas de tiempo, álbumes y `AlbumsViewModel` con repositorios falsos —crear, añadir, reordenar, portada, quitar, ocultar, eliminar—, búsqueda, píxeles del editor, geometría del recorte libre, deshacer/rehacer, DataStore sobre archivo real, almacenamiento, rendimiento de dominio).
- `connectedDebugAndroidTest`: 34 tests instrumentados en verde: Room y migración 1→2 contra `schemas/2.json`, pantallas de galería/visor/vídeo/álbumes/búsqueda/ajustes, tema (animaciones/háptico/calidad de miniatura), editor completo con imagen real (recorte arrastrando una esquina, deshacer), guardado real de copia en `Pictures/Lumina` con dimensiones correctas, original intacto y **EXIF conservado** (marca, modelo, fecha; orientación normalizada).
- `assembleDebug`, `assembleRelease` (R8) y `lintDebug` sin errores; `apksigner verify` OK.

Recorrido real en pantalla (capturas con `screencap`, interacción con `input`)
- Biblioteca, Álbumes, Búsqueda y Ajustes en tema claro y oscuro; permisos; crear un álbum, añadir fotos desde la selección, abrir su detalle y seleccionar.
- Editor: sliders (exposición, saturación, temperatura), pestaña de recorte, giro, espejo, deshacer, rehacer, mantener pulsado para comparar con el original y guardar copia (archivo creado en `Pictures/Lumina`).
- Vídeo (fase 4): reproducción hasta «Repetir» y giro a horizontal con modo inmersivo; el vídeo ocupa la pantalla.
- **APK release (R8) ejecutado**: arranca, lista 10 128 elementos reales con miniaturas; Room, DataStore, búsqueda, ajustes y reproducción de vídeo funcionan sin excepciones ni reglas de keep adicionales.
- «Proteger pantalla»: con el ajuste activo `screencap` devuelve negro (FLAG_SECURE efectivo); desactivado, normal.

Defectos encontrados y corregidos al verlo en pantalla
- Texto negro sobre fondo oscuro (faltaba `Surface` en el tema) y esquema de color M3 incompleto (barra de navegación y chips lavanda).
- La cabecera de «Mis álbumes» quedaba bajo la barra de título (no contaba el inset de estado).
- El arrastre del recorte aplicaba solo el último delta (estado obsoleto entre eventos): ahora acumula.
- Salir del visor tras «girar» un vídeo dejaba toda la app en horizontal: se restablece la orientación al cerrar.
- Los interruptores de ajustes solo respondían al pulsar el propio interruptor (ahora toda la fila) y apagados no se veían.

Rendimiento (emulador, APK release, **10 128 imágenes reales** de 480×360 sembradas con `tools/seed-perf.py`, miniaturas incluidas)
- 25 deslizamientos rápidos de punta a punta en la biblioteca, `dumpsys gfxinfo`: 1,71 % de frames lentos (15 de ≈ 880); percentiles de frame 50/90/95/99 = 8/16/16/18 ms (límite de 16,6 ms a 60 Hz); GPU 7/15/16/16 ms; 0 vsync perdidos. Memoria: Java heap ≈ 20 MB, Native ≈ 160 MB.
- Dominio en JVM con 50 000 elementos: línea de tiempo 7 ms, resumen de álbumes 12 ms, búsqueda 25 ms; editor completo sobre 12 MP ≈ 1,0 s.
- Son cifras del emulador (GPU/CPU emuladas): sirven para comparar versiones, no para prometer fluidez en un móvil concreto.

Limitaciones que siguen abiertas (lo único)
- **Móvil físico**: no probado (ni rendimiento, ni cámara real, ni sincronización con otras apps). Es la única verificación pendiente de Adrián.
- No se recorrieron a mano en pantalla: portada, reordenar y ocultar álbum (verificados por `AlbumsViewModelTest` y los tests de Room/pantalla, no con el dedo); la escala de animación del sistema y la «calidad» de miniaturas (solo comprobado que el tema las expone a la UI).
- Editor: vista previa en CPU a 1280 px; exportación hasta 4096 px de lado mayor (`largeHeap`); no se conserva EXIF en copias PNG.
- Álbumes ocultos: solo se ocultan de la lista (las fotos siguen en la biblioteca); favoritos y álbumes son locales a Lumina (Room) y no se sincronizan con otras apps.
- APK release firmado con la clave de depuración: sirve para probar, no para publicar.
