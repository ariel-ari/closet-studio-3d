package com.closetstudio.core.geometry

/**
 * Accumulates quads into a [MeshData].
 *
 * UVs are world-space planar projections divided by [uvScaleMm]: a panel of
 * 600 mm and a panel of 1200 mm of the same material show the same physical
 * texture size, which is what makes wood grain look consistent across a run of
 * carcasses. The projection axis is chosen from the dominant normal component.
 */
class MeshBuilder(
    private val uvScaleMm: Float = 600f,
    private val rotateUv90: Boolean = false,
) {
    private val positions = FloatList(256)
    private val normals = FloatList(256)
    private val uvs = FloatList(256)
    private val indices = IntList(128)

    fun addQuad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, normal: Vec3) {
        val n = normalize(normal)
        val base = positions.size / 3
        for (p in arrayOf(a, b, c, d)) {
            positions.add(p.x)
            positions.add(p.y)
            positions.add(p.z)
            normals.add(n.x)
            normals.add(n.y)
            normals.add(n.z)
            val uv = planarUv(p, n)
            uvs.add(uv.first)
            uvs.add(uv.second)
        }
        // Two triangles: 0-1-2 and 0-2-3 keep the quad's winding.
        indices.add(base)
        indices.add(base + 1)
        indices.add(base + 2)
        indices.add(base)
        indices.add(base + 2)
        indices.add(base + 3)
    }

    /** Adds an axis aligned box spanning [min]..[max] with outward facing normals. */
    fun addBox(min: Vec3, max: Vec3) {
        val (x0, y0, z0) = Triple(min.x, min.y, min.z)
        val (x1, y1, z1) = Triple(max.x, max.y, max.z)

        // +X
        addQuad(
            Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x1, y1, z1), Vec3(x1, y0, z1),
            Vec3.UNIT_X,
        )
        // -X
        addQuad(
            Vec3(x0, y0, z0), Vec3(x0, y0, z1), Vec3(x0, y1, z1), Vec3(x0, y1, z0),
            Vec3(-1f, 0f, 0f),
        )
        // +Y
        addQuad(
            Vec3(x0, y1, z0), Vec3(x0, y1, z1), Vec3(x1, y1, z1), Vec3(x1, y1, z0),
            Vec3.UNIT_Y,
        )
        // -Y
        addQuad(
            Vec3(x0, y0, z0), Vec3(x1, y0, z0), Vec3(x1, y0, z1), Vec3(x0, y0, z1),
            Vec3(0f, -1f, 0f),
        )
        // +Z
        addQuad(
            Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x0, y1, z1),
            Vec3.UNIT_Z,
        )
        // -Z
        addQuad(
            Vec3(x0, y0, z0), Vec3(x0, y1, z0), Vec3(x1, y1, z0), Vec3(x1, y0, z0),
            Vec3(0f, 0f, -1f),
        )
    }

    /** Box defined by origin plus size, convenient for panel style envelopes. */
    fun addBoxOriginSize(origin: Vec3, size: Vec3) {
        require(size.x > 0f && size.y > 0f && size.z > 0f) { "box size must be positive, got $size" }
        addBox(origin, origin + size)
    }

    fun build(): MeshData = MeshData(
        positions.toArray(),
        normals.toArray(),
        uvs.toArray(),
        indices.toArray(),
    )

    private fun planarUv(p: Vec3, n: Vec3): Pair<Float, Float> {
        val ax = kotlin.math.abs(n.x)
        val ay = kotlin.math.abs(n.y)
        val az = kotlin.math.abs(n.z)
        val scale = if (uvScaleMm == 0f) 1f else 1f / uvScaleMm
        val (u, v) = when {
            ax >= ay && ax >= az -> (p.z * scale) to (p.y * scale)
            ay >= ax && ay >= az -> (p.x * scale) to (p.z * scale)
            else -> (p.x * scale) to (p.y * scale)
        }
        return if (rotateUv90) (v to u) else (u to v)
    }

    private fun normalize(v: Vec3): Vec3 {
        val len = v.length
        require(len > 1e-6f) { "normal must be non zero" }
        return Vec3(v.x / len, v.y / len, v.z / len)
    }
}

/** Minimal growable float list: avoids boxing on the per-vertex hot path. */
private class FloatList(initialCapacity: Int) {
    private var data = FloatArray(initialCapacity)
    var size: Int = 0
        private set

    fun add(value: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun toArray(): FloatArray = data.copyOf(size)
}

private class IntList(initialCapacity: Int) {
    private var data = IntArray(initialCapacity)
    var size: Int = 0
        private set

    fun add(value: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun toArray(): IntArray = data.copyOf(size)
}
