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
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.node.CameraNode
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

    // Position the camera in the center of the room at eye level (1.5 m)
    LaunchedEffect(room, yawAngleDegrees, pitchAngleDegrees, cameraNode) {
        val eyeX = room.clearWidthMm * 0.5f * MM_TO_M
        val eyeY = (room.clearHeightMm * 0.5f).coerceIn(1400f, 1700f) * MM_TO_M
        val eyeZ = room.clearDepthMm * 0.5f * MM_TO_M

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
        cameraManipulator = null, // Custom 360 camera control
    ) {
        parts.forEachIndexed { index, part ->
            key(part.surface, index) {
                val material = remember(engine, part.surface) {
                    when (part.surface) {
                        RoomSurface.FLOOR -> materialLoader.createColorInstance(
                            color = Color(0xFFD6C8B4), // Parquet floor tone
                            metallic = 0.05f,
                            roughness = 0.45f,
                            reflectance = 0.4f,
                        )
                        RoomSurface.WALL -> materialLoader.createColorInstance(
                            color = Color(0xFFEBE8E1), // Wallpaper tone
                            metallic = 0.0f,
                            roughness = 0.85f,
                            reflectance = 0.2f,
                        )
                        RoomSurface.CEILING -> materialLoader.createColorInstance(
                            color = Color(0xFFF8F7F4),
                            metallic = 0.0f,
                            roughness = 0.95f,
                            reflectance = 0.1f,
                        )
                        RoomSurface.DOOR_FRAME -> materialLoader.createColorInstance(
                            color = Color(0xFF7A5430), // Dark Oak frame
                            metallic = 0.0f,
                            roughness = 0.5f,
                            reflectance = 0.4f,
                        )
                        RoomSurface.DOOR_LEAF -> materialLoader.createColorInstance(
                            color = Color(0xFF9E7247), // Wood door panel
                            metallic = 0.05f,
                            roughness = 0.4f,
                            reflectance = 0.5f,
                        )
                        RoomSurface.DOOR_HANDLE -> materialLoader.createColorInstance(
                            color = Color(0xFFE0E0E0), // Brushed chrome
                            metallic = 0.9f,
                            roughness = 0.2f,
                            reflectance = 0.9f,
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
