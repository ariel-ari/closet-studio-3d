# ClosetStudio — análisis y contexto temporal

Este archivo conserva el mapa del proyecto y el punto de continuación mientras Engram no permite guardar desde la sesión actual. Es un **primer corte**, no una auditoría completa ni autorización para cambiar código.

**Actualizado:** 2 de noviembre de 2026  
**Workspace:** `D:\Proyectos2026\App_3D`  
**Estado:** análisis inicial de arquitectura y persistencia; el siguiente paso es profundizar en flujos UI y restricciones geométricas.

## Resumen ejecutivo

ClosetStudio es una aplicación Android nativa en Kotlin/Jetpack Compose para diseñar armarios en una habitación: define dimensiones y ángulos del recinto, inserta y edita componentes, representa el resultado en 3D y guarda proyectos JSON locales. El dominio geométrico está separado en `:core`, pero gran parte de la orquestación de interfaz y estado reside en un `RoomScreen.kt` de casi 7.000 líneas.

## Estructura y flujo principal

| Área | Ubicación | Responsabilidad observada |
|---|---|---|
| Entrada Android | `app/src/main/java/com/closetstudio/app/MainActivity.kt` | Configura el tema Compose y monta `RoomScreen()`. |
| Orquestación UI | `app/src/main/java/com/closetstudio/app/ui/RoomScreen.kt` | Estado de proyecto/habitación, modos de vista, paneles, selección, edición e interacción. Incluye `RoomViewport` (línea ~4185). |
| Plano CAD 2D | `app/src/main/java/com/closetstudio/app/ui/RoomFloorplan2D.kt` | Dibuja/interpreta interacciones del plano y traduce taps a `FloorplanElement`; la selección se entrega a callbacks. |
| Escena 3D | `app/src/main/java/com/closetstudio/app/ui/RoomScene.kt` | Construye las piezas geométricas y coordina SceneView/Filament, cámara, vista y recursos visuales. |
| Adaptador de mallas | `app/src/main/java/com/closetstudio/app/render/MeshGeometry.kt`, `MeshPartNode.kt` | Convierte `MeshData` a geometría Filament y gestiona ciclo de vida de nodos. La conversión de milímetros a metros está concentrada en `MeshGeometry`. |
| Dominio y restricciones | `core/src/main/kotlin/com/closetstudio/core/model/Project.kt` | Modelos `RoomSpec`, puerta, contenedores y componentes; medidas en mm, ángulos, locks y métodos de adaptación/solver. |
| Geometría procedural | `core/src/main/kotlin/com/closetstudio/core/geometry/` | `RoomGeometryBuilder`, `MeshBuilder`, `MeshData`, vectores y superficies sin depender de Android. |
| Persistencia | `app/src/main/java/com/closetstudio/app/data/ProjectStorage.kt`, `core/src/main/kotlin/com/closetstudio/core/model/ProjectCodec.kt` | JSON local de proyectos y preferencias de idioma/unidades. |
| Catálogo de materiales | `app/src/main/java/com/closetstudio/app/material/MaterialCatalog.kt` | Enumera texturas por categoría desde assets. |

**Call path 3D:** `MainActivity → RoomScreen → RoomViewport → RoomScene → RoomGeometryBuilder.build → MeshData → MeshData.toFilamentGeometry → MeshNode`.

## Módulos y herramientas

- Gradle incluye `:app` y `:core`; el primero es Android (min SDK 26, compile/target SDK 36) y el segundo Kotlin/JVM puro (toolchain 21).
- Dependencias principales observadas: Jetpack Compose/Material 3, SceneView 4.63.0 y kotlinx.serialization JSON. Catálogo: AGP 9.5.0, Kotlin 2.4.21.
- CodeGraph está inicializado en `.codegraph/` y sincronizado. Último estado observado: **33 archivos, 943 nodos, 2.997 relaciones; índice al día**. La base local se excluye con `.gitignore` porque el inicializador no pudo escribir en `.git/info/exclude`.
- Las consultas de lectura de Engram funcionan, pero el hook de Codex bloquea las escrituras con `Codex host session is not registered`. Por pedido del usuario, este Markdown funciona como memoria temporal; todavía no se sincroniza con Engram.

## Portabilidad a iOS y macOS

**Conclusión:** el proyecto actual no se compila directamente para iOS ni macOS. Sí hay una base reutilizable y una ruta de migración, pero requiere convertir módulos y reemplazar las fronteras Android/render.

- `:app` usa el plugin Android application y `MainActivity : ComponentActivity`; `ProjectStorage` depende de `Context.filesDir`. No hay targets iOS/macOS ni source sets KMP en la configuración actual.
- `:core` usa el plugin Kotlin/JVM y es el mejor candidato para compartir el dominio, geometría y codec después de auditar APIs JVM (`java.*`) y moverlo a `commonMain` de Kotlin Multiplatform.
- `RoomScene` y `MeshPartNode` usan APIs Android/Filament de bajo nivel. La dependencia actual `io.github.sceneview:sceneview` es la integración Android; la ruta Compose Multiplatform de SceneView es un artefacto separado (`sceneview-compose`), está en alpha y es un subconjunto de viewer, no un reemplazo transparente del generador de mallas actual.
- Apple necesitaría un renderer/UI propio o adaptado (SceneView usa RealityKit/SwiftUI en Apple); el plano CAD y el editor 3D interactivo requerirían validar equivalencia funcional, no solo compilar.
- Para una app iOS final y el simulador se necesita macOS con Xcode. Un target macOS Desktop sería otro target/plataforma independiente, no una salida automática del APK.

**Rutas posibles (sin decisión tomada):** compartir `:core` KMP y crear UI/render SwiftUI+RealityKit para máxima integración nativa; o migrar UI compartible a Compose Multiplatform y adaptar el renderer al viewer multiplataforma alpha para reutilizar más interfaz. Ambas implican trabajo; la primera duplica más UI y la segunda depende de una superficie 3D con menor madurez.

## Hallazgos confirmados y puntos a validar

1. **Estado UI concentrado:** `RoomScreen.kt` tiene 6.976 líneas. El estado de habitación/proyecto se crea dentro del composable con `remember`; la inspección puntual no encontró `rememberSaveable` ni `viewModel()` en ese archivo. Revisar comportamiento ante recreación de actividad y cómo se recuperan cambios no guardados.
2. **Guardado explícito:** las llamadas visibles a `saveProject` están en acciones de guardar/guardar como/crear proyecto. No se observó autosave en esa búsqueda. `ProjectStorage.persistAll()` hace `writeText()` dentro de `runCatching` y no propaga errores; por eso una falla de escritura podría quedar oculta y la UI podría seguir el camino de éxito. Confirmar si ese comportamiento es intencional antes de proponer un cambio.
3. **Codec tolerante:** `ProjectCodec` usa `ignoreUnknownKeys = true` y `encodeDefaults = true`; hay pruebas de round-trip y de campos desconocidos. No se identificó aún una estrategia de migración/versionado del JSON.
4. **Dominio geométrico testeable:** `RoomSpec` deriva vértices a partir de longitudes y ángulos; el solver `solvePolygonForTargetSouth` respeta locks y adapta dimensiones/ángulos. Hay pruebas específicas de invariantes de esquinas, paredes y puerta.
5. **Cobertura desigual:** `:core:test` pasó con **54 pruebas, 0 fallos, 0 errores y 0 omitidas**. Cubren geometría, mallas, componentes/particiones y codec. CodeGraph no encontró pruebas cercanas para `RoomScreen` o `RoomFloorplan2D`; falta verificar si existe cobertura UI fuera de ese mapa.
6. **Referencia APK separada:** ya existe `ANALISIS-APK.md` para `Closet+Planner+3D_2.19.1_APKPure.apk`, además de herramientas y salidas de decompilación bajo `tools/` y `work/`. Este corte no revalidó ese análisis binario ni estableció equivalencia entre ese APK y el código actual.

## Flujo de edición paramétrica del recinto

- `RoomScreen` inicia con una selección de plano (`FloorplanElement.WallA`) y mantiene el `RoomSpec`; la hoja de dimensiones combina el plano interactivo y filas de entrada numérica.
- `FloorplanElement` identifica paredes, puerta sur, cuatro esquinas y altura. `RoomFloorplan2D` calcula objetivos de hit-test a partir de la geometría vigente y emite la selección al callback.
- La fila numérica directa normaliza coma decimal a punto; si un número no se puede convertir, restaura el texto anterior. `AppUnitSystem` ofrece mm, cm, m, pulgadas y pies/pulgadas.
- `RoomSpec` valida dimensiones no negativas y calcula sus vértices `NW/NE/SW/SE`; la pared sur es la distancia geométrica entre `SW` y `SE`. Los métodos de actualización aplican los locks y adaptan segmentos de la puerta.
- `RoomSpecTest` cubre compensación entre ángulos/paredes, invariancia de locks, tamaños de puerta y el mínimo geométrico al reducir la pared sur. Las filas y gestos Compose no aparecen cubiertos por esas pruebas de core.

**Implicación para cambios futuros:** las restricciones geométricas pertenecen al modelo/core y deben seguir probándose allí; no duplicar cálculo de locks dentro de la UI. Los nombres de paredes/esquinas tienen correspondencias específicas UI↔modelo: verificarlas antes de modificar etiquetas o fórmulas.

## Armarios y componentes interiores

- `ClosetContainerSpec` vive dentro de `RoomSpec` y se adosa a una de las cuatro paredes. `RoomGeometryBuilder.addContainerToBuilders()` toma los extremos de la pared, deriva el eje y la normal interior, y construye carcasa/paneles y componentes en ese marco local.
- La generación 3D considera espesor de panel, zócalo, fondo y selección. En el builder observado, la profundidad usada para generar la carcasa tiene un mínimo de 350 mm, el fondo se limita a 8–18 mm y el armario se separa 4 mm de la pared para evitar Z-fighting. Son reglas del renderer que deben compararse con el modelo y la UI antes de cambiar dimensiones.
- El core deriva `ContainerBay` y ajusta componentes a celdas creadas por divisores/estantes. Las reglas del producto exigen cero penetración, anclaje posterior, adaptación al mover particiones y snapping magnético (umbral documentado de 28 mm).
- El picking 3D usa rayos calculados desde la pantalla y funciones del dominio para seleccionar armarios/particiones. La proyección de un toque al interior del armario también calcula coordenadas locales en `RoomScreen.kt`; mantener alineadas estas transformaciones con la geometría core es un punto importante de verificación.
- `ClosetPartitionTest` cubre estantes, divisores, cajones, tubos, zapateras, puertas, raycast, snapping, transferencia entre celdas y cero penetración. Estos tests son la red principal contra regresiones de comportamiento de carpintería.

### Comportamiento acordado: mover cuerpo del closet

- El closet debe poder arrastrarse a lo largo de su pared anfitriona, manteniendo apoyo contra pared y piso y respetando la geometría/límites del recinto.
- El movimiento del cuerpo debe trasladar en conjunto sus componentes interiores, conservando sus posiciones relativas al contenedor.
- Si el closet está unido por esquina a otro, iniciar su movimiento debe separarlo primero; el vecino permanece fijo.
- Separar solo elimina la relación de unión: conserva las dimensiones actuales, incluyendo cualquier ancho que haya aumentado al fusionar. No debe encoger ni restaurar el ancho previo.
- Si el ancho retenido provoca proximidad o solapamiento con el vecino, el arrastre debe respetar el espacio geométrico disponible sin atravesarlo ni modificar el tamaño. La política exacta de bloqueo/límite debe reflejarse en el dominio y probarse.
- Evidencia actual: `RoomScreen.kt` enruta el arrastre de un dedo a componentes interiores detectados; el resto mueve la cámara. `RoomSpec.clampContainer` acota a pared/techo/profundidad, pero no evita por sí solo la colisión entre contenedores de una misma pared. `withCornerFusion(false)` quita flags de fusión, no restaura explícitamente el ancho anterior.
- Implementación iniciada en `codex/persistence-errors`: se agregó `RoomSpec.withMovedContainer()` con desacople de esquinas, ancho/objetos interiores intactos y límite frente a closets de la misma pared; `RoomScreen` enruta el drag seleccionado al movimiento del cuerpo.
- Pruebas verificadas con JDK de Android Studio (`C:\Program Files\Android\Android Studio\jbr`, Java 25), Gradle 9.8.2 del proyecto y `ANDROID_USER_HOME` temporal: `:core:test` y `:app:testDebugUnitTest` dirigidas a `ClosetContainerMovementTest` (3 casos) y `ProjectStorageTest` (1 caso) exitosas. `git diff --check` pasó.
- La revisión RDD queda pendiente: `gentle-ai review assess` reportó riesgo `high` por inventario de archivos no rastreados y el STATUS canónico falló de forma segura con `Access is denied` antes de iniciar mutación.

**Lectura arquitectónica:** el modelo de colocación y restricciones pertenece a `:core`; la pantalla sigue alojando selección, rayos, gestos y proyección. Una futura separación debería mover lógica reutilizable al dominio solo cuando pueda conservar las mismas unidades e invariantes.

## Reglas del producto ya documentadas

`ARCHITECTURE_RULES.md` establece invariantes de interacción/arquitectura: selección 3D con menú contextual explícito (no abrir inspector automáticamente), compartimentos no solapados, piezas acotadas y con anclaje posterior, regla de cero penetración, inserción en vivo y alineación magnética. Hay que leer la regla pertinente antes de tocar componentes o gestos.

- Decisión de interacción: el menú lateral izquierdo no debe abrirse/deslizarse por gesto de borde; se abre mediante el botón de menú. El panel de unidades del lado derecho es independiente.
- El menú izquierdo debe seguir cerrándose al tocar fuera. Material 3 liga ese cierre del scrim a `gesturesEnabled`; al desactivar el swipe, implementar explícitamente la acción del área exterior. Se añadió una capa de toque transparente sobre esa área que cierra el `DrawerState`, manteniendo el swipe desactivado.
- Barra superior adaptable: en anchos menores a 600 dp se conservan los controles principales y las acciones secundarias (etiquetas de paredes, puertas, dimensiones e información) pasan al menú de desbordamiento; en pantallas más amplias permanecen visibles.
- Modo de gesto 3D: el botón de candado bloquea solo el movimiento del cuerpo del closet. Los arrastres de elementos internos siguen teniendo prioridad; al bloquear, el cuerpo y el espacio vacío orbitan la cámara. Al desbloquear, se conserva el orden de particiones → closet → cámara. Los toques y gestos de dos dedos permanecen separados.
- Catálogo “Agregar Unidad”: mejorar solo miniaturas, sin agregar nombres, descripciones ni instrucciones. Las nueve previsualizaciones usan un marco visual y tamaño consistentes, conservando tap y arrastre.

## Verificación y entorno

- Comando exitoso: `:core:test --no-daemon`, con `JAVA_HOME` apuntando a `tools/jdk/jdk-21.0.13+9` y `GRADLE_USER_HOME` temporal fuera del repo.
- El primer intento falló porque no había JDK configurado; el siguiente, porque el Gradle cache por defecto apuntaba a `C:\.gradle`; con JDK local, caché temporal y permiso de red, Gradle 9.6.0 se descargó y las pruebas pasaron.
- No se verificó aún `:app:assembleDebug`, ejecución en emulador, accesibilidad ni flujos UI completos.
- Para compilar la UI adaptable: `:app:compileDebugKotlin --no-daemon` pasó. `:app:assembleDebug --no-daemon` pasó al ubicar `ANDROID_USER_HOME` en `.gradle/android-home`; el primer intento falló por acceso denegado al `debug.keystore.lock` bajo `%TEMP%`.
- `:app:testDebugUnitTest --tests com.closetstudio.app.ui.RoomGestureModeTest --no-daemon` pasó para los cuatro casos del modo de arrastre, y `:app:testDebugUnitTest --no-daemon` pasó. El test RED inicial reprodujo el bloqueo de particiones; la corrección pasó ambos checks.
- APK reconstruido tras el ajuste: `app/build/outputs/apk/debug/app-debug.apk`, con `:app:assembleDebug --no-daemon` exitoso.
- Miniaturas: `:app:testDebugUnitTest :app:assembleDebug --no-daemon` pasó; revisión independiente estructural confirmó los nueve marcos de imagen y las nueve rutas de toque/arrastre. Falta revisar el render en dispositivo/emulador.
- La revisión nativa RDD quedó sin iniciar porque `gentle-ai review status`/`assess` recibió `Access is denied` al acceder al índice temporal dentro de `.git`; no se modificó autoridad ni se generó recibo. Las verificaciones visuales en dispositivo/emulador siguen pendientes.
- Verificador independiente del bloqueo: revisión estructural aprobada; su probe de Gradle en scratch quedó parcial por resolución de Gradle/Foojay. El test enfocado y la suite, ejecutados en el workspace por el agente principal, sí pasaron.

## Historial previo para continuidad

El contexto de Engram recuperado antes del bloqueo reportaba como trabajo reciente: botón flotante `+` y panel lateral de unidades/componentes; selector ES/EN; gestión de proyectos; y varias correcciones de locks/invariantes geométricas. Tratar estos datos como contexto histórico y contrastarlos con el código antes de basar cambios en ellos.

## Próximo tramo del análisis

1. Documentar los flujos de edición del plano 2D, dimensiones y selección, con sus transiciones de estado.
2. Seguir inserción/drag/edición de contenedores y particiones hasta sus invariantes del core y pruebas.
3. Revisar ciclo de vida de materiales/recursos Filament y comportamiento de cambios de proyecto.
4. Priorizar, con evidencia, riesgos y mejoras; no implementar hasta que el usuario elija el alcance.

## Archivos fuente clave

- `app/src/main/java/com/closetstudio/app/MainActivity.kt` — entrada Compose.
- `app/src/main/java/com/closetstudio/app/ui/RoomScreen.kt` — coordinación UI y estado principal.
- `app/src/main/java/com/closetstudio/app/ui/RoomFloorplan2D.kt` — plano CAD 2D.
- `app/src/main/java/com/closetstudio/app/ui/RoomScene.kt` — presentación de la escena 3D.
- `app/src/main/java/com/closetstudio/app/data/ProjectStorage.kt` — almacenamiento local.
- `core/src/main/kotlin/com/closetstudio/core/model/Project.kt` — modelos y restricciones del dominio.
- `core/src/main/kotlin/com/closetstudio/core/model/ProjectCodec.kt` — serialización de proyectos.
- `core/src/main/kotlin/com/closetstudio/core/geometry/RoomGeometry.kt` — generación procedural de superficies.
