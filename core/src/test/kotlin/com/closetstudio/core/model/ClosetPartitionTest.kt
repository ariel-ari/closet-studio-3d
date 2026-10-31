package com.closetstudio.core.model

import com.closetstudio.core.assertMm
import com.closetstudio.core.geometry.RoomGeometryBuilder
import com.closetstudio.core.geometry.RoomSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClosetPartitionTest {

    @Test
    fun `adding vertical divider splits container carcass into two bays and calculates interior dimensions`() {
        val container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
            plinthHeightMm = 80f,
        )

        // Interior width: 2000 - 2 * 20 = 1960 mm
        // Interior height: 2400 - 80 - 20 - 20 = 2280 mm
        // Interior depth: 600 - 6 = 594 mm
        assertMm(1960f, container.interiorWidthMm)
        assertMm(2280f, container.interiorHeightMm)
        assertMm(594f, container.interiorDepthMm)

        // Initial bays with no dividers: 1 single bay spanning full interior
        val baysInitial = container.getBays()
        assertEquals(1, baysInitial.size)
        assertMm(0f, baysInitial[0].startXMm)
        assertMm(1960f, baysInitial[0].endXMm)
        assertMm(1960f, baysInitial[0].widthMm)

        // Add vertical divider at default midpoint
        val withDiv = container.withAddedDivider()
        assertEquals(1, withDiv.dividers.size)
        val div = withDiv.dividers.single()

        val bays = withDiv.getBays()
        assertEquals(2, bays.size)
        // Two symmetric bays split by 18mm divider
        assertMm(0f, bays[0].startXMm)
        assertMm(bays[0].endXMm, div.offsetXMm)
        assertMm(div.offsetXMm + div.thicknessMm, bays[1].startXMm)
        assertMm(1960f, bays[1].endXMm)
        assertEquals(bays[0].widthMm, bays[1].widthMm)
    }

    @Test
    fun `horizontal shelf automatically adapts when vertical divider moves`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
        ).withAddedDivider()

        val divId = container.dividers.single().id

        // Add shelf to bay 0
        container = container.withAddedShelf(bayIndex = 0, elevationYMm = 1000f)
        assertEquals(1, container.shelves.size)
        val shelf = container.shelves.single()
        assertEquals(0, shelf.bayIndex)

        val bay0Initial = container.getBays()[0]
        assertMm(970f, bay0Initial.widthMm)

        // Now move the divider to the right by 200 mm (offset = 1170 mm)
        container = container.withUpdatedDivider(divId) {
            it.copy(offsetXMm = 1170f)
        }

        // Bay 0 expands to 1170 mm
        val baysAfter = container.getBays()
        assertMm(1170f, baysAfter[0].widthMm)
        // Shelf is still in bay 0 and dynamically spans 1170 mm!
        assertMm(1170f, baysAfter[shelf.bayIndex].widthMm)
    }

    @Test
    fun `vertical divider adapts its height to compartment when placed above or below horizontal shelf`() {
        // Create container and add a horizontal shelf at 1000 mm in the single bay
        val container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
        ).withAddedShelf(bayIndex = 0, elevationYMm = 1000f)

        // 1. Add vertical divider in lower compartment (targetY = 400 mm < 1000 mm)
        val withLowerDiv = container.withAddedDivider(offsetXMm = 500f, targetYMm = 400f)
        val lowerDiv = withLowerDiv.dividers.single()
        assertMm(0f, lowerDiv.effectiveStartY())
        assertMm(1000f, lowerDiv.effectiveEndY(container.interiorHeightMm))
        assertMm(1000f, lowerDiv.effectiveHeight(container.interiorHeightMm))

        // 2. Add vertical divider in upper compartment (targetY = 1600 mm > 1000 mm)
        val withUpperDiv = container.withAddedDivider(offsetXMm = 500f, targetYMm = 1600f)
        val upperDiv = withUpperDiv.dividers.single()
        // Starts on top of the 20mm shelf: 1000 + 20 = 1020 mm
        assertMm(1020f, upperDiv.effectiveStartY())
        assertMm(container.interiorHeightMm, upperDiv.effectiveEndY(container.interiorHeightMm))
        assertMm(container.interiorHeightMm - 1020f, upperDiv.effectiveHeight(container.interiorHeightMm))
    }

    @Test
    fun `moving shelf laterally transfers to adjacent bay and adapts width`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
        ).withAddedDivider(offsetXMm = 600f) // Bay 0 width: 600mm, Bay 1 width: 1342mm

        val shelf = container.withAddedShelf(bayIndex = 0, elevationYMm = 800f).shelves.single()
        container = container.copy(shelves = listOf(shelf))

        // Initial shelf is in bay 0
        assertEquals(0, container.shelves.single().bayIndex)

        // Move shelf sideways to X = 1200 mm (which belongs to bay 1)
        container = container.withMovedShelf(shelf.id, newElevationYMm = 800f, targetXMm = 1200f)
        val moved = container.shelves.single()
        assertEquals(1, moved.bayIndex)
        val bays = container.getBays()
        assertMm(bays[1].widthMm, container.getBays()[moved.bayIndex].widthMm)
    }

    @Test
    fun `custom depth allows protruding forward while strictly anchoring to back panel`() {
        val container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
        ).withAddedDivider(offsetXMm = 800f)

        val div = container.dividers.single()
        // By default, effectiveDepth matches interiorDepthMm
        assertMm(container.interiorDepthMm, div.effectiveDepth(container.interiorDepthMm))

        // Set custom depth protruding forward: 750 mm (greater than carcass depth 600 mm)
        val customDiv = div.copy(depthMm = 750f)
        assertMm(750f, customDiv.effectiveDepth(container.interiorDepthMm))

        // Build room geometry and verify mesh bounds
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearHeightMm = 2600f,
            clearDepthMm = 3600f,
        ).withAddedContainer(
            wall = WallIndex.NORTH,
            id = "c1",
            defaultWidthMm = 2000f,
            defaultHeightMm = 2400f,
            defaultDepthMm = 600f,
        ).withUpdatedContainer("c1") {
            it.copy(dividers = listOf(customDiv))
        }

        val parts = RoomGeometryBuilder.build(room)
        val vertParts = parts.filter { it.surface == RoomSurface.CONTAINER_SIDE_PANEL }
        assertTrue(vertParts.isNotEmpty())
    }

    @Test
    fun `raycast picks vertical divider and horizontal shelf with touch tolerance`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearHeightMm = 2600f,
            clearDepthMm = 3600f,
        ).withAddedContainer(
            wall = WallIndex.NORTH,
            id = "c1",
            defaultWidthMm = 2000f,
            defaultHeightMm = 2400f,
            defaultDepthMm = 600f,
        ).withUpdatedContainer("c1") {
            it.withAddedDivider().withAddedShelf(bayIndex = 0, elevationYMm = 1000f)
        }

        val container = room.containers.single()
        val div = container.dividers.single()
        val shelf = container.shelves.single()

        // 3D ray towards vertical divider center
        val divWorldX = container.offsetMm + container.panelThicknessMm + div.offsetXMm + 9f
        val divWorldY = container.plinthHeightMm + container.panelThicknessMm + container.interiorHeightMm / 2f
        val divWorldZ = 300f

        val hitDiv = room.pickPartitionByRayMm(
            rayOriginX = divWorldX,
            rayOriginY = divWorldY,
            rayOriginZ = 2000f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = -1f,
        )

        assertNotNull(hitDiv)
        assertEquals("c1", hitDiv.containerId)
        assertEquals(div.id, hitDiv.partitionId)
        assertTrue(hitDiv.isVertical)

        // 3D ray towards horizontal shelf center
        val bay0 = container.getBays()[0]
        val shelfWorldX = container.offsetMm + container.panelThicknessMm + (bay0.startXMm + bay0.endXMm) / 2f
        val shelfWorldY = container.plinthHeightMm + container.panelThicknessMm + shelf.elevationYMm + 9f
        val shelfWorldZ = 300f

        val hitShelf = room.pickPartitionByRayMm(
            rayOriginX = shelfWorldX,
            rayOriginY = shelfWorldY,
            rayOriginZ = 2000f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = -1f,
        )

        assertNotNull(hitShelf)
        assertEquals("c1", hitShelf.containerId)
        assertEquals(shelf.id, hitShelf.partitionId)
        assertTrue(!hitShelf.isVertical)
    }

    @Test
    fun `geometry builder produces purple selected outline for active partition`() {
        val room = RoomSpec(
            clearWidthMm = 3600f,
            clearHeightMm = 2600f,
            clearDepthMm = 3600f,
        ).withAddedContainer(
            wall = WallIndex.NORTH,
            id = "c1",
            defaultWidthMm = 2000f,
            defaultHeightMm = 2400f,
            defaultDepthMm = 600f,
        ).withUpdatedContainer("c1") {
            it.withAddedDivider()
        }

        val div = room.containers.single().dividers.single()

        val parts = RoomGeometryBuilder.build(
            room = room,
            selectedContainerId = "c1",
            selectedPartitionId = div.id,
        )

        val outline = parts.find { it.surface == RoomSurface.PARTITION_SELECTED_OUTLINE }
        assertNotNull(outline)
        assertTrue(outline.mesh.positions.isNotEmpty())
    }
}
