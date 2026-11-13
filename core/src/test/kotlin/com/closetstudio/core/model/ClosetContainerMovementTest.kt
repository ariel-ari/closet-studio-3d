package com.closetstudio.core.model

import com.closetstudio.core.assertMm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ClosetContainerMovementTest {
    @Test
    fun `moving container preserves dimensions and attached components`() {
        val room = RoomSpec().withAddedContainer(WallIndex.NORTH, id = "moving", defaultWidthMm = 800f)
            .withUpdatedContainer("moving") { it.copy(offsetMm = 100f, shelves = listOf(HorizontalShelfSpec(id = "shelf"))) }

        val moved = room.withMovedContainer("moving", 500f).containers.single()

        assertMm(500f, moved.offsetMm)
        assertMm(800f, moved.widthMm)
        assertEquals(listOf("shelf"), moved.shelves.map { it.id })
    }

    @Test
    fun `moving fused container detaches but retains size and leaves neighbor unchanged`() {
        val base = RoomSpec().withAddedContainer(WallIndex.NORTH, id = "moving", defaultWidthMm = 1000f)
            .withAddedContainer(WallIndex.NORTH, id = "neighbor", defaultWidthMm = 800f)
        val room = base.copy(containers = base.containers.map {
            when (it.id) {
                "moving" -> it.copy(offsetMm = 0f, widthMm = 1200f, fuseStartCorner = true)
                else -> it.copy(offsetMm = 1800f)
            }
        })
        val neighborBefore = room.containers.first { it.id == "neighbor" }

        val moved = room.withMovedContainer("moving", 500f)
        val container = moved.containers.first { it.id == "moving" }

        assertMm(500f, container.offsetMm)
        assertMm(1200f, container.widthMm)
        assertFalse(container.fuseStartCorner)
        assertFalse(container.fuseEndCorner)
        assertEquals(neighborBefore, moved.containers.first { it.id == "neighbor" })
    }

    @Test
    fun `moving container stops before same-wall neighbor without shrinking`() {
        val base = RoomSpec().withAddedContainer(WallIndex.NORTH, id = "moving", defaultWidthMm = 800f)
            .withAddedContainer(WallIndex.NORTH, id = "neighbor", defaultWidthMm = 800f)
        val room = base.copy(containers = base.containers.map {
            when (it.id) {
                "moving" -> it.copy(offsetMm = 100f)
                else -> it.copy(offsetMm = 1500f)
            }
        })

        val moved = room.withMovedContainer("moving", 2000f).containers.first { it.id == "moving" }

        assertMm(700f, moved.offsetMm)
        assertMm(800f, moved.widthMm)
    }
}
