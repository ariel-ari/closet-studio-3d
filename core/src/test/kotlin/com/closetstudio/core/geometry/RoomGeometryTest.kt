package com.closetstudio.core.geometry

import com.closetstudio.core.assertMm
import com.closetstudio.core.model.RoomSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RoomGeometryTest {

    @Test
    fun `floor top surface aligns with zero level`() {
        val room = RoomSpec(clearWidthMm = 3000f, clearHeightMm = 2500f, clearDepthMm = 800f)
        val parts = RoomGeometryBuilder.build(room)
        val floor = assertNotNull(parts.find { it.surface == RoomSurface.FLOOR })

        val bounds = floor.mesh.bounds()
        assertMm(0f, bounds.max.y)
    }

    @Test
    fun `walls cover full room width and height`() {
        val room = RoomSpec(clearWidthMm = 2800f, clearHeightMm = 2400f, clearDepthMm = 700f)
        val parts = RoomGeometryBuilder.build(room)
        val wall = assertNotNull(parts.find { it.surface == RoomSurface.WALL })

        val bounds = wall.mesh.bounds()
        assertEquals(true, bounds.max.x >= 2800f)
        assertEquals(true, bounds.max.y >= 2400f)
    }

    @Test
    fun `rejects non positive dimensions`() {
        assertFailsWith<IllegalArgumentException> {
            RoomSpec(clearWidthMm = -100f, clearHeightMm = 2000f, clearDepthMm = 600f)
        }
    }
}
