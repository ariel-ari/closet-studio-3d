package com.closetstudio.app.render

import com.closetstudio.core.geometry.MeshData
import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.geometries.Geometry

/** The domain model is in millimetres; Filament works in metres. */
const val MM_TO_M: Float = 0.001f

/**
 * Uploads a [MeshData] to Filament.
 *
 * This is the only place that converts millimetres to metres: the mesh data
 * itself stays in the domain unit so tests can assert real cabinet dimensions.
 */
fun MeshData.toFilamentGeometry(engine: Engine): Geometry {
    val vertices = ArrayList<Geometry.Vertex>(vertexCount)
    for (i in 0 until vertexCount) {
        vertices += Geometry.Vertex(
            position = Float3(
                positions[i * 3] * MM_TO_M,
                positions[i * 3 + 1] * MM_TO_M,
                positions[i * 3 + 2] * MM_TO_M,
            ),
            normal = Float3(
                normals[i * 3],
                normals[i * 3 + 1],
                normals[i * 3 + 2],
            ),
            uvCoordinate = Float2(uvs[i * 2], uvs[i * 2 + 1]),
            color = COLORS_NONE,
        )
    }

    return Geometry.Builder(RenderableManager.PrimitiveType.TRIANGLES)
        .vertices(vertices)
        .indices(indices.toList())
        .build(engine)
}

/** Vertex colours are unused: colour comes from the material instance. */
private val COLORS_NONE = Float4(1f, 1f, 1f, 1f)
