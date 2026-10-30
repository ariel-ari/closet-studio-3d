package com.closetstudio.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectCodecTest {

    @Test
    fun `a project round trips through json`() {
        val project = ClosetProject(
            name = "Master bedroom",
            room = RoomSpec(clearWidthMm = 2600f, clearHeightMm = 2300f, clearDepthMm = 620f),
            modules = listOf(
                ModuleSpec(id = "m1", widthMm = 900f, heightMm = 2300f, shelfCount = 4),
                ModuleSpec(id = "m2", kind = ModuleKind.FILLER, widthMm = 120f, heightMm = 2300f),
            ),
        )

        val restored = ProjectCodec.decode(ProjectCodec.encode(project))

        assertEquals(project, restored)
    }

    @Test
    fun `unknown fields from a newer version are ignored`() {
        val text = """
            {
              "name": "Legacy",
              "room": { "clearWidthMm": 2000.0, "clearHeightMm": 2200.0, "clearDepthMm": 580.0 },
              "futureField": "ignore me"
            }
        """.trimIndent()

        val project = ProjectCodec.decode(text)

        assertEquals("Legacy", project.name)
        assertEquals(2000f, project.room.clearWidthMm)
        assertEquals(18f, project.panels.thicknessMm)
        assertEquals(emptyList(), project.modules)
    }

    @Test
    fun `defaults describe a buildable closet`() {
        val project = ClosetProject()

        assertEquals(1, project.schemaVersion)
        assertEquals(600f, project.finish.uvScaleMm)
    }
}
