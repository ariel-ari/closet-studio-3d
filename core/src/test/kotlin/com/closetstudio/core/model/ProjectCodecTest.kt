package com.closetstudio.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectCodecTest {

    @Test
    fun `room dimensions round trip through json`() {
        val original = RoomSpec(clearWidthMm = 3200f, clearHeightMm = 2600f, clearDepthMm = 650f)
        val text = ProjectCodec.encode(original)
        val recovered = ProjectCodec.decode(text)

        assertEquals(original, recovered)
    }

    @Test
    fun `unknown fields from newer versions are ignored`() {
        val jsonWithExtra = """
            {
                "clearWidthMm": 2000.0,
                "clearHeightMm": 2200.0,
                "clearDepthMm": 500.0,
                "futureProperty": "ignoredValue"
            }
        """.trimIndent()

        val room = ProjectCodec.decode(jsonWithExtra)
        assertEquals(2000f, room.clearWidthMm)
        assertEquals(2200f, room.clearHeightMm)
        assertEquals(500f, room.clearDepthMm)
    }

    @Test
    fun `saved projects list round trips with all room measurements and locks`() {
        val customRoom = RoomSpec(
            clearWidthMm = 3325f,
            clearDepthMm = 4177f,
            wallBMm = 3769f,
            angleACDeg = 90f,
            angleABDeg = 90f,
            lockWallA = true,
            lockWallC = true,
            lockSegmentD = true,
            door = DoorSpec(widthMm = 660f, offsetMm = 1345f, lockWidth = true),
        )
        val list = listOf(
            SavedProject(
                id = "proj-1",
                name = "Vestidor Principal",
                updatedAtEpochMs = 1793420720000L,
                room = customRoom,
            ),
        )
        val encoded = ProjectCodec.encodeProjects(list)
        val decoded = ProjectCodec.decodeProjects(encoded)
        assertEquals(1, decoded.size)
        assertEquals("Vestidor Principal", decoded[0].name)
        assertEquals(customRoom, decoded[0].room)
    }
}
