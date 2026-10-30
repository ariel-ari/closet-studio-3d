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
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.SceneView
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
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
    override fun scrollBegin(x: Int, y: Int, amount: Float) {}
    override fun scrollUpdate(x: Int, y: Int, amount: Float, delta: Float) {}
    override fun scrollEnd() {}
    override fun update(deltaTime: Float) {}
}

@Composable
fun RoomScene(
    room: RoomSpec,
    yawAngleDegrees: Float,
    pitchAngleDegrees: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val parts = remember(room) { RoomGeometryBuilder.build(room) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)

    // Room center coordinates and human eye height (1.40 m)
    val centerX = room.clearWidthMm * 0.5f * MM_TO_M
    val eyeY = 1.40f // 1.40 m from finished floor
    val centerZ = room.clearDepthMm * 0.5f * MM_TO_M

    // Un paso suave hacia atrás (0.65 m) respecto a la pared activa, continuo e imperceptible
    val backOffset = 0.65f.coerceAtMost(minOf(centerX, centerZ) * 0.45f)

    val manipulator = remember(centerX, centerZ) {
        PanoramicCameraManipulator().apply {
            eye = Float3(centerX, eyeY, centerZ)
            target = Float3(centerX, eyeY, centerZ + 1f)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val aspect = (maxWidth.value / maxHeight.value.coerceAtLeast(1f)).toDouble()

        LaunchedEffect(room, yawAngleDegrees, pitchAngleDegrees, cameraNode, aspect) {
            val yawRad = Math.toRadians(yawAngleDegrees.toDouble()).toFloat()
            val pitchRad = Math.toRadians(pitchAngleDegrees.toDouble()).toFloat()

            // Vector de dirección de la mirada
            val dirX = sin(yawRad) * cos(pitchRad)
            val dirY = sin(pitchRad)
            val dirZ = -cos(yawRad) * cos(pitchRad)

            // Posición con retroceso dinámico suave (órbita circular interior imperceptible)
            val eye = Float3(
                centerX - (sin(yawRad) * backOffset),
                eyeY,
                centerZ - (-cos(yawRad) * backOffset),
            )

            val target = Float3(eye.x + dirX, eye.y + dirY, eye.z + dirZ)

            manipulator.eye = eye
            manipulator.target = target

            // En tablet/horizontal (aspect >= 1.0) fijamos FOV VERTICAL (74°) para que se vean techo y piso con aire
            // En celular/vertical (aspect < 1.0) fijamos FOV HORIZONTAL (86°) para que entre el ancho de pared
            val (fov, direction) = if (aspect >= 1.0) {
                74.0 to Camera.Fov.VERTICAL
            } else {
                86.0 to Camera.Fov.HORIZONTAL
            }

            try {
                cameraNode.setProjection(
                    fov,
                    aspect,
                    0.05,
                    50.0,
                    direction,
                )
            } catch (_: Throwable) {}
        }

        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = manipulator,
            autoCenterContent = false,
            autoFitContent = false,
        ) {
            // High-intensity central interior light (ceiling lamp)
            LightNode(
                type = LightManager.Type.POINT,
                position = Float3(centerX, (room.clearHeightMm - 100f) * MM_TO_M, centerZ),
                intensity = 250_000f,
                color = Float4(1f, 0.98f, 0.94f, 1f),
            )

            // Eye-level ambient fill light to guarantee every wall is brilliantly illuminated
            LightNode(
                type = LightManager.Type.POINT,
                position = Float3(centerX, eyeY, centerZ),
                intensity = 150_000f,
                color = Float4(1f, 1f, 1f, 1f),
            )

            parts.forEachIndexed { index, part ->
                key(part.surface, index) {
                    val material = remember(engine, part.surface) {
                        when (part.surface) {
                            RoomSurface.FLOOR -> materialLoader.createColorInstance(
                                color = Color(0xFFE0D2BE), // Bright light oak parquet
                                metallic = 0.05f,
                                roughness = 0.35f,
                                reflectance = 0.5f,
                            )
                            RoomSurface.WALL -> materialLoader.createColorInstance(
                                color = Color(0xFFFAF7F0), // Clean bright warm eggshell wall
                                metallic = 0.0f,
                                roughness = 0.5f,
                                reflectance = 0.6f,
                            )
                            RoomSurface.CEILING -> materialLoader.createColorInstance(
                                color = Color(0xFFFAF9F6),
                                metallic = 0.0f,
                                roughness = 0.9f,
                                reflectance = 0.2f,
                            )
                            RoomSurface.DOOR_FRAME -> materialLoader.createColorInstance(
                                color = Color(0xFF8B5A2B), // Warm rich wood frame
                                metallic = 0.0f,
                                roughness = 0.45f,
                                reflectance = 0.5f,
                            )
                            RoomSurface.DOOR_LEAF -> materialLoader.createColorInstance(
                                color = Color(0xFFA67444), // Warm cedar wood panel
                                metallic = 0.05f,
                                roughness = 0.4f,
                                reflectance = 0.55f,
                            )
                            RoomSurface.DOOR_HANDLE -> materialLoader.createColorInstance(
                                color = Color(0xFFF5F5F5), // Polished bright chrome
                                metallic = 0.95f,
                                roughness = 0.1f,
                                reflectance = 0.95f,
                            )
                        }
                    }

                    MeshNode(
                        mesh = part.mesh,
                        material = material,
                        name = "room-${part.surface.name.lowercase()}-$index",
                    )
                }
            }
        }
    }
}
