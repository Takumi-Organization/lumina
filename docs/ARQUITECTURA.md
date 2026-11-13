# LUMINA — Análisis y arquitectura

Galería multimedia para Android. Kotlin + Compose + MVVM. Un solo módulo Gradle (`:app`) con paquetes con frontera estricta; se parte en módulos solo si la compilación incremental lo pide (YAGNI).

## 1. Decisiones y restricciones reales de Android

| Tema | Decisión | Motivo |
|---|---|---|
| Descubrir medios | `MediaStore.Images/Video` con `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` (API 33+) y `READ_EXTERNAL_STORAGE` (29–32). **minSdk 30** (scoped storage, `DATE_TAKEN`, `RELATIVE_PATH`, `createTrashRequest`/`createWriteRequest`; en API 29 el borrado exige un flujo por archivo con `RecoverableSecurityException`, que no compensa mantener). Acceso parcial (API 34 `READ_MEDIA_VISUAL_USER_SELECTED`) tratado como estado propio, no como error. | Scoped storage: no se puede recorrer directorios arbitrarios. «Descargas» y «Capturas» se infieren por `RELATIVE_PATH`/`BUCKET_DISPLAY_NAME`, solo de lo que MediaStore expone. |
| Miniaturas | `ContentResolver.loadThumbnail` vía Coil (`MediaStore` fetcher propio) + `coil-video` para fotogramas. Nunca el original en la cuadrícula. | Rendimiento con miles de ítems. |
| Lista grande | `MediaStore` consultado paginado → `Flow<List>` en memoria con claves estables (`MediaStore.Files._ID`), agrupado por fecha en dominio; `LazyVerticalGrid` con claves. | Sin Paging 3 inicialmente: la consulta de solo metadatos de 50 k filas cabe en memoria (~10 MB). Se reevalúa si se mide lo contrario. |
| Borrar / modificar | `MediaStore.createTrashRequest` (papelera del sistema, recuperable ~30 días; diálogo de confirmación del sistema) / `createWriteRequest`. | Único camino permitido a medios de otras apps. |
| Guardar ediciones | Copia nueva en `Pictures/Lumina` vía `MediaStore.insert` (sin permiso en API 29+). Nunca sobrescribe el original. | Cumple «guardar copia» y es seguro. |
| Favoritos | **Solo locales (Room, tabla `favorite`)**. No se escribe `IS_FAVORITE` del sistema. | `MediaStore.createFavoriteRequest` exige un diálogo de confirmación del sistema en cada acción; inviable para marcar en lote con fluidez. Coste: no se sincroniza con los favoritos de otras apps. |
| Álbumes manuales | Solo Room (`album`, `album_item`): referencias `mediaId`, no copias. | No mover/duplicar archivos del usuario. |
| Ubicación EXIF | Requiere `ACCESS_MEDIA_LOCATION` (opcional, se pide solo al abrir «información»). `MediaStore.setRequireOriginal`. | Android redacta el GPS si no. |
| Reconocimiento IA | **No se implementa.** Interfaz `SearchProvider` con implementación de metadatos; un futuro `MlSearchProvider` se enchufa sin tocar la UI. | Pedido explícito: no simular. |
| Vídeo | Media3 ExoPlayer + controles propios en Compose. | API moderna, soporte oficial. |
| Edición | Pipeline no destructivo: `EditState` (parámetros) → render por `ColorMatrix`/`RenderEffect` en vista previa (GPU); render final en `Bitmap` a resolución completa con kernel de nitidez, viñeta y curvas sombras/luces. Pila de estados para undo/redo. | Los parámetros son datos; la imagen original no se toca hasta exportar. |
| DI | Manual (`AppContainer`). | Sin Hilt/Koin: ~10 dependencias, no justifica el coste (regla: sin abstracción sin necesidad). |

## 2. Paquetes

```
com.adrian.lumina
├─ app/            LuminaApp (Application), MainActivity, AppContainer, navegación raíz
├─ domain/
│  ├─ model/       MediaItem, MediaKind, Album, DateGroup, SearchQuery, EditState…
│  ├─ repository/  MediaRepository, AlbumRepository, SettingsRepository (interfaces)
│  └─ usecase/     GroupByDate, BuildTimelineIndex, SmartAlbums, Search (puros, testeables en JVM)
├─ data/
│  ├─ media/       MediaStoreSource (consultas, observer), ThumbnailFetcher, MediaActions (borrar/escribir)
│  ├─ db/          Room: AlbumEntity, AlbumItemEntity, FavoriteEntity, DAOs
│  └─ prefs/       DataStore: tema, columnas, calidad, autoplay, animaciones, orden, privacidad
├─ editor/         EditState, EditPipeline (preview + export), UndoStack
└─ ui/
   ├─ theme/       paleta, tipografía, formas, movimiento
   ├─ components/  Skeleton, EmptyState, ScrubBar…
   ├─ gallery/     GalleryScreen/ViewModel (cuadrícula, selección, scrubber temporal)
   ├─ viewer/      pager, zoom, EXIF, reproductor
   ├─ albums/      lista, detalle, edición
   ├─ editor/      pantalla del editor
   ├─ search/      búsqueda
   └─ settings/    ajustes
```

Reglas de dependencia: `ui → domain ← data`; `domain` no importa Android (solo Kotlin + coroutines) para testearlo en JVM puro.

## 3. Diseño visual (identidad propia)

- **Concepto**: «cámara oscura». Superficie de tinta casi negra tintada de cálido (no gris azulado), la foto como única fuente de luz. Modo claro: papel de revelado off-white.
- **Acento único**: ámbar «lumen» (#E8A33D oscuro / #B86E00 claro), usado solo para estado (selección, favorito, scrubber). Sin degradados decorativos ni cristal; cumple [[feedback-ui-no-generica-ia]].
- **Tipografía**: sans de sistema con pesos ligeros en cabeceras de fecha, tabulares para fechas y tamaños.
- **Cuadrícula**: mosaico con ritmo — filas de 3 columnas con una miniatura destacada 2×2 cada N ítems en vistas «Días»; uniforme en «Meses/Años». Separadores: fecha grande a la izquierda + recuento, línea fina.
- **Movimiento**: duración corta (180–280 ms), curvas `emphasized`; todo respeta el ajuste «Animaciones» (off → instantáneo) y el ajuste del sistema.
- **Scrubber temporal**: carril a la derecha con marcas de año/mes; al arrastrar muestra burbuja «Mar 2021» y salta usando el índice precalculado (offset de fila por mes).

## 4. Fases y criterio de «terminado»

| Fase | Contenido | Verificado cuando |
|---|---|---|
| 0 | Análisis + arquitectura (este doc) | — |
| 1 | Esqueleto, tema, permisos, MediaStore, cuadrícula cronológica con grupos por fecha, skeleton, estados vacío/sin permiso | build + tests de dominio + captura en emulador con medios sembrados |
| 2 | Scrubber temporal, selección múltiple, favoritos, indicadores de vídeo, zoom de cuadrícula (pinch) | tests + UI test de selección |
| 3 | Visor: pager, zoom, transición compartida, EXIF, compartir, borrar, favoritos | UI test + prueba manual en emulador |
| 4 | Vídeo: Media3, controles, gestos, horizontal | prueba en emulador con vídeo sembrado |
| 5 | Álbumes (Room): manuales, portada, orden, descripción, favoritos, ocultos, automáticos | tests DAO instrumentados |
| 6 | Editor no destructivo + undo/redo + comparar + exportar | tests del pipeline (píxeles) + guardar copia real |
| 7 | Búsqueda por metadatos + `SearchProvider` | tests de dominio |
| 8 | Ajustes (DataStore), almacenamiento, privacidad, háptico | tests + captura |
| 9 | Rendimiento (biblioteca de 10 k+ sembrada), pulido, README, APK release firmado de depuración | medición de frames/memoria declarada |

Cada fase termina con: compila, tests en verde, ejecución en emulador `Pixel_API36` y lo que **no** se pudo comprobar dicho explícitamente.

## 5. Riesgos conocidos

- Transición compartida cuadrícula→visor entre pantallas de navegación: `SharedTransitionLayout` de Compose es estable pero delicada con `LazyGrid` + `Pager`; plan B: visor como overlay en el mismo árbol de composición.
- Bibliotecas reales de miles de fotos: solo hay emulador; se siembra con imágenes generadas (`tools/seed-media.sh`). El rendimiento en el móvil físico (25053PC47G) queda como prueba manual de Adrián.
- Edición de «Sombras/Altas luces» sin shader AGSL en API <33: fallback por LUT en CPU para exportación y vista previa reducida.
