package com.closetstudio.core.geometry

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MeshBuilderTest {

    private fun boxOf600() = MeshBuilder(uvScaleMm = 600f).apply {
        addBox(Vec3.ZERO, Vec3(600f, 600f, 600f))
    }.build()

    @Test
    fun `a box is six quads with twelve triangles`() {
        val mesh = boxOf600()

        assertEquals(24, mesh.vertexCount)
        assertEquals(36, mesh.indices.size)
        assertEquals(12, mesh.triangleCount)
    }

    @Test
    fun `every normal is unit length`() {
        val mesh = boxOf600()

        var i = 0
        while (i < mesh.normals.size) {
            val length = sqrt(
                mesh.normals[i] * mesh.normals[i] +
                    mesh.normals[i + 1] * mesh.normals[i + 1] +
                    mesh.normals[i + 2] * mesh.normals[i + 2],
            )
            assertTrue(abs(length - 1f) < 1e-4f, "normal $i has length $length")
            i += 3
        }
    }

    @Test
    fun `winding matches the outward normal on every triangle`() {
        val mesh = boxOf600()

        var t = 0
        while (t < mesh.indices.size) {
            val i0 = mesh.indices[t]
            val i1 = mesh.indices[t + 1]
            val i2 = mesh.indices[t + 2]

            val ax = mesh.positions[i1 * 3] - mesh.positions[i0 * 3]
            val ay = mesh.positions[i1 * 3 + 1] - mesh.positions[i0 * 3 + 1]
            val az = mesh.positions[i1 * 3 + 2] - mesh.positions[i0 * 3 + 2]
            val bx = mesh.positions[i2 * 3] - mesh.positions[i0 * 3]
            val by = mesh.positions[i2 * 3 + 1] - mesh.positions[i0 * 3 + 1]
            val bz = mesh.positions[i2 * 3 + 2] - mesh.positions[i0 * 3 + 2]

            val cx = ay * bz - az * by
            val cy = az * bx - ax * bz
            val cz = ax * by - ay * bx

            val dot = cx * mesh.normals[i0 * 3] +
                cy * mesh.normals[i0 * 3 + 1] +
                cz * mesh.normals[i0 * 3 + 2]

            assertTrue(dot > 0f, "triangle $t is wound against its normal (dot=$dot)")
            t += 3
        }
    }

    @Test
    fun `uvs are world space and scaled by the physical tile size`() {
        val mesh = boxOf600()

        var minU = Float.POSITIVE_INFINITY
        var maxU = Float.NEGATIVE_INFINITY
        var minV = Float.POSITIVE_INFINITY
        var maxV = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < mesh.uvs.size) {
            minU = minOf(minU, mesh.uvs[i])
            maxU = maxOf(maxU, mesh.uvs[i])
            minV = minOf(minV, mesh.uvs[i + 1])
            maxV = maxOf(maxV, mesh.uvs[i + 1])
            i += 2
        }

        // A 600 mm panel with a 600 mm tile must span exactly one tile.
        assertTrue(abs(minU) < 1e-4f, "minU=$minU")
        assertTrue(abs(minV) < 1e-4f, "minV=$minV")
        assertTrue(abs(maxU - 1f) < 1e-4f, "maxU=$maxU")
        assertTrue(abs(maxV - 1f) < 1e-4f, "maxV=$maxV")
    }

    @Test
    fun `a 1200 mm panel against a 600 mm tile spans two tiles`() {
        val mesh = MeshBuilder(uvScaleMm = 600f).apply {
            addBox(Vec3.ZERO, Vec3(1200f, 600f, 600f))
        }.build()

        var maxU = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < mesh.uvs.size) {
            maxU = maxOf(maxU, mesh.uvs[i])
            i += 2
        }

        assertTrue(abs(maxU - 2f) < 1e-4f, "maxU=$maxU")
    }

    @Test
    fun `bounds report the box envelope`() {
        val mesh = MeshBuilder(600f).apply {
            addBoxOriginSize(Vec3(10f, 20f, 30f), Vec3(100f, 200f, 300f))
        }.build()

        val bounds = mesh.bounds()
        assertEquals(Vec3(10f, 20f, 30f), bounds.min)
        assertEquals(Vec3(110f, 220f, 330f), bounds.max)
    }

    @Test
    fun `a degenerate normal is rejected instead of producing NaNs`() {
        assertFailsWith<IllegalArgumentException> {
            MeshBuilder().addQuad(Vec3.ZERO, Vec3.UNIT_X, Vec3.UNIT_Y, Vec3.UNIT_Z, Vec3.ZERO)
        }
    }

    @Test
    fun `translation moves positions but not normals or uvs`() {
        val mesh = boxOf600()
        val moved = mesh.translated(Vec3(1000f, 0f, 0f))

        assertEquals(mesh.positions[0] + 1000f, moved.positions[0])
        assertEquals(mesh.normals[0], moved.normals[0])
        assertEquals(mesh.uvs[0], moved.uvs[0])
    }
}
