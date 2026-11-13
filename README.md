# Lumina

Galería multimedia para Android (Kotlin, Compose, MVVM). Diseño y fases: [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

## Estado
Fase 1 hecha: cuadrícula cronológica por días desde MediaStore, miniaturas del sistema, skeleton, estados vacío/sin permiso.

## Probar
```
./gradlew :app:testDebugUnitTest :app:assembleDebug
emulator -avd Pixel_API36 -no-window &
python3 tools/seed-media.py emulator-5554 120   # siembra fotos/vídeos sintéticos (solo emulador)
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```
