package com.closetstudio.core.scene

import com.closetstudio.core.assertMm
import com.closetstudio.core.geometry.Surface
import com.closetstudio.core.model.ClosetProject
import com.closetstudio.core.model.ModuleKind
import com.closetstudio.core.model.ModuleSpec
import com.closetstudio.core.model.RoomSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClosetAssemblyTest {

    private fun project() = ClosetProject(
        room = RoomSpec(clearWidthMm = 2400f, clearHeightMm = 2400f, clearDepthMm = 600f),
        modules = listOf(
            ModuleSpec(id = "a", widthMm = 900f, heightMm = 2400f, shelfCount = 4),
            ModuleSpec(id = "b", widthMm = 900f, heightMm = 2400f, shelfCount = 2, hasHangingRod = true),
            ModuleSpec(id = "f", kind = ModuleKind.FILLER, widthMm = 100f, heightMm = 2400f),
        ),
    )

    @Test
    fun `an empty project still produces the cabinet envelope`() {
        val assembly = ClosetAssembly.build(ClosetProject())

        assertTrue(assembly.parts.any { it.surface == Surface.CARCASS })
        assertTrue(assembly.parts.any { it.surface == Surface.BACK })
        assertTrue(assembly.layout.placements.isEmpty())
    }

    @Test
    fun `solved modules contribute interior geometry`() {
        val assembly = ClosetAssembly.build(project())

        // Two nodes share the carcass material: the shell and the interior.
        assertEquals(2, assembly.parts.count { it.surface == Surface.CARCASS })
        assertNotNull(assembly.parts.last().mesh)
    }

    @Test
    fun `the interior stays inside the cabinet envelope`() {
        val assembly = ClosetAssembly.build(project())
        val interior = assembly.parts.last().mesh.bounds()

        assertMm(118f, interior.min.y)
        assertTrue(interior.min.y >= 0f, "interior extends below the floor: ${interior.min.y}")
        assertTrue(interior.max.x <= 2400f + 0.01f, "interior overflows the run: ${interior.max.x}")
        assertTrue(interior.max.y <= 2400f + 0.01f, "interior exceeds the height: ${interior.max.y}")
    }

    @Test
    fun `the layout leaves no gap because the filler absorbs it`() {
        val assembly = ClosetAssembly.build(project())

        assertTrue(assembly.layout.placements.any { it.id == "f" })
        assertMm(2400f, assembly.layout.usedWidthMm)
    }

    @Test
    fun `building twice is stable`() {
        val first = ClosetAssembly.build(project())
        val second = ClosetAssembly.build(project())

        assertEquals(first.layout.placements, second.layout.placements)
        assertEquals(first.parts.size, second.parts.size)
        assertEquals(first.parts.first().mesh.vertexCount, second.parts.first().mesh.vertexCount)
    }
}
