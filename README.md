# ClosetStudio 3D

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%2026%2B-green.svg)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![3D Engine](https://img.shields.io/badge/Render-SceneView%20%2F%20Google%20Filament-orange.svg)](https://github.com/SceneView/sceneview-android)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20Modular-teal.svg)](#architecture)

**ClosetStudio 3D** is a professional, native Android parametric 3D room and custom closet (wardrobe) configurator. It enables architects, interior designers, and carpentry professionals to define room geometry, place modular closet carcasses against architectural walls, customize internal partitions in real time, and visualize projects with photorealistic PBR rendering and interactive 2D CAD floor plans.

---

## Key Features

- **Interactive 2D CAD Floor Plan**:
  - Parametric wall sizing, corner angles ($90^\circ$ and non-orthogonal acute/obtuse angles), and real-time wall locking constraints (*candados*).
  - South wall parametric split-door support with segment-preserving compensation.
- **Real-Time 3D Viewport (Google Filament PBR)**:
  - Physically Based Rendering (PBR) with delicate architectural ambient occlusion ($18\text{ cm}$ radius), contact shadows, FXAA, and MSAA.
  - Panoramic first-person and free-orbit camera controllers with seamless transitions between Top-Down 2D, Front Elevation, and 3D Perspective.
- **Parametric Modular Closets & Carpentry Engine**:
  - Drag-and-drop placement of modular carcasses along host walls with automatic collision avoidance and wall edge clamping.
  - Intelligent corner fusion across adjacent walls with independent depths.
  - Internal components: vertical dividers, horizontal shelves, drawers (notch & handle), hanging clothes rails, vertical poles, inclined shoe racks, and full/compartment cabinet doors.
  - **Zero-Penetration & Magnetic Snapping**: internal units strictly respect bay compartments and automatically adapt heights and widths when dividers or shelves move.
- **Accurate Screen-to-World Raycasting**:
  - Custom ray-plane and ray-box intersection algorithms for instant 3D picking of carcasses, shelves, and dividers directly on touch.
- **Local Persistence & Export**:
  - Robust JSON persistence with tolerant codecs (`kotlinx.serialization`).
  - Unit system converter (mm, cm, m, inches, feet/inches) and full bilingual localization (English / Spanish).

---

## Architecture

The project is structured following **Clean / Hexagonal Architecture** principles, strictly decoupling domain logic and geometric calculations from the Android UI framework and the 3D graphics pipeline:

```
App_3D/
├── core/                                # Pure Kotlin/JVM domain & geometry module (zero Android dependencies)
│   ├── src/main/kotlin/com/closetstudio/core/
│   │   ├── geometry/                    # Procedural mesh generation (MeshBuilder, RoomGeometryBuilder)
│   │   │   ├── MeshBuilder.kt           # Low-level triangle, quad, vertex normal, and UV generation
│   │   │   ├── RoomGeometry.kt          # Architectural room, walls, corners, and carcass geometry builders
│   │   │   └── Vec3.kt                  # Lightweight 3D vector arithmetic
│   │   └── model/                       # Domain entities, constraints, and solvers
│   │       ├── Project.kt               # RoomSpec, ClosetContainerSpec, ClosetPartitionSpec, and raycasting
│   │       └── ProjectCodec.kt          # JSON serialization codecs
│   └── src/test/                        # Rigorous unit tests for geometry invariants and solvers
│
└── app/                                 # Android Application module (UI, Filament adapter, and storage)
    └── src/main/java/com/closetstudio/app/
        ├── data/                        # ProjectStorage repository implementation
        ├── render/                      # SceneView / Google Filament adapters (MeshNode, MeshGeometry)
        └── ui/                          # Jetpack Compose UI, 2D Floor Plan, and 3D viewport orchestration
            ├── RoomScreen.kt            # Main screen state, gesture arbitration, and view transitions
            ├── RoomScene.kt             # SceneView 3D scene, PBR materials, lights, and camera nodes
            ├── RoomFloorplan2D.kt       # Interactive 2D Canvas CAD floor plan
            └── MaterialsDialog.kt       # Material & texture catalog picker
```

### Architectural Principles
- **Domain Independence (`:core`)**: Pure Kotlin JVM module. All geometry formulas, wall angle compensation solvers, bay subdivision math, and collision bounds live in `:core` and can be tested without an Android emulator or robolectric.
- **Single Source of Truth**: The `RoomSpec` immutable data model drives both the 2D CAD floor plan canvas and the 3D Filament mesh generator.
- **Coordinate System**: All domain dimensions and positions are calculated in **millimeters (mm)**. The rendering adapter converts to Filament meters ($1\text{ m} = 1000\text{ mm}$) strictly at the boundary.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.4.21 |
| **UI Framework** | Jetpack Compose (BOM 2025.02.00) + Material 3 |
| **3D Engine** | SceneView 4.63.0 (backed by Google Filament PBR) |
| **Serialization** | `kotlinx.serialization` (JSON) |
| **Build Tooling** | Gradle 9.5 (Kotlin DSL), Android Gradle Plugin 9.5.0 |
| **Target SDK** | Android 14+ (Compile SDK 36, Min SDK 26) |
| **Testing** | JUnit 5 (Jupiter), Kotlin Test |

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 21+ (configured in `JAVA_HOME`)
- Android SDK 36

### Build and Run
Clone the repository and run the debug build:

```bash
# Compile and run unit test suite across :core and :app
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Install to connected device or emulator
./gradlew installDebug
```

---

## Verification & Testing

The project maintains high test coverage over domain rules, carpentry constraints, and polygon compensation algorithms:

- `RoomGeometryTest`: Generates 3D meshes for room surfaces, doors, and closet carcasses across acute, orthogonal, and obtuse corners.
- `MeshBuilderTest`: Asserts unit normals, CCW quad winding, and physical world-space UV scaling.
- `ClosetPartitionTest`: Verifies zero penetration, bay subdivision, elevation snapping, and compartment bounds.
- `ClosetContainerMovementTest`: Asserts corner decoupling on drag and same-wall collision stopping.
- `RoomSpecTest`: Comprehensive stress tests for wall locks (*candados*), corner angle compensation, and geometric minima.
- `ProjectStorageTest`: Verifies persistent JSON round-trips and error handling.

---

## License

This project is licensed under the [MIT License](LICENSE).
