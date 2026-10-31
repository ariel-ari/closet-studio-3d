package com.closetstudio.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomSpecTest {

    @Test
    fun `default door is centered between segment D and E`() {
        val room = RoomSpec(clearWidthMm = 3600f, door = DoorSpec(widthMm = 900f, offsetMm = 1350f))
        assertEquals(1350f, room.segmentDMm, 0.001f)
        assertEquals(1350f, room.segmentEMm, 0.001f)
        assertTrue(room.isDoorCentered)
    }

    @Test
    fun `modifying segment D adapts segment E keeping door width immutable`() {
        val room = RoomSpec(clearWidthMm = 3600f, door = DoorSpec(widthMm = 900f))
        // Set segment D to 800 mm
        val updated = room.withSegmentD(800f)
        assertEquals(800f, updated.segmentDMm, 0.001f)
        assertEquals(900f, updated.door.widthMm, 0.001f)
        // Segment E should be 3600 - 900 - 800 = 1900 mm
        assertEquals(1900f, updated.segmentEMm, 0.001f)
        assertEquals(3600f, updated.segmentDMm + updated.door.widthMm + updated.segmentEMm, 0.001f)
    }

    @Test
    fun `modifying segment E adapts segment D keeping door width immutable`() {
        val room = RoomSpec(clearWidthMm = 3600f, door = DoorSpec(widthMm = 900f))
        // Set segment E to 1100 mm
        val updated = room.withSegmentE(1100f)
        assertEquals(1100f, updated.segmentEMm, 0.001f)
        assertEquals(900f, updated.door.widthMm, 0.001f)
        // Segment D should be 3600 - 900 - 1100 = 1600 mm
        assertEquals(1600f, updated.segmentDMm, 0.001f)
        assertEquals(3600f, updated.segmentDMm + updated.door.widthMm + updated.segmentEMm, 0.001f)
    }
}
