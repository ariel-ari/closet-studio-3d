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

    @Test
    fun `locking corner C-D keeps angleCDDeg strictly invariant when editing angleACDeg`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            lockAngleAC = false,
            lockAngleAB = true,
            lockAngleCD = true,
            lockAngleBE = false,
        )
        val updated = room.withAngleAC(100f)

        assertEquals(100f, updated.angleACDeg, 0.05f)
        assertEquals(90f, updated.angleABDeg, 0.05f)
        assertEquals(90f, updated.angleCDDeg, 0.05f) // Locked C-D MUST remain 90°!
        assertEquals(80f, updated.angleBEDeg, 0.05f) // Unlocked B-E absorbs the -10°
    }

    @Test
    fun `locking corner B-E keeps angleBEDeg strictly invariant when editing angleACDeg`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            lockAngleAC = false,
            lockAngleAB = true,
            lockAngleCD = false,
            lockAngleBE = true,
        )
        val updated = room.withAngleAC(98f)

        assertEquals(98f, updated.angleACDeg, 0.05f)
        assertEquals(90f, updated.angleABDeg, 0.05f)
        assertEquals(82f, updated.angleCDDeg, 0.05f) // Unlocked C-D absorbs -8°
        assertEquals(90f, updated.angleBEDeg, 0.05f) // Locked B-E MUST remain 90°!
    }

    @Test
    fun `locking both bottom corners C-D and B-E routes compensation to unlocked top corner A-B`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            lockAngleAC = false,
            lockAngleAB = false,
            lockAngleCD = true,
            lockAngleBE = true,
        )
        val updated = room.withAngleAC(96f)

        assertEquals(96f, updated.angleACDeg, 0.05f)
        assertEquals(84f, updated.angleABDeg, 0.05f) // Unlocked A-B absorbs -6°
        assertEquals(90f, updated.angleCDDeg, 0.05f) // Locked C-D stays 90°
        assertEquals(90f, updated.angleBEDeg, 0.05f) // Locked B-E stays 90°
    }

    @Test
    fun `editing corner C-D when B-E is locked preserves B-E and routes compensation to unlocked A-C`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            lockAngleAC = false,
            lockAngleAB = true,
            lockAngleCD = false,
            lockAngleBE = true,
        )
        val updated = room.withAngleCD(97f)

        assertEquals(83f, updated.angleACDeg, 0.05f) // Unlocked A-C absorbs -7°
        assertEquals(90f, updated.angleABDeg, 0.05f) // Locked A-B stays 90°
        assertEquals(97f, updated.angleCDDeg, 0.05f)
        assertEquals(90f, updated.angleBEDeg, 0.05f) // Locked B-E stays 90°!
    }

    @Test
    fun `locked door width remains strictly invariant when walls or angles grow`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            door = DoorSpec(widthMm = 900f, lockWidth = true),
        )
        val widened = room.withWallA(4800f, keepDoorCentered = true)
        assertEquals(900f, widened.door.widthMm, 0.01f)
        assertEquals(1950f, widened.segmentDMm, 0.5f)
        assertEquals(1950f, widened.segmentEMm, 0.5f)

        val angled = widened.withAngleAC(100f, keepDoorCentered = true)
        assertEquals(900f, angled.door.widthMm, 0.01f)
    }

    @Test
    fun `unlocked door width scales proportionally when door wall grows`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            door = DoorSpec(widthMm = 900f, lockWidth = false),
        )
        val widened = room.withWallSouth(4800f, keepDoorCentered = true)
        assertEquals(1200f, widened.door.widthMm, 10f)
    }

    @Test
    fun `locking a wall prevents direct resizing and routes angle adaptation to unlocked opposite wall`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearDepthMm = 3600f,
            wallBMm = 3600f,
            lockWallB = true, // Right wall (UI Pared C) locked
            lockWallC = false, // Left wall (UI Pared A) unlocked
        )
        // Direct edit on locked wall B must be ignored
        val directAttempt = room.withWallB(4500f)
        assertEquals(3600f, directAttempt.wallBMm, 0.01f)

        // Changing corner angle C-D when wallBMm is locked must preserve wallBMm and adapt unlocked wallCMm
        val angled = room.withAngleCD(96f)
        assertEquals(3600f, angled.wallBMm, 0.01f)
        assertTrue(angled.wallCMm < 3600f)
        assertEquals(96f, angled.angleCDDeg, 0.2f)
    }
}
