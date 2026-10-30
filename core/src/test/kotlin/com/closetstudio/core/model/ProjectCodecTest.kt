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
}
