package com.closetstudio.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.closetstudio.app.render.MM_TO_M
import com.closetstudio.app.render.MeshPartNode
import com.closetstudio.core.geometry.Surface
import com.closetstudio.core.model.ClosetProject
import com.closetstudio.core.model.FinishSpec
import com.closetstudio.core.scene.ClosetAssembly
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader

/**
 * The 3D editor viewport.
 *
 * Geometry is rebuilt from the project value, so every edit to the room or the
 * module list flows through the same tested `ClosetAssembly` path. Node and
 * material identity is keyed on the part and its colour, so a colour change
 * swaps instances instead of leaking them.
 */
@Composable
fun ClosetScene(project: ClosetProject, modifier: Modifier = Modifier) {
    val assembly = remember(project) { ClosetAssembly.build(project) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    // The camera is created once: re-creating it on every slider frame would reset
    // the orbit the user just set up.
    val room = project.room
    val cameraManipulator = rememberCameraManipulator(
        orbitRadius = maxOf(room.clearWidthMm, room.clearHeightMm) * MM_TO_M * 1.7f,
        targetPosition = Float3(
            room.clearWidthMm * 0.5f * MM_TO_M,
            room.clearHeightMm * 0.5f * MM_TO_M,
            room.clearDepthMm * 0.5f * MM_TO_M,
        ),
    )

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        cameraManipulator = cameraManipulator,
    ) {
        assembly.parts.forEachIndexed { index, part ->
            key(index, part.surface) {
                val color = part.surface.colorFor(project.finish)
                val material = remember(engine, color) {
                    materialLoader.createColorInstance(
                        color = Color(color),
                        metallic = 0f,
                        roughness = 0.55f,
                        reflectance = 0.4f,
                    )
                }
                MeshPartNode(
                    part = part,
                    material = material,
                    name = "${part.surface.name.lowercase()}-$index",
                )
            }
        }
    }
}

private fun Surface.colorFor(finish: FinishSpec): Long = when (this) {
    Surface.CARCASS -> finish.carcassColor
    Surface.BACK -> finish.backColor
    Surface.FRONT -> finish.frontColor
    Surface.TOE_KICK -> finish.toeKickColor
}
