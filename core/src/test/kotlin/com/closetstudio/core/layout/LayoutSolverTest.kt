package com.closetstudio.core.layout

import com.closetstudio.core.assertMm
import com.closetstudio.core.model.ModuleKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutSolverTest {

    private fun carcass(id: String, width: Float) = ModuleRequest(id, ModuleKind.CARCASS, width)
    private fun filler(id: String, width: Float) = ModuleRequest(id, ModuleKind.FILLER, width)

    @Test
    fun `fillers absorb the leftover so the run lands exactly on the wall`() {
        val result = LayoutSolver.solve(
            2400f,
            listOf(carcass("a", 800f), carcass("b", 800f), filler("f", 100f)),
        )

        val placedFiller = result.placements.single { it.id == "f" }
        assertMm(800f, placedFiller.widthMm)
        assertMm(2400f, result.usedWidthMm)
        assertMm(0f, result.remainingWidthMm)
        assertFalse(result.isOverflowing)
    }

    @Test
    fun `two fillers split the leftover in proportion to their request`() {
        val result = LayoutSolver.solve(
            2000f,
            listOf(carcass("a", 1200f), filler("left", 100f), filler("right", 300f)),
        )

        assertMm(200f, result.placements.single { it.id == "left" }.widthMm)
        assertMm(600f, result.placements.single { it.id == "right" }.widthMm)
        assertMm(2000f, result.usedWidthMm)
    }

    @Test
    fun `zero width fillers split evenly`() {
        val result = LayoutSolver.solve(
            1500f,
            listOf(carcass("a", 900f), filler("f1", 0f), filler("f2", 0f)),
        )

        assertMm(300f, result.placements.single { it.id == "f1" }.widthMm)
        assertMm(300f, result.placements.single { it.id == "f2" }.widthMm)
    }

    @Test
    fun `placements keep the requested order and never overlap`() {
        val result = LayoutSolver.solve(
            2400f,
            listOf(filler("f", 0f), carcass("a", 600f), carcass("b", 900f)),
        )

        assertEquals(listOf("f", "a", "b"), result.placements.map { it.id })
        var cursor = 0f
        for (placement in result.placements) {
            assertMm(cursor, placement.startXMm)
            cursor = placement.endXMm
        }
    }

    @Test
    fun `overflow is reported when carcasses do not fit`() {
        val result = LayoutSolver.solve(
            1000f,
            listOf(carcass("a", 700f), carcass("b", 700f), filler("f", 100f)),
        )

        assertMm(400f, result.overflowMm)
        assertTrue(result.isOverflowing)
        assertMm(0f, result.placements.single { it.id == "f" }.widthMm)
        assertTrue(result.squeezedFillers)
    }

    @Test
    fun `without fillers the leftover is reported as remaining`() {
        val result = LayoutSolver.solve(2400f, listOf(carcass("a", 800f), carcass("b", 800f)))

        assertMm(800f, result.remainingWidthMm)
        assertFalse(result.isOverflowing)
        assertEquals(2, result.placements.size)
    }

    @Test
    fun `solving twice gives identical results`() {
        val requests = listOf(carcass("a", 640f), filler("f", 50f), carcass("b", 900f))

        val first = LayoutSolver.solve(2000f, requests)
        val second = LayoutSolver.solve(2000f, requests)

        assertEquals(first.placements, second.placements)
    }

    @Test
    fun `an empty run places nothing`() {
        val result = LayoutSolver.solve(2400f, emptyList<ModuleRequest>())

        assertTrue(result.placements.isEmpty())
        assertMm(2400f, result.remainingWidthMm)
    }
}
