# Closet Sketcher 3D 2.19.1 — Análisis técnico del APK

Artefacto analizado: `Closet+Planner+3D_2.19.1_APKPure.apk` (25.9 MB)
Paquete: `com.closetsketcher` — Nombre comercial: **Closet Sketcher 3D**
versionCode 50 / versionName 2.19.1 · minSdk 24 · targetSdk 36

Herramientas: JDK 21 (Temurin), JADX 1.5.6, apktool 3.0.3 (instaladas en `tools/`).

---

## 1. Qué es

Un **configurador de placares (closets) paramétrico en 3D** para Android. No es un modelador
3D libre ni un juego: es un editor con un motor de render propio, donde el usuario define
medidas del mueble y del ambiente, inserta módulos de ferretería (estantes, cajones, barras,
canastos) y aplica materiales/texturas. La escena es siempre un "closet" dentro de una
habitación, visto en 3D o en planta 2D.

## 2. Stack real (verificado en el binario)

| Capa | Tecnología | Evidencia |
|---|---|---|
| Render 3D | **libGDX** (g3d: ModelBatch, MeshBuilder, PerspectiveCamera, DefaultShader) | `lib/*/libgdx.so`, `com.badlogic.gdx.*` en dex, assets `com/badlogic/gdx/**` |
| Empaquetado del render | `AndroidFragmentApplication` (libGDX embebido como Fragment dentro de una Activity normal) | `MainActivity implements AndroidFragmentApplication.Callbacks`, `KitchenEditorFragment` |
| Lenguaje | Kotlin 2.3.21 + Java (mezcla; el visor está en Java, la capa de datos en Kotlin) | `kotlin-tooling-metadata.json`, `@Metadata` |
| Build | Gradle 9.4.1, Java 17, R8 con minificación agresiva (nombres → `a()`, `j`, `k`) | `kotlin-tooling-metadata.json`, firmas ofuscadas |
| UI | Android Views clásico + **DataBinding** + Fragments + ViewPager + RecyclerView (no Compose) | `databinding/*Binding`, `res/layout/fragment_*.xml` |
| DI | **Dagger 2** (componente manual, sin Hilt) | `AppComponent`, `DaggerAppComponent`, `*_MembersInjector` |
| Persistencia | **Gson** a archivos JSON en el almacenamiento externo; SharedPreferences para ajustes | `ProjectsDataSource`, `Project`, `SerializedName` |
| Monetización | **Google Play Billing 9.0.0** (suscripciones) + **RuStore SDK** (variante rusa) | manifest, `PremiumHelperImpl`, `rustore_id=888631743` |
| Analítica | **Firebase Analytics + Crashlytics** | `firebase-*.properties`, `AnalyticsConnectorRegistrar` |
| Anti-abuso | **FingerprintJS** (device ID) | `com.fingerprintjs.android.*` en `App.onCreate` |
| Otros | RateThisApp, OkHttp, Glide, AppCompat/Material3 | manifest + dex |

**No hay Unity, ni Flutter, ni React Native, ni WebView/Three.js.** El 3D es libGDX puro.

## 3. Arquitectura de capas

```
Activities (Splash, Main, OpenProject, ProjectInfo, Settings)
  └─ Fragments: KitchenEditorFragment (host del GL) + paneles de settings
       ├─ KitchenViewer (libGDX ApplicationAdapter)  ← el motor
       │    ├─ Room / Closet / Modules / Doors / Selection / Sizes  (RenderableProviders)
       │    ├─ CameraController2D | CameraController3D
       │    └─ MeshBuilder + KitchenGeometries  (geometría procedural)
       └─ AppState (singleton global mutable) + Actions (enum de eventos)
Modelo: Project (medidas, colores, lista de Module, Door[]) ↔ Gson ↔ archivo .json
Persistencia: ProjectsDataSource (projects/, previews/), HistoryHelper (undo = snapshots JSON)
Premium: PremiumHelperImpl (BillingClient) + PremiumState + flags "pro" en assets
```

`AppState.q` es un singleton con todo el estado de la app; los cambios emiten `Actions`
(vía `StateChangedListener`) y cada renderable decide si reconstruirse (`update = true`).
Es un patrón **"estado global + invalidación por flags"**, no reactivo ni ECS.

## 4. El motor 3D (lo más importante)

### 4.1 Geometría: TODO es procedural, no hay modelos

En el APK **no existe un solo archivo de malla** (`.obj/.g3dj/.g3db/.glb`). Las 716 imágenes
de `assets/img/` son solo texturas. Es decir:

- El mueble se construye con `MeshPartBuilder.box()` y `ModelBuilder` (`Closet.c()`, `Room`).
- Los paneles tienen esquinas redondeadas generadas a mano: `RoundedBoxKt` subdivide cada cara
  en 9 tiles con radio 7 mm (`Indexer`, `Face`, `Vertex`), usado en `ShelfModule.paint()`.
- Dimensiones en **milímetros** como unidad de mundo (closet 2500×2000×650 por defecto).
- Cada pieza es un `ModelInstance` propio; se reconstruye entera cuando cambia (`update`).

Esto es clave: **el "3D" es un CAD ligero de cajas paramétricas**, no un sistema de assets.

### 4.2 Shader

`viewer.vertex.glsl` / `viewer.fragment.glsl` = **DefaultShader de libGDX** con una sola
modificación: se agregó `u_diffuseUVScale` para **rotar la UV del material** (así giran la
veta de la madera en cada panel). Es Blinn-Phong (difusa + especular + emisiva + luz
ambiental + N luces direccionales), con soporte de shadow map en el shader pero sin uso real
en la práctica. `MainShader` solo registra ese uniform extra.

### 4.3 Iluminación y cámara

- `Environment` con ambient gris 0.8 + **una** luz direccional (0.3) que sigue a la cámara.
- Dos cámaras: `PerspectiveCamera` (modo 3D) y `OrthographicCamera` (modo **planta 2D**),
  conmutables en runtime (`KitchenViewer.m()`).
- Un `InputMultiplexer` combina el input del viewer con el del controlador de cámara
  (orbit/pan/zoom propios, no `CameraInputController`).

### 4.4 Picking y drag & drop

- `camera.getPickRay()` + intersección con **planos** (`KitchenViewer.d()`) para obtener el
  punto 3D bajo el dedo (plano frontal y plano de fondo del closet).
- `Modules.e()` hace ray vs AABB por módulo y elige el más cercano; `DragContainer` maneja
  el estado de arrastre.
- **Snapping** a 25 mm: en `Modules.d()` se buscan los topes (maxY/minY) del resto de los
  módulos y, si el delta es < 25, se ajusta. También `Math.round()` para mantener enteros.

### 4.5 El "solucionador de layout" (el corazón del producto)

`Modules.g()` recalcula, para cada módulo, sus límites libres `top/bottom/left/right`
comparando **contra todos los demás** (`Modules.h()` resuelve pares). Después encaja el
módulo dentro de esos límites y le fija tamaño:

- módulo `axis="x"` (horizontal, ej. estante) → ocupa todo el ancho libre, `w = right - left`
- módulo `axis="y"` (vertical, ej. barra/columna) → ocupa todo el alto libre, `h = top - bottom`

Es un solver 2D de restricciones AABB, no un motor físico. Eso explica el comportamiento
"el estante siempre toca las paredes y se apoya en lo que tiene al lado".

### 4.6 Acotado y previews

- `Sizes` (827 líneas) dibuja **cotas con medidas** dentro de la escena 3D (líneas + texto
  generado con FreeType, solo glifos `0-9 - . , '` ), con tamaño configurable por preferencia.
- `ScreenshotFactoryKt` + `ScreenshotFactoryKt$makeScreenshot$1`: `glReadPixels` → flip →
  PNG, vía corrutina, para la miniatura del proyecto (`previews/<nombre>.png`).
- `PreviewViewer`: un `ApplicationAdapter` separado que renderiza la miniatura.

## 5. Sistema de módulos (extensible por ID)

`Module` (283 líneas) es la clase base de todas las piezas de ferretería. IDs fijos:

| moduleId | Clase | Descripción (assets `furniture.json`) |
|---|---|---|
| 1 | `DrawerModule` | cajón con tirador, h=200 |
| 2 | `DrawerNoHandleModule` | cajón sin tirador |
| 10 | `Old_RodModule` | barra legacy (compatibilidad) |
| 11 | `RodModule` | barra colgadora (x/y) |
| 21 | `PantographModule` | pantógrafo (h=650) |
| 51 | `ShelfModule` | estante (x/y), esquinas redondeadas o cajón de sección lateral |
| 151 | `BasketModule` | canasto |
| 201/202 | `ShoeShelfModule` / `...MetalModule` | zapatera |
| 301 | `RackModule` | **pro** |

Cada subclase solo sobrescribe `paint()` (construye la malla) y `updateMaterials()`.
`ModulesFactory.a(id)` mapea id → clase; `ModuleTypeAdapter` (Gson) **deserializa polimórfico**
leyendo `moduleId` antes de instanciar. `Module.m0clone()` clona serializando a JSON y
re-parseando con la clase concreta — truco simple y muy práctico.

Flags de monetización: `furniture.json` marca `"pro": true` (barra vertical y rack).

## 6. Modelo de datos y persistencia

`Project` (JSON, claves cortas para ahorrar espacio):

```json
{ "version":"2.0.0", "type":1,
  "cw":2500, "ch":2000, "cl":650,          // closet width/height/depth
  "l":2500, "h":2700,                       // room length/height
  "leftWall":1000, "rightWall":1000,        // paredes visibles
  "niche_left":false, "niche_right":false,  // nichos laterales
  "top":1, "back":1, "left":1, "right":1, "bottom":1,  // 0=sin panel,1=completo,2=parcial(100mm)
  "plinth":100, "thickness":20,
  "l_section":{...}, "r_section":{...},     // SideSection (enable/bottom/top/back/radius)
  "modules":[ {"moduleId":51,"w":800,"h":20,"d":650,"x":-400,"y":1200,"z":0,"axis":"x"} ],
  "doors":[ {"delimiters":[...], "color":{...}, "color_s":{...} } ],
  "wall_c":{...}, "floor_c":{...}, "corp_c":{...}, "back_c":{...}, "frame_c":{...} }
}
```

- Archivos: `getExternalFilesDir(DOWNLOADS)/projects/<nombre>` (JSON) +
  `filesDir/previews/<nombre>.png`. Autosave en `filesDir/projects/com.closetsketcher.autosave`.
- Guardado con `AsyncTask`; lectura con `JsonReader` (streaming).
- **Undo/redo = pila de 10 strings JSON** del `Project` completo (`HistoryHelper`). Sin diff,
  sin comandos. Funciona y es trivial de implementar.

## 7. Materiales y texturas

`ItemColorModel` es el "material" serializable: `color` (ARGB int), `img` (ruta en assets),
`type` (1 color plano, 2 asset, 3 ruta absoluta, 4 recurso Android), `scaleX/Y`, `offsetX/Y`,
`rotation`, `opacity`.

`updateMaterial()` aplica al `Material` de libGDX:
- `AdvTextureAttribute` (TextureAttribute custom con offset/scale/rotación de UV) → alimenta
  el `u_diffuseUVScale` del shader (la rotación de veta).
- `ColorAttribute` (tinte RGBA), `BlendingAttribute` (si opacidad < 1, ej. vidrio/reflejo).
- Texturas cargadas **lazy** por `AssetManager` con `Repeat` + mipmaps.

Catálogos en `assets/data/*.json`: `simple/floor/wall/corp/marble/tiles/mirror-colors.json`.
Son listas de colores ARGB y de rutas a JPG. El "material library" es data, no código.

## 8. UI

- Layouts XML + DataBinding; paneles flotantes (`panel.xml`, `toolbar_for_panels.xml`).
- `KitchenEditorFragment` contiene el GL; los paneles (Closet / Doors / Door / Module /
  Shelves / Room / Texture / Color) son Fragments que escriben sobre `Project` y emiten `Actions`.
- `ColorPickerView` (554 líneas) y `NumericStepperView` (252) son widgets propios.
- Modo 3D / planta 2D / "ver cotas" vía `AppState.n` y `SettingsActivity`.
- i18n: ~40 idiomas en `res/values-*`.

## 9. Qué reutilizar si vas a hacer tu propia app

**Copiable como idea (no como código):**
1. Geometría procedural con cajas paramétricas + esquinas redondeadas. Es lo que hace que la
   app pese 26 MB y sea coherente a cualquier medida.
2. Separar `Project` (JSON plano, claves cortas) del render, y reconstruir la malla por pieza
   con un flag `update`.
3. El solver de límites `top/bottom/left/right` + snapping de 25 mm: ~150 líneas que dan toda
   la sensación de "mueble real".
4. Undo por snapshots JSON (10 niveles): 40 líneas.
5. Catálogo de materiales como JSON (color ARGB + textura + UV transform), no hardcodeado.
6. Módulos como clases con `id` + factory + Gson polimórfico: agregar una pieza nueva = 1 clase
   + 1 id + 1 entrada en el catálogo.

**Mejorable (y ahí está tu ventaja competitiva):**
- `AppState` global mutable + flags: reemplazable por estado inmutable + Compose/StateFlow.
- Sin sombras reales, sin AO, sin PBR: el shader es Blinn-Phong plano. Un motor moderno
  (Filament, SceneView, o Three.js/WebGL) da un salto visual enorme.
- `Modules.g()` es O(n²) y recalcula todo: con muchos módulos conviene un índice espacial.
- Sin exportación (DXF/OBJ/PDF de planos), sin precios/presupuesto de materiales, sin
  colaboración, sin AR. Todas features "obvias" que la app no tiene.
- Guardado con `AsyncTask` (API deprecada) y sin backup/migración de versiones del JSON
  (el campo `version` existe pero no hay migrador visible).

## 10. Alternativas de stack para la app nueva

| Opción | Pros | Contras |
|---|---|---|
| Android nativo + **Filament** o SceneView (Kotlin) | Calidad visual PBR/IBL, rápido en gama baja, control total | Más trabajo de geometría procedural (Filament no trae helpers tipo `box()`) |
| **Flutter + three_dart / FlutterGL** | Multiplataforma, UI productiva | 3D en Flutter sigue verde; performance irregular |
| **Web (Three.js / React Three Fiber) + Capacitor** | Desarrollo rapidísimo, PBR, editor web y app a la vez, AR con WebXR | Requiere WebGL2; APK más pesado y arranque más lento |
| **Unity** | Ecosistema 3D, AR Foundation | Overkill para un CAD de cajas; APK base ~25 MB solo de runtime; licencia |

Para un configurador paramétrico, la opción con mejor relación esfuerzo/calidad es
**Three.js (web) envuelto en Capacitor** si querés velocidad, o **Kotlin + Filament** si
querés una app nativa premium. libGDX solo tiene sentido si querés reusar la lógica existente
y ya sabés Java.

## 11. Límite legal (importante)

- Analizar el APK para entender arquitectura y aprender: legítimo.
- **No** reutilizar sus 716 texturas JPG, sus íconos, ni su nombre/marca ("Closet Sketcher 3D").
  Son obra y marca registradas.
- **No** republicar el APK modificado ni el código decompilado.
- Lo correcto: reimplementar las ideas en código propio, con assets propios (CC0: Poly Haven,
  AmbientCG) o generados.

## 12. Comandos útiles (herramientas ya instaladas)

```powershell
$env:JAVA_HOME='D:\Proyectos2026\App_3D\tools\jdk\jdk-21.0.13+9'
$env:Path="$env:JAVA_HOME\bin;$env:Path"

# Decompilar a Java
.\tools\jadx\bin\jadx.bat -d work\jadx_all --no-res -j 8 "Closet+Planner+3D_2.19.1_APKPure.apk"

# Recursos + manifest + smali reconstruible
java -jar .\tools\apktool\apktool.jar d -f -o work\apktool_full "...apk"

# GUI (buscar/leer clases cómodo)
.\tools\jadx\bin\jadx-gui.bat "...apk"
```

Salidas actuales: `work/jadx_all/sources/com/closetsketcher/` (216 clases),
`work/apktool/` (manifest + res decodificados), `work/assets/` (data + shaders).
