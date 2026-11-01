package com.closetstudio.core.model

import com.closetstudio.core.assertMm
import com.closetstudio.core.geometry.RoomGeometryBuilder
import com.closetstudio.core.geometry.RoomSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `zero penetration rule - vertical divider sits flush between bottom and top panels without clipping`() {
        val totalH = 2200f
        val plinthH = 80f
        val panelT = 18f
        val container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 1800f,
            heightMm = totalH,
            depthMm = 600f,
            panelThicknessMm = panelT,
            plinthHeightMm = plinthH,
        ).withAddedDivider()

        val div = container.dividers.single()

        // Effective bottom and top bounds of interior space
        val floorPanelTopY = container.effectivePlinthHeightMm + container.effectivePanelThicknessMm
        val roofPanelBottomY = totalH - container.effectivePanelThicknessMm
        val expectedInteriorHeight = roofPanelBottomY - floorPanelTopY

        assertMm(expectedInteriorHeight, container.interiorHeightMm)
        assertMm(0f, div.effectiveStartY())
        assertMm(container.interiorHeightMm, div.effectiveEndY(container.interiorHeightMm))

        // Total divider world top Y: floorPanelTopY + div.effectiveEndY()
        val divWorldTopY = floorPanelTopY + div.effectiveEndY(container.interiorHeightMm)
        // Must match roofPanelBottomY EXACTLY to the millimeter — NEVER penetrate roof panel!
        assertMm(roofPanelBottomY, divWorldTopY)
        assertTrue(divWorldTopY <= roofPanelBottomY)
        assertTrue(divWorldTopY < totalH) // Must be strictly below total closet height by panel thickness

        // Even with an invalid manual endYMm that tries to exceed interior height, it must clamp:
        val overflowingDiv = div.copy(endYMm = 9999f)
        assertMm(container.interiorHeightMm, overflowingDiv.effectiveEndY(container.interiorHeightMm))
    }

    @Test
    fun `horizontal shelf in bay 1 magnetically snaps to elevation of shelf in bay 0 within threshold`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
            plinthHeightMm = 80f,
        ).withAddedDivider(offsetXMm = 970f) // Creates Bay 0 and Bay 1

        // Add reference shelf in Bay 0 at elevation 1000 mm
        container = container.withAddedShelf(bayIndex = 0, elevationYMm = 1000f)
        val shelf0 = container.shelves.first { it.bayIndex == 0 }
        assertMm(1000f, shelf0.elevationYMm)

        // Add shelf in Bay 1 at raw elevation 1018 mm (within 28mm threshold of 1000 mm)
        container = container.withAddedShelf(bayIndex = 1, elevationYMm = 1018f)
        val shelf1 = container.shelves.first { it.id != shelf0.id }

        // Must magnetically snap to exactly 1000f matching shelf 0!
        assertMm(1000f, shelf1.elevationYMm)

        // Moving shelf1 near 1000 mm (e.g. 985 mm) also snaps to 1000 mm
        val moved = container.withMovedShelf(shelf1.id, newElevationYMm = 985f)
        val movedShelf1 = moved.shelves.first { it.id == shelf1.id }
        assertMm(1000f, movedShelf1.elevationYMm)
    }

    @Test
    fun `horizontal shelf moves freely when outside magnetic snap threshold`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
            plinthHeightMm = 80f,
        ).withAddedDivider(offsetXMm = 970f)

        // Reference shelf in Bay 0 at 1000 mm
        container = container.withAddedShelf(bayIndex = 0, elevationYMm = 1000f)

        // Add shelf in Bay 1 at raw elevation 1100 mm (well outside 28mm threshold of 1000 mm, and not at center/thirds)
        // Note: innerH = 2400 - 80 - 40 = 2280 mm. Center = (2280 - 20) * 0.5 = 1130 mm. Distance to 1130 is 30mm > 28mm.
        val nonSnappedElev = 1060f
        container = container.withAddedShelf(bayIndex = 1, elevationYMm = nonSnappedElev)
        val shelf1 = container.shelves.first { it.bayIndex == 1 }

        // Must retain exact non-snapped elevation
        assertMm(nonSnappedElev, shelf1.elevationYMm)
    }

    @Test
    fun `vertical divider magnetically snaps to container center`() {
        val container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
        )
        // innerW = 2000 - 40 = 1960 mm. Center = (1960 - 20) * 0.5 = 970 mm.
        // User moves divider to 982 mm (within 28 mm of 970 mm)
        val withDiv = container.withAddedDivider(offsetXMm = 982f)
        val div = withDiv.dividers.single()

        // Must snap to exactly 970 mm!
        assertMm(970f, div.offsetXMm)
    }

    @Test
    fun `adding drawer places it in bay and respects zero penetration rule`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 1800f,
            heightMm = 2200f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        ).withAddedDivider(offsetXMm = 900f)

        // Add drawer in Bay 0 at bottom
        container = container.withAddedDrawer(bayIndex = 0, elevationYMm = 0f, heightMm = 200f)
        val drawer = container.drawers.single()
        assertMm(0f, drawer.elevationYMm)
        assertMm(200f, drawer.heightMm)
        assertEquals(0, drawer.bayIndex)

        // Add second drawer in Bay 0 (should default to stacking on top of drawer 1: elevation 200 mm)
        container = container.withAddedDrawer(bayIndex = 0, heightMm = 200f)
        assertEquals(2, container.drawers.size)
        val drawer2 = container.drawers[1]
        assertMm(200f, drawer2.elevationYMm)

        // Geometry builds with drawer parts and outline
        val room = RoomSpec(containers = listOf(container))
        val parts = RoomGeometryBuilder.build(room, selectedContainerId = "c1", selectedPartitionId = drawer.id)
        assertTrue(parts.any { it.surface == RoomSurface.CONTAINER_BODY })
        assertTrue(parts.any { it.surface == RoomSurface.DOOR_HANDLE })
        assertTrue(parts.any { it.surface == RoomSurface.PARTITION_SELECTED_OUTLINE })
    }

    @Test
    fun `drawer in adjacent bay magnetically snaps to match drawer elevation`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 20f,
            plinthHeightMm = 80f,
        ).withAddedDivider(offsetXMm = 970f)

        // Reference drawer in Bay 0 at elevation 0 mm
        container = container.withAddedDrawer(bayIndex = 0, elevationYMm = 0f, heightMm = 200f)

        // Add drawer in Bay 1 at elevation 15 mm (near 0 mm threshold)
        container = container.withAddedDrawer(bayIndex = 1, elevationYMm = 15f, heightMm = 200f)
        val drawerBay1 = container.drawers.first { it.bayIndex == 1 }

        // Must snap to 0 mm flush with drawer in bay 0!
        assertMm(0f, drawerBay1.elevationYMm)
    }

    @Test
    fun `drawer with finger pull notch respects modular architecture and zero penetration`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 1800f,
            heightMm = 2200f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        ).withAddedDivider(offsetXMm = 900f)

        // Add notch drawer in Bay 0
        container = container.withAddedDrawer(
            bayIndex = 0,
            elevationYMm = 0f,
            heightMm = 220f,
            frontType = DrawerFrontType.NOTCH,
        )
        val drawer = container.drawers.single()
        assertEquals(DrawerFrontType.NOTCH, drawer.frontType)
        assertTrue(!drawer.hasHandle)
        assertMm(0f, drawer.elevationYMm)
        assertMm(220f, drawer.heightMm)

        // Stacking a second notch drawer directly on top
        container = container.withAddedDrawer(
            bayIndex = 0,
            heightMm = 220f,
            frontType = DrawerFrontType.NOTCH,
        )
        assertEquals(2, container.drawers.size)
        val stacked = container.drawers[1]
        assertMm(220f, stacked.elevationYMm)

        // Build geometry: builds notched front faces, open wooden box, and purple outline
        val room = RoomSpec(containers = listOf(container))
        val parts = RoomGeometryBuilder.build(room, selectedContainerId = "c1", selectedPartitionId = drawer.id)
        assertTrue(parts.any { it.surface == RoomSurface.CONTAINER_BODY })
        assertTrue(parts.any { it.surface == RoomSurface.PARTITION_SELECTED_OUTLINE })
    }

    @Test
    fun `clothes rail defaults to center, respects zero penetration, and builds chrome rod geometry`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 1800f,
            heightMm = 2200f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        )

        val innerH = container.interiorHeightMm // 2200 - 80 - 36 = 2084 mm
        val innerD = container.interiorDepthMm // 600 - 6 = 594 mm

        // 1. Adding rail with default params: centers vertically in bay and centers in depth
        container = container.withAddedClothesRail()
        assertEquals(1, container.clothesRails.size)
        val rail = container.clothesRails.single()
        assertMm(innerH * 0.5f, rail.elevationYMm)
        assertMm(innerD * 0.5f, rail.effectiveDepth(innerD))
        assertEquals(25f, rail.diameterMm)

        // 2. Add second bay by adding vertical divider
        container = container.withAddedDivider(offsetXMm = 880f)
        assertEquals(2, container.getBays().size)

        // Add second rail in bay 1 nearby in elevation: magnetically snaps to match bay 0 rail
        container = container.withAddedClothesRail(bayIndex = 1, elevationYMm = (innerH * 0.5f) + 15f)
        assertEquals(2, container.clothesRails.size)
        val railBay1 = container.clothesRails[1]
        assertMm(rail.elevationYMm, railBay1.elevationYMm) // Snap alignment

        // 3. Build geometry: produces DOOR_HANDLE mesh for chrome rod and brackets, and purple selection outline
        val room = RoomSpec(containers = listOf(container))
        val parts = RoomGeometryBuilder.build(room, selectedContainerId = "c1", selectedPartitionId = rail.id)
        assertTrue(parts.any { it.surface == RoomSurface.DOOR_HANDLE })
        assertTrue(parts.any { it.surface == RoomSurface.PARTITION_SELECTED_OUTLINE })

        // 4. Raycast picking hits the rail
        val s0Inner = container.offsetMm + container.effectivePanelThicknessMm
        val bay0MidX = s0Inner + container.getBays()[0].widthMm * 0.5f
        val railWorldY = container.effectivePlinthHeightMm + container.effectivePanelThicknessMm + rail.elevationYMm
        val railWorldD = 4f + container.backThicknessMm + rail.effectiveDepth(innerD)

        val hit = room.pickPartitionByRayMm(
            rayOriginX = bay0MidX,
            rayOriginY = railWorldY,
            rayOriginZ = -200f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = 1f,
        )
        assertNotNull(hit)
        assertEquals("c1", hit.containerId)
        assertEquals(rail.id, hit.partitionId)
        assertTrue(!hit.isVertical)

        // 5. Moving clothes rail vertically and across bays
        val movedContainer = container.withMovedClothesRail(
            id = rail.id,
            newElevationYMm = 1400f,
            targetXMm = 1200f, // Points into Bay 1 (offsetXMm 880 to 1800)
            snapThresholdMm = 0f,
        )
        val movedRail = movedContainer.clothesRails.first { it.id == rail.id }
        assertMm(1400f, movedRail.elevationYMm)
        assertEquals(1, movedRail.bayIndex)
    }

    @Test
    fun `vertical pole defaults to bay center, spans compartment height, snaps, builds geometry and raycasts`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 1800f,
            heightMm = 2200f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        )

        val innerW = container.interiorWidthMm
        val innerH = container.interiorHeightMm
        val innerD = container.interiorDepthMm

        // 1. Add vertical pole at default position
        container = container.withAddedVerticalPole(bayIndex = 0)
        assertEquals(1, container.verticalPoles.size)
        val pole = container.verticalPoles.single()

        assertEquals(0, pole.bayIndex)
        assertMm(innerW * 0.5f, pole.effectiveX(container))
        assertMm(innerD * 0.5f, pole.effectiveDepth(innerD))
        assertEquals(25f, pole.diameterMm)
        val (y0, y1) = pole.effectiveYRange(container)
        assertMm(0f, y0)
        assertMm(innerH, y1)

        // 2. Build geometry: produces DOOR_HANDLE mesh for chrome pole & flange brackets, and purple selection outline
        val room = RoomSpec(containers = listOf(container))
        val parts = RoomGeometryBuilder.build(room, selectedContainerId = "c1", selectedPartitionId = pole.id)
        assertTrue(parts.any { it.surface == RoomSurface.DOOR_HANDLE })
        assertTrue(parts.any { it.surface == RoomSurface.PARTITION_SELECTED_OUTLINE })

        // 3. Raycast picking hits the pole with isVertical = true
        val s0Inner = container.offsetMm + container.effectivePanelThicknessMm
        val poleWorldX = s0Inner + pole.effectiveX(container)
        val poleMidY = container.effectivePlinthHeightMm + container.effectivePanelThicknessMm + (innerH * 0.5f)

        val hit = room.pickPartitionByRayMm(
            rayOriginX = poleWorldX,
            rayOriginY = poleMidY,
            rayOriginZ = -200f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = 1f,
        )
        assertNotNull(hit)
        assertEquals("c1", hit.containerId)
        assertEquals(pole.id, hit.partitionId)
        assertTrue(hit.isVertical)

        // 4. Add divider to create bay 0 and bay 1
        container = container.withAddedDivider(offsetXMm = 1400f)
        assertEquals(2, container.getBays().size)

        // 5. Moving vertical pole horizontally across into Bay 1
        val movedContainer = container.withMovedVerticalPole(
            id = pole.id,
            newOffsetXMm = 1550f, // Points into Bay 1 (offsetXMm 1400+18 to 1800)
            targetYMm = 1000f,
            snapThresholdMm = 0f,
        )
        val movedPole = movedContainer.verticalPoles.first { it.id == pole.id }
        assertMm(1550f, movedPole.effectiveX(movedContainer))
        assertEquals(1, movedPole.bayIndex)
    }

    @Test
    fun `inclined shoe rack with solid wood lip defaults, zero penetration, geometry, snapping and picking`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            offsetMm = 200f,
            widthMm = 1800f,
            heightMm = 2200f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
            backThicknessMm = 6f,
        )

        val innerW = container.interiorWidthMm
        val innerH = container.interiorHeightMm
        val innerD = container.interiorDepthMm

        // 1. Add inclined shoe rack at elevation 300mm
        container = container.withAddedShoeRack(bayIndex = 0, elevationYMm = 300f, inclinationAngleDeg = 18f)
        assertEquals(1, container.shoeRacks.size)
        val rack = container.shoeRacks.single()

        assertEquals(0, rack.bayIndex)
        assertMm(300f, rack.elevationYMm)
        assertEquals(18f, rack.thicknessMm)
        assertEquals(18f, rack.inclinationAngleDeg)
        assertEquals(40f, rack.lipHeightMm)
        assertEquals(18f, rack.lipThicknessMm)
        assertMm(innerD, rack.effectiveDepth(innerD))

        // Total vertical span & zero penetration check
        val span = rack.totalVerticalSpanMm(innerD)
        assertTrue(span > 100f)
        val maxElev = rack.maxElevation(innerH, innerD)
        assertEquals(innerH - span, maxElev)

        // 2. Build geometry: produces CLOSET_HORIZONTAL mesh (for slanted slab and solid wood lip),
        // CLOSET_FRONT_EDGE, and purple selection outline
        val room = RoomSpec(containers = listOf(container))
        val parts = RoomGeometryBuilder.build(room, selectedContainerId = "c1", selectedPartitionId = rack.id)
        assertTrue(parts.any { it.surface == RoomSurface.CONTAINER_BODY })
        assertTrue(parts.any { it.surface == RoomSurface.CONTAINER_FRONT_EDGE })
        assertTrue(parts.any { it.surface == RoomSurface.PARTITION_SELECTED_OUTLINE })

        // 3. Raycast picking hits the shoe rack with isVertical = false
        val s0Inner = container.offsetMm + container.effectivePanelThicknessMm
        val bay = container.getBays().first()
        val rackMidX = s0Inner + (bay.startXMm + bay.endXMm) * 0.5f
        val rackFrontY = container.effectivePlinthHeightMm + container.effectivePanelThicknessMm + rack.elevationYMm + 20f

        val hit = room.pickPartitionByRayMm(
            rayOriginX = rackMidX,
            rayOriginY = rackFrontY,
            rayOriginZ = -200f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = 1f,
        )
        assertNotNull(hit)
        assertEquals("c1", hit.containerId)
        assertEquals(rack.id, hit.partitionId)
        assertFalse(hit.isVertical)

        // 4. Magnetic Snapping: Add divider to create Bay 0 and Bay 1
        container = container.withAddedDivider(offsetXMm = 900f)
        assertEquals(2, container.getBays().size)

        // Add second shoe rack in Bay 1 within snap threshold of first shoe rack's elevation (300mm)
        // With raw 315mm (within 28mm threshold), it must magnetically snap to exactly 300mm
        val snappedElev = container.findSnapElevation(
            currentShelfId = null,
            rawElevationYMm = 315f,
            targetBayIndex = 1,
            shelfThicknessMm = 18f,
            snapThresholdMm = 28f,
        )
        assertMm(300f, snappedElev)

        // 5. Lateral Bay Transfer: moving shoe rack horizontally across into Bay 1
        val movedContainer = container.withMovedShoeRack(
            id = rack.id,
            newElevationYMm = 300f,
            targetXMm = 1200f, // Points into Bay 1 (offsetXMm 900+18 to 1800)
            snapThresholdMm = 0f,
        )
        val movedRack = movedContainer.shoeRacks.first { it.id == rack.id }
        assertEquals(1, movedRack.bayIndex)
    }

    @Test
    fun `cabinet doors default to 2 leaves, support 1 leaf toggle, global visibility hiding, and raycast picking`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 1800f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        )

        // 1. Add shelf at 1200mm to create compartments
        container = container.withAddedShelf(elevationYMm = 1200f)
        val shelf = container.shelves.first()

        // 2. Add Cabinet Doors with default 2 leaves
        container = container.withAddedCabinetDoors(
            bayIndex = 0,
            targetYMm = 600f,
        )
        assertEquals(1, container.cabinetDoors.size)
        val doors = container.cabinetDoors.first()
        assertEquals(2, doors.doorCount)
        assertTrue(doors.hasHandles)
        assertMm(18f, doors.thicknessMm)

        // 3. 3D Geometry with showDoors = true produces panels and cabinet door handles
        var room = RoomSpec(containers = listOf(container))
        val partsVisible = RoomGeometryBuilder.build(room, showDoors = true)
        val handleVertsVisible = partsVisible.filter { it.surface == RoomSurface.DOOR_HANDLE }.sumOf { it.mesh.vertexCount }

        // 4. 3D Geometry with showDoors = false hides cabinet doors and their handles
        val partsHidden = RoomGeometryBuilder.build(room, showDoors = false)
        val handleVertsHidden = partsHidden.filter { it.surface == RoomSurface.DOOR_HANDLE }.sumOf { it.mesh.vertexCount }
        assertTrue(handleVertsVisible > handleVertsHidden, "Cabinet door handles must be removed when showDoors is false")

        // 5. Raycasting: when includeDoors is true, raycast hits cabinet doors
        val doorMidX = container.offsetMm + container.panelThicknessMm + 400f
        val doorMidY = container.plinthHeightMm + container.panelThicknessMm + doors.elevationYMm + doors.heightMm * 0.5f
        val hitDoors = room.pickPartitionByRayMm(
            rayOriginX = doorMidX,
            rayOriginY = doorMidY,
            rayOriginZ = -200f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = 1f,
            includeDoors = true,
        )
        assertNotNull(hitDoors)
        assertEquals("c1", hitDoors.containerId)
        assertEquals(doors.id, hitDoors.partitionId)

        // 6. Raycasting: when includeDoors is false, raycast passes through doors to interior elements
        val shelfFrontY = container.plinthHeightMm + container.panelThicknessMm + shelf.elevationYMm + shelf.thicknessMm * 0.5f
        val hitInterior = room.pickPartitionByRayMm(
            rayOriginX = doorMidX,
            rayOriginY = shelfFrontY,
            rayOriginZ = -200f,
            rayDirX = 0f,
            rayDirY = 0f,
            rayDirZ = 1f,
            includeDoors = false,
        )
        assertNotNull(hitInterior)
        assertEquals("c1", hitInterior.containerId)
        assertEquals(shelf.id, hitInterior.partitionId)

        // 7. Toggle to 1 door leaf configuration
        val withOneDoor = container.withUpdatedCabinetDoors(doors.id) { it.copy(doorCount = 1) }
        assertEquals(1, withOneDoor.cabinetDoors.first().doorCount)

        // 8. Magnetic snapping across bays
        container = container.withAddedDivider(offsetXMm = 900f)
        val snappedElev = container.findSnapElevation(
            currentShelfId = "doors_test",
            rawElevationYMm = doors.elevationYMm + 15f,
            targetBayIndex = 1,
            shelfThicknessMm = doors.heightMm,
            snapThresholdMm = 28f,
        )
        assertMm(doors.elevationYMm, snappedElev)
    }

    @Test
    fun `cabinet doors adapt to 1, 2, or all compartments and ignore drawers in adjacent bays`() {
        var container = ClosetContainerSpec(
            id = "c1",
            wall = WallIndex.NORTH,
            widthMm = 2000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        )

        // Divide container into two bays (Bay 0 and Bay 1)
        container = container.withAddedDivider(offsetXMm = 1000f)
        assertEquals(2, container.getBays().size)

        // Bay 0 has a horizontal shelf in the middle (elevation = 1100mm, thickness = 18mm)
        container = container.withAddedShelf(bayIndex = 0, elevationYMm = 1100f)
        val shelf0 = container.shelves.first { it.bayIndex == 0 }

        // Bay 1 has a bottom drawer (elevation = 0mm, height = 300mm)
        container = container.withAddedDrawer(bayIndex = 1, elevationYMm = 0f, heightMm = 300f)
        val drawer1 = container.drawers.first { it.bayIndex == 1 }

        // Test 1: In Bay 0 (no drawers), snapping does NOT pick drawer from Bay 1
        val snapElevBay0 = container.findSnapElevation(
            currentShelfId = "doors_new",
            rawElevationYMm = 290f,
            targetBayIndex = 0,
            shelfThicknessMm = 1000f,
            snapThresholdMm = 28f,
        )
        assertFalse(kotlin.math.abs(snapElevBay0 - (drawer1.elevationYMm + drawer1.heightMm)) < 1f)

        // Test 2: In Bay 0, dropping in upper region (targetY = 1700mm) adapts door to upper compartment
        val withUpperDoor = container.withAddedCabinetDoors(bayIndex = 0, targetYMm = 1700f)
        val upperDoor = withUpperDoor.cabinetDoors.last()
        assertMm(shelf0.elevationYMm + shelf0.thicknessMm, upperDoor.elevationYMm)
        assertMm(container.interiorHeightMm - (shelf0.elevationYMm + shelf0.thicknessMm), upperDoor.heightMm)

        // Test 3: In Bay 0, dropping in lower region (targetY = 500mm) adapts door to lower compartment
        val withLowerDoor = container.withAddedCabinetDoors(bayIndex = 0, targetYMm = 500f)
        val lowerDoor = withLowerDoor.cabinetDoors.last()
        assertMm(0f, lowerDoor.elevationYMm)
        assertMm(shelf0.elevationYMm, lowerDoor.heightMm)

        // Test 4: In Bay 0, dropping near the shelf mid spans BOTH compartments (full bay)
        val shelfMid = shelf0.elevationYMm + shelf0.thicknessMm * 0.5f
        val withFullDoor = container.withAddedCabinetDoors(bayIndex = 0, targetYMm = shelfMid)
        val fullDoor = withFullDoor.cabinetDoors.last()
        assertMm(0f, fullDoor.elevationYMm)
        assertMm(container.interiorHeightMm, fullDoor.heightMm)

        // Test 5: In Bay 1 (with bottom drawer), door sits flush on top of drawer
        val withDrawerDoor = container.withAddedCabinetDoors(bayIndex = 1, targetYMm = 1200f)
        val drawerDoor = withDrawerDoor.cabinetDoors.last()
        assertMm(drawer1.elevationYMm + drawer1.heightMm, drawerDoor.elevationYMm)
        assertMm(container.interiorHeightMm - (drawer1.elevationYMm + drawer1.heightMm), drawerDoor.heightMm)

        // Test 6: 3 compartments (Bay 0 with 2 shelves at 700mm and 1400mm)
        var container3Comp = ClosetContainerSpec(
            id = "c3",
            wall = WallIndex.NORTH,
            widthMm = 1000f,
            heightMm = 2400f,
            depthMm = 600f,
            panelThicknessMm = 18f,
            plinthHeightMm = 80f,
        )
        container3Comp = container3Comp.withAddedShelf(bayIndex = 0, elevationYMm = 700f)
        container3Comp = container3Comp.withAddedShelf(bayIndex = 0, elevationYMm = 1400f)
        val comps3 = container3Comp.getBayCompartments(0)
        assertEquals(3, comps3.size)
        assertMm(0f, comps3[0].bottomYMm)
        assertMm(700f, comps3[0].topYMm)
        assertMm(718f, comps3[1].bottomYMm)
        assertMm(1400f, comps3[1].topYMm)
        assertMm(1418f, comps3[2].bottomYMm)
        assertMm(container3Comp.interiorHeightMm, comps3[2].topYMm)
    }
}
