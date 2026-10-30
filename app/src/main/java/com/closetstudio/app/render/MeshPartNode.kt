package com.closetstudio.app.render

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.closetstudio.core.geometry.MeshData
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.safeDestroyGeometry

/**
 * Declares one procedural mesh as a SceneView node.
 */
@SuppressLint("RestrictedApi")
@Composable
fun SceneScope.MeshNode(
    mesh: MeshData,
    material: MaterialInstance,
    name: String,
) {
    val geometry = remember(this.engine, mesh) { mesh.toFilamentGeometry(this.engine) }

    DisposableEffect(geometry) {
        onDispose { engine.safeDestroyGeometry(geometry) }
    }

    val node = remember(engine, geometry, material) {
        GeometryNode(
            engine = engine,
            geometry = geometry,
            materialInstance = material,
            destroyMaterialsOnDispose = true,
        ).apply { this.name = name }
    }

    NodeLifecycle(node, null)
}
