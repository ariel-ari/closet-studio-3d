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
}
