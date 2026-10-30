package com.closetstudio.core.geometry

/**
 * Render-agnostic triangle mesh.
 *
 * Deliberately plain arrays: it can be produced and asserted in unit tests with no
 * GL context, no engine and no Android runtime, then handed to Filament's vertex and
 * index buffers by the app layer.
 *
 * Layout:
 *  - [positions] xyz per vertex
 *  - [normals]   xyz per vertex, unit length
 *  - [uvs]       uv per vertex
 *  - [indices]   triangle list
 */
class MeshData(
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray,
    val indices: IntArray,
) {
    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3

    init {
        require(positions.size % 3 == 0) { "positions must be xyz triples, got ${positions.size}" }
        require(normals.size == positions.size) { "normals must match positions size" }
        require(uvs.size == vertexCount * 2) { "uvs must be 2 per vertex" }
        require(indices.size % 3 == 0) { "indices must be triangles, got ${indices.size}" }
        require(indices.all { it in 0 until vertexCount }) { "index out of range" }
    }

    fun bounds(): Aabb {
        if (vertexCount == 0) return Aabb.EMPTY
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < positions.size) {
            val x = positions[i]
            val y = positions[i + 1]
            val z = positions[i + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
            i += 3
        }
        return Aabb(Vec3(minX, minY, minZ), Vec3(maxX, maxY, maxZ))
    }

    fun translated(offset: Vec3): MeshData {
        if (offset == Vec3.ZERO) return this
        val out = positions.copyOf()
        var i = 0
        while (i < out.size) {
            out[i] += offset.x
            out[i + 1] += offset.y
            out[i + 2] += offset.z
            i += 3
        }
        return MeshData(out, normals, uvs, indices)
    }

    companion object {
        val EMPTY = MeshData(FloatArray(0), FloatArray(0), FloatArray(0), IntArray(0))
    }
}
