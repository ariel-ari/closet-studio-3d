package com.closetstudio.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader

@Composable
fun RoomScene(
    room: RoomSpec,
    modifier: Modifier = Modifier,
) {
    val parts = remember(room) { RoomGeometryBuilder.build(room) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    // Center the camera on the middle of the room volume
    val cameraManipulator = rememberCameraManipulator(
        orbitRadius = maxOf(room.clearWidthMm, room.clearHeightMm, room.clearDepthMm) * MM_TO_M * 2.2f,
        targetPosition = Float3(
            room.clearWidthMm * 0.5f * MM_TO_M,
            room.clearHeightMm * 0.45f * MM_TO_M,
            room.clearDepthMm * 0.5f * MM_TO_M,
        ),
    )

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraManipulator = cameraManipulator,
    ) {
        parts.forEachIndexed { index, part ->
            key(part.surface) {
                val material = remember(engine, part.surface) {
                    when (part.surface) {
                        RoomSurface.FLOOR -> materialLoader.createColorInstance(
                            color = Color(0xFFDED9D0),
                            metallic = 0.05f,
                            roughness = 0.5f,
                            reflectance = 0.4f,
                        )
                        RoomSurface.WALL -> materialLoader.createColorInstance(
                            color = Color(0xFFF7F7F5),
                            metallic = 0.0f,
                            roughness = 0.9f,
                            reflectance = 0.2f,
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
