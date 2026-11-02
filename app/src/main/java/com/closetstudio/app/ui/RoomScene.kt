package com.closetstudio.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.closetstudio.app.render.MM_TO_M
import com.closetstudio.app.render.MeshNode
import com.closetstudio.core.geometry.RoomGeometryBuilder
import com.closetstudio.core.geometry.RoomSurface
import com.closetstudio.core.model.RoomSpec
import com.google.android.filament.Camera
import com.google.android.filament.LightManager
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberView
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom interior first-person camera manipulator that locks the eye position
 * in the center of the room and smoothly looks at any wall angle.
 */
class PanoramicCameraManipulator : CameraGestureDetector.CameraManipulator {
    var eye: Float3 = Float3(0f, 1.4f, 0f)
    var target: Float3 = Float3(0f, 1.4f, -1f)

    override fun setViewport(width: Int, height: Int) {}
    override fun getTransform(): Mat4 = lookAt(eye, target, Float3(0f, 1f, 0f))
    override fun grabBegin(x: Int, y: Int, strafe: Boolean) {}
    override fun grabUpdate(x: Int, y: Int) {}
    override fun grabEnd() {}
    override fun scrollBegin(x: Int, y: Int, separation: Float) {}
    override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {}
    override fun scrollEnd() {}
    override fun update(deltaTime: Float) {}
}

@Composable
fun RoomScene(
    room: RoomSpec,
    target: Float3,
    yawAngleDegrees: Float,
    modifier: Modifier = Modifier,
    pitchAngleDegrees: Float = 0f,
    distance: Float = 2.45f,
    isOrtho: Boolean = false,
    orthoSize: Float = 3.0f,
    selectedContainerId: String? = null,
    selectedPartitionId: String? = null,
    showDoors: Boolean = true,
) {
    val parts = remember(room, selectedContainerId, selectedPartitionId, showDoors) {
        RoomGeometryBuilder.build(
            room,
            selectedContainerId = selectedContainerId,
            selectedPartitionId = selectedPartitionId,
            showDoors = showDoors,
        )
    }

    val engine = rememberEngine()
    val view = rememberView(engine) {
        engine.createView().apply {
            ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
                enabled = true
                radius = 0.18f // Tight architectural contact radius (18 cm) instead of wide halos
                intensity = 0.90f // Delicate realistic contact shadow, avoiding heavy dark soot
                power = 1.0f
                bias = 0.008f
                quality = View.QualityLevel.HIGH
            }
            setShadowingEnabled(true)
        }
    }
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val context = androidx.compose.ui.platform.LocalContext.current

    val textureCache = remember(engine) { mutableMapOf<String, com.google.android.filament.Texture?>() }
    androidx.compose.runtime.DisposableEffect(engine) {
        onDispose {
            textureCache.values.filterNotNull().forEach { tex ->
                runCatching { engine.destroyTexture(tex) }
            }
            textureCache.clear()
        }
    }

    // Room center coordinates and human eye height (1.40 m)
    val centerX = room.clearWidthMm * 0.5f * MM_TO_M
    val eyeY = 1.40f
    val centerZ = room.clearDepthMm * 0.5f * MM_TO_M

    val manipulator = remember {
        PanoramicCameraManipulator().apply {
            eye = Float3(centerX, eyeY, centerZ - distance)
            this.target = Float3(centerX, eyeY, centerZ)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val aspect = (maxWidth.value / maxHeight.value.coerceAtLeast(1f)).toDouble()

        LaunchedEffect(room, target, yawAngleDegrees, pitchAngleDegrees, distance, isOrtho, orthoSize, cameraNode, aspect) {
            val yawRad = Math.toRadians(yawAngleDegrees.toDouble()).toFloat()
            val pitchRad = Math.toRadians(pitchAngleDegrees.toDouble()).toFloat()

            // Unit gaze direction vector (from eye toward target)
            val dirX = sin(yawRad) * cos(pitchRad)
            val dirY = sin(pitchRad)
            val dirZ = -cos(yawRad) * cos(pitchRad)

            // When orthographic, distance only sets the depth placement between near and far planes
            val eyeDist = if (isOrtho) maxOf(distance, 4.0f) else distance
            val eye = Float3(
                target.x - (dirX * eyeDist),
                if (isOrtho && kotlin.math.abs(pitchAngleDegrees) < 5f) target.y else (target.y - (dirY * eyeDist)).coerceAtLeast(0.10f),
                target.z - (dirZ * eyeDist),
            )

            manipulator.eye = eye
            manipulator.target = target

            try {
                if (isOrtho) {
                    val halfH = (orthoSize * 0.5f).toDouble()
                    val halfW = halfH * aspect
                    cameraNode.setProjection(
                        Camera.Projection.ORTHO,
                        -halfW, halfW,
                        -halfH, halfH,
                        0.05, 50.0,
                    )
                } else {
                    val (fov, direction) = if (aspect >= 1.0) {
                        48.0 to Camera.Fov.VERTICAL
                    } else {
                        58.0 to Camera.Fov.HORIZONTAL
                    }
                    cameraNode.setProjection(
                        fov,
                        aspect,
                        0.05,
                        50.0,
                        direction,
                    )
                }
            } catch (t: Throwable) {
                android.util.Log.e("RoomScene", "setProjection error", t)
            }
        }

        SceneView(
            modifier = Modifier.fillMaxSize(),
            surfaceType = SurfaceType.TextureSurface,
            engine = engine,
            view = view,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = manipulator,
            autoCenterContent = false,
            autoFitContent = false,
            isOpaque = false,
        ) {
            // Eye-level ambient fill light to keep the room luminous, clean and airy
            LightNode(
                type = LightManager.Type.POINT,
                position = Float3(centerX, eyeY, centerZ),
                intensity = 130_000f,
                color = Float4(1f, 1f, 1f, 1f),
            )

            // Directional key light to provide natural architectural orientation
            LightNode(
                type = LightManager.Type.DIRECTIONAL,
                direction = Float3(-0.35f, -0.85f, -0.35f),
                intensity = 60_000f,
                color = Float4(1f, 1f, 1f, 1f),
            )

            parts.forEachIndexed { index, (surface, mesh, textureKey) ->
                key(surface, index, textureKey) {
                    val material = remember(engine, surface, textureKey) {
                        val loadedTexture = textureKey?.let { path ->
                            if (textureCache.containsKey(path)) {
                                textureCache[path]
                            } else {
                                val loaded = try {
                                    context.assets.open(path).use { stream ->
                                        val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                                        if (bitmap != null) {
                                            io.github.sceneview.texture.ImageTexture.Builder()
                                                .bitmap(bitmap, com.google.android.filament.utils.TextureType.COLOR)
                                                .build(engine)
                                        } else null
                                    }
                                } catch (t: Throwable) {
                                    android.util.Log.e("RoomScene", "Error loading texture: $path", t)
                                    null
                                }
                                textureCache[path] = loaded
                                loaded
                            }
                        }

                        val texturedMaterial = if (loadedTexture != null) {
                            val instance = when (surface) {
                                RoomSurface.FLOOR -> materialLoader.createTextureInstance(
                                    texture = loadedTexture,
                                    isOpaque = true,
                                    metallic = 0.0f,
                                    roughness = 0.80f,
                                    reflectance = 0.15f,
                                )
                                RoomSurface.WALL -> materialLoader.createTextureInstance(
                                    texture = loadedTexture,
                                    isOpaque = true,
                                    metallic = 0.0f,
                                    roughness = 0.95f,
                                    reflectance = 0.05f,
                                )
                                RoomSurface.CONTAINER_FRONT_EDGE -> materialLoader.createTextureInstance(
                                    texture = loadedTexture,
                                    isOpaque = true,
                                    metallic = 0.0f,
                                    roughness = 0.88f,
                                    reflectance = 0.08f,
                                )
                                RoomSurface.CONTAINER_BODY,
                                RoomSurface.CONTAINER_SIDE_PANEL -> materialLoader.createTextureInstance(
                                    texture = loadedTexture,
                                    isOpaque = true,
                                    metallic = 0.0f,
                                    roughness = 0.90f,
                                    reflectance = 0.08f,
                                )
                                else -> null
                            }
                            instance?.apply {
                                val sampler = com.google.android.filament.TextureSampler(
                                    com.google.android.filament.TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
                                    com.google.android.filament.TextureSampler.MagFilter.LINEAR,
                                    com.google.android.filament.TextureSampler.WrapMode.MIRRORED_REPEAT,
                                )
                                setParameter("texture", loadedTexture, sampler)
                            }
                        } else null

                        texturedMaterial ?: when (surface) {
                            RoomSurface.FLOOR -> materialLoader.createColorInstance(
                                color = Color(0xFFE0D2BE), // Bright light oak parquet
                                metallic = 0.0f,
                                roughness = 0.80f,
                                reflectance = 0.15f,
                            )
                            RoomSurface.WALL -> materialLoader.createColorInstance(
                                color = Color(0xFFFAF7F0), // Clean bright warm eggshell wall
                                metallic = 0.0f,
                                roughness = 0.95f,
                                reflectance = 0.05f,
                            )
                            RoomSurface.CEILING -> materialLoader.createColorInstance(
                                color = Color(0xFFFAF9F6),
                                metallic = 0.0f,
                                roughness = 0.95f,
                                reflectance = 0.05f,
                            )
                            RoomSurface.DOOR_FRAME -> materialLoader.createColorInstance(
                                color = Color(0xFF8B5A2B), // Warm rich wood frame
                                metallic = 0.0f,
                                roughness = 0.70f,
                                reflectance = 0.25f,
                            )
                            RoomSurface.DOOR_LEAF -> materialLoader.createColorInstance(
                                color = Color(0xFFA67444), // Warm cedar wood panel
                                metallic = 0.0f,
                                roughness = 0.80f,
                                reflectance = 0.15f,
                            )
                            RoomSurface.DOOR_HANDLE -> materialLoader.createColorInstance(
                                color = Color(0xFFF5F5F5), // Polished bright chrome
                                metallic = 0.95f,
                                roughness = 0.1f,
                                reflectance = 0.95f,
                            )
                            RoomSurface.CONTAINER_BODY -> materialLoader.createColorInstance(
                                color = Color(0xFFD2A679), // Warm oak horizontal shelf/roof surface
                                metallic = 0.0f,
                                roughness = 0.88f,
                                reflectance = 0.08f,
                            )
                            RoomSurface.CONTAINER_SIDE_PANEL -> materialLoader.createColorInstance(
                                color = Color(0xFFAA7C50), // Deeper warm wood for vertical side depth walls & plinth
                                metallic = 0.0f,
                                roughness = 0.90f,
                                reflectance = 0.08f,
                            )
                            RoomSurface.CONTAINER_FRONT_EDGE -> materialLoader.createColorInstance(
                                color = Color(0xFFE6C299), // Bright crisp front board thickness edge (canto frontal)
                                metallic = 0.0f,
                                roughness = 0.85f,
                                reflectance = 0.08f,
                            )
                            RoomSurface.CONTAINER_SELECTED_OUTLINE -> materialLoader.createColorInstance(
                                color = Color(0xFF3D5AFE), // Royal blue selection rim when the container is selected
                                metallic = 0.15f,
                                roughness = 0.25f,
                                reflectance = 0.80f,
                            )
                            RoomSurface.PARTITION_SELECTED_OUTLINE -> materialLoader.createColorInstance(
                                color = Color(0xFF8B5CF6), // Bright purple selection rim for active vertical divider or shelf
                                metallic = 0.15f,
                                roughness = 0.25f,
                                reflectance = 0.85f,
                            )
                            RoomSurface.CONTAINER_BACK -> materialLoader.createColorInstance(
                                color = Color(0xFFF5EFE6), // Architectural warm white linen backing board (fondo de placard)
                                metallic = 0.0f,
                                roughness = 0.95f,
                                reflectance = 0.05f,
                            )
                        }
                    }

                    MeshNode(
                        mesh = mesh,
                        material = material,
                        name = "room-${surface.name.lowercase()}-$index",
                    )
                }
            }
        }
    }
}
