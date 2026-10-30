package com.closetstudio.core.geometry

import com.closetstudio.core.assertMm
import com.closetstudio.core.model.PanelSpec
import com.closetstudio.core.model.RoomSpec
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClosetShellTest {

    private val room = RoomSpec(clearWidthMm = 2400f, clearHeightMm = 2400f, clearDepthMm = 600f)
    private val panels = PanelSpec(
        thicknessMm = 18f,
        backThicknessMm = 4f,
        toeKickHeightMm = 100f,
        toeKickInsetMm = 50f,
    )

    @Test
    fun `the carcass spans the full opening`() {
        val shell = ClosetShell.build(room, panels)
        val carcass = assertNotNull(shell.meshFor(Surface.CARCASS))
        val bounds = carcass.bounds()

        assertMm(0f, bounds.min.x)
        assertMm(2400f, bounds.max.x)
        assertMm(0f, bounds.min.y)
        assertMm(2400f, bounds.max.y)
        assertMm(0f, bounds.min.z)
        assertMm(600f, bounds.max.z)
    }

    @Test
    fun `sides top and bottom are separate boxes inside the carcass mesh`() {
        val shell = ClosetShell.build(room, panels)
        val carcass = assertNotNull(shell.meshFor(Surface.CARCASS))

        // Four panels, twenty four vertices each.
        assertTrue(carcass.vertexCount == 96, "expected 96 vertices, got ${carcass.vertexCount}")
    }

    @Test
    fun `the back panel is thin and flush with the wall`() {
        val shell = ClosetShell.build(room, panels)
        val back = assertNotNull(shell.meshFor(Surface.BACK))
        val bounds = back.bounds()

        assertMm(0f, bounds.min.z)
        assertMm(4f, bounds.max.z)
        assertMm(2400f, bounds.max.x)
    }

    @Test
    fun `the toe kick is recessed from the front face`() {
        val shell = ClosetShell.build(room, panels)
        val kick = assertNotNull(shell.meshFor(Surface.TOE_KICK))
        val bounds = kick.bounds()

        assertMm(100f, bounds.max.y)
        assertTrue(bounds.max.z < 600f, "toe kick should sit behind the front face, was ${bounds.max.z}")
    }

    @Test
    fun `a room without a toe kick omits the plinth`() {
        val shell = ClosetShell.build(room, panels.copy(toeKickHeightMm = 0f))

        assertNull(shell.meshFor(Surface.TOE_KICK))
    }

    @Test
    fun `the shell bounds frame the whole unit`() {
        val bounds = ClosetShell.build(room, panels).bounds()

        assertMm(0f, bounds.min.y)
        assertMm(2400f, bounds.max.y)
    }

    @Test
    fun `a room narrower than two panels is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ClosetShell.build(room.copy(clearWidthMm = 30f), panels)
        }
    }

    @Test
    fun `the tooth kick fits within the depth`() {
        val bounds = assertNotNull(ClosetShell.build(room, panels).meshFor(Surface.TOE_KICK)).bounds()

        assertTrue(bounds.min.z >= 0f, "toe kick starts behind the wall: ${bounds.min.z}")
    }
}
