package com.closetstudio.app.ui

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
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import kotlin.math.cos
import kotlin.math.sin

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

    // Position the camera in the center of the room at eye level
    val eyeX = room.clearWidthMm * 0.5f * MM_TO_M
    val eyeY = (room.clearHeightMm * 0.5f).coerceIn(1400f, 1600f) * MM_TO_M
    val eyeZ = room.clearDepthMm * 0.5f * MM_TO_M

    LaunchedEffect(room, yawAngleDegrees, pitchAngleDegrees, cameraNode) {
        val eye = Float3(eyeX, eyeY, eyeZ)

        val yawRad = Math.toRadians(yawAngleDegrees.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchAngleDegrees.toDouble()).toFloat()

        // Direction vector from spherical coordinates
        val dirX = sin(yawRad) * cos(pitchRad)
        val dirY = sin(pitchRad)
        val dirZ = -cos(yawRad) * cos(pitchRad)

        val target = Float3(eye.x + dirX, eye.y + dirY, eye.z + dirZ)
        cameraNode.lookAt(eye, target, Float3(0f, 1f, 0f))
    }

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = null,
    ) {
        // High-intensity central interior light (ceiling lamp)
        LightNode(
            type = LightManager.Type.POINT,
            position = Float3(eyeX, (room.clearHeightMm - 100f) * MM_TO_M, eyeZ),
            intensity = 250_000f,
            color = Float4(1f, 0.98f, 0.94f, 1f),
        )

        // Eye-level ambient fill light to guarantee every wall is brilliantly illuminated
        LightNode(
            type = LightManager.Type.POINT,
            position = Float3(eyeX, eyeY, eyeZ),
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
