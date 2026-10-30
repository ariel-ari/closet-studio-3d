package com.closetstudio.core.geometry

import com.closetstudio.core.assertMm
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomGeometryTest {

    @Test
    fun `generates all room surfaces including door`() {
        val room = RoomSpec(clearWidthMm = 3600f, clearHeightMm = 2600f, clearDepthMm = 3600f)
        val parts = RoomGeometryBuilder.build(room)

        assertNotNull(parts.find { it.surface == RoomSurface.FLOOR })
        assertNotNull(parts.find { it.surface == RoomSurface.WALL })
        assertNotNull(parts.find { it.surface == RoomSurface.DOOR_FRAME })
        assertNotNull(parts.find { it.surface == RoomSurface.DOOR_LEAF })
    }

    @Test
    fun `floor encloses bottom level`() {
        val room = RoomSpec(clearWidthMm = 3000f, clearHeightMm = 2500f, clearDepthMm = 3000f)
        val parts = RoomGeometryBuilder.build(room)

        val floor = assertNotNull(parts.find { it.surface == RoomSurface.FLOOR })
        assertMm(0f, floor.mesh.bounds().max.y)
    }

    @Test
    fun `door offset leaves wall before and after the opening`() {
        val room = RoomSpec(
            clearWidthMm = 4000f,
            clearHeightMm = 2600f,
            clearDepthMm = 4000f,
            door = DoorSpec(offsetMm = 1000f, widthMm = 900f, heightMm = 2100f),
        )
        val parts = RoomGeometryBuilder.build(room)
        val walls = assertNotNull(parts.find { it.surface == RoomSurface.WALL })

        val bounds = walls.mesh.bounds()
        // Wall spans full room opening
        assertTrue(bounds.min.x <= 0f)
        assertTrue(bounds.max.x >= 4000f)
        assertTrue(bounds.max.z >= 4000f)
    }
}
