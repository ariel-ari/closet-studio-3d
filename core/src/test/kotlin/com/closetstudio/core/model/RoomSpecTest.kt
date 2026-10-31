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

    @Test
    fun `modifying wall B independently from wall C adapts south wall length and corner angles to sum 360`() {
        val room = RoomSpec(clearWidthMm = 3600f, clearDepthMm = 3600f)
        val updated = room.withWallB(4200f)

        assertEquals(3600f, updated.wallCMm, 0.01f)
        assertEquals(4200f, updated.wallBMm, 0.01f)
        // Hypotenuse: sqrt(3600^2 + 600^2) = 3649.6575
        assertEquals(3649.6575f, updated.wallSouthMm, 0.5f)
        val sumAngles = updated.angleACDeg + updated.angleABDeg + updated.angleCDDeg + updated.angleBEDeg
        assertEquals(360f, sumAngles, 0.1f)
        assertTrue(updated.angleCDDeg > 90f)
        assertTrue(updated.angleBEDeg < 90f)
    }

    @Test
    fun `modifying corner angle C-D adapts wall B while preserving locked walls A and C`() {
        val room = RoomSpec(clearWidthMm = 3600f, clearDepthMm = 3600f)
        val updated = room.withAngleCD(95f)

        assertEquals(3600f, updated.wallAMm, 0.01f)
        assertEquals(3600f, updated.wallCMm, 0.01f)
        assertEquals(95f, updated.angleCDDeg, 0.2f)
        assertEquals(85f, updated.angleBEDeg, 0.2f)
        assertTrue(updated.wallBMm > 3600f)
    }
}
