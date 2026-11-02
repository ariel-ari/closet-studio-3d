package com.closetstudio.core.geometry

import com.closetstudio.core.assertMm
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomGeometryTest {

    @Test
    fun `generates all room surfaces including door`() {
        val room = RoomSpec(clearWidthMm = 3600f, clearHeightMm = 2600f, clearDepthMm = 3600f)
        val parts = RoomGeometryBuilder.build(room)

        assertNotNull(parts.find { it.surface == RoomSurface.FLOOR })
        assertNotNull(parts.find { it.surface == RoomSurface.WALL })
        assertNotNull(parts.find { it.surface == RoomSurface.DOOR_FRAME })
        assertNotNull(parts.find { it.surface == RoomSurface.DOOR_LEAF })
    }

    @Test
    fun `floor encloses bottom level`() {
        val room = RoomSpec(clearWidthMm = 3000f, clearHeightMm = 2500f, clearDepthMm = 3000f)
        val parts = RoomGeometryBuilder.build(room)

        val floor = assertNotNull(parts.find { it.surface == RoomSurface.FLOOR })
        assertMm(0f, floor.mesh.bounds().max.y)
    }

    @Test
    fun `door offset leaves wall before and after the opening`() {
        val room = RoomSpec(
            clearWidthMm = 4000f,
            clearHeightMm = 2600f,
            clearDepthMm = 4000f,
            door = DoorSpec(offsetMm = 1000f, widthMm = 900f, heightMm = 2100f),
        )
        val parts = RoomGeometryBuilder.build(room)
        val walls = assertNotNull(parts.find { it.surface == RoomSurface.WALL })

        val bounds = walls.mesh.bounds()
        // Wall spans full room opening
        assertTrue(bounds.min.x <= 0f)
        assertTrue(bounds.max.x >= 4000f)
        assertTrue(bounds.max.z >= 4000f)
    }

    @Test
    fun `closet container clamps to host wall, room height, and adjacent wall depth and builds 3D carcass`() {
        val room = RoomSpec(
            clearWidthMm = 2000f,
            clearHeightMm = 2100f,
            clearDepthMm = 500f,
        ).withAddedContainer(
            wall = com.closetstudio.core.model.WallIndex.NORTH,
            id = "c1",
            defaultWidthMm = 2500f,  // exceeds wall width 2000f
            defaultHeightMm = 2400f, // exceeds room height 2100f
            defaultDepthMm = 700f,   // exceeds adjacent wall depth 500f
        )

        val placed = room.containers.single()
        assertMm(2000f, placed.widthMm)
        assertMm(2100f, placed.heightMm)
        assertMm(500f, placed.depthMm)
        assertMm(0f, placed.offsetMm)

        val parts = RoomGeometryBuilder.build(room, selectedContainerId = "c1")
        val body = assertNotNull(parts.find { it.surface == RoomSurface.CONTAINER_BODY })
        val side = assertNotNull(parts.find { it.surface == RoomSurface.CONTAINER_SIDE_PANEL })
        val frontEdge = assertNotNull(parts.find { it.surface == RoomSurface.CONTAINER_FRONT_EDGE })
        val outline = assertNotNull(parts.find { it.surface == RoomSurface.CONTAINER_SELECTED_OUTLINE })
        val back = assertNotNull(parts.find { it.surface == RoomSurface.CONTAINER_BACK })

        // Verify that every single triangle in all container meshes is wound outward (CCW)
        // so Filament backface culling never hides visible outer faces or thicknesses!
        listOf(body, side, frontEdge, outline, back).forEach { part ->
            val m = part.mesh
            var t = 0
            while (t < m.indices.size) {
                val i0 = m.indices[t]
                val i1 = m.indices[t + 1]
                val i2 = m.indices[t + 2]

                val ax = m.positions[i1 * 3] - m.positions[i0 * 3]
                val ay = m.positions[i1 * 3 + 1] - m.positions[i0 * 3 + 1]
                val az = m.positions[i1 * 3 + 2] - m.positions[i0 * 3 + 2]
                val bx = m.positions[i2 * 3] - m.positions[i0 * 3]
                val by = m.positions[i2 * 3 + 1] - m.positions[i0 * 3 + 1]
                val bz = m.positions[i2 * 3 + 2] - m.positions[i0 * 3 + 2]

                val cx = ay * bz - az * by
                val cy = az * bx - ax * bz
                val cz = ax * by - ay * bx

                val dot = cx * m.normals[i0 * 3] +
                    cy * m.normals[i0 * 3 + 1] +
                    cz * m.normals[i0 * 3 + 2]

                assertTrue(dot > 0f, "Part ${part.surface} triangle $t has inverted winding (dot=$dot)")
                t += 3
            }
        }
    }

    @Test
    fun `corner fusion works on acute, 90, and obtuse corners with independent depths and outward CCW winding`() {
        val baseRoom = RoomSpec(
            clearWidthMm = 3600f,
            clearHeightMm = 2600f,
            clearDepthMm = 3600f,
            angleACDeg = 105f, // Obtuse corner NW (A-B)
        ).withAddedContainer(
            wall = com.closetstudio.core.model.WallIndex.WEST,
            id = "west_c",
            defaultWidthMm = 1800f,
            defaultDepthMm = 650f,
        ).withUpdatedContainer("west_c") {
            // Move near the NW corner (end of Wall WEST)
            it.copy(offsetMm = 1750f)
        }.withAddedContainer(
            wall = com.closetstudio.core.model.WallIndex.NORTH,
            id = "north_c",
            defaultWidthMm = 1600f,
            defaultDepthMm = 450f, // Independent depth!
        ).withUpdatedContainer("north_c") {
            // Move near the NW corner (start of Wall NORTH)
            it.copy(offsetMm = 100f)
        }

        val candidates = baseRoom.detectCornerIntersections()
        assertEquals(1, candidates.size)
        assertEquals(com.closetstudio.core.model.RoomCorner.NW, candidates.single().corner)

        val fusedRoom = baseRoom.withCornerFusion(com.closetstudio.core.model.RoomCorner.NW, fuse = true)
        val westFused = fusedRoom.containers.first { it.id == "west_c" }
        val northFused = fusedRoom.containers.first { it.id == "north_c" }
        assertTrue(westFused.fuseEndCorner)
        assertTrue(northFused.fuseStartCorner)
        // Independent depths remain untouched
        assertMm(650f, westFused.depthMm)
        assertMm(450f, northFused.depthMm)

        val parts = RoomGeometryBuilder.build(fusedRoom)
        parts.filter {
            it.surface == RoomSurface.CONTAINER_BODY ||
                it.surface == RoomSurface.CONTAINER_SIDE_PANEL ||
                it.surface == RoomSurface.CONTAINER_FRONT_EDGE ||
                it.surface == RoomSurface.CONTAINER_BACK
        }.forEach { part ->
            val m = part.mesh
            var t = 0
            while (t < m.indices.size) {
                val i0 = m.indices[t]
                val i1 = m.indices[t + 1]
                val i2 = m.indices[t + 2]

                val ax = m.positions[i1 * 3] - m.positions[i0 * 3]
                val ay = m.positions[i1 * 3 + 1] - m.positions[i0 * 3 + 1]
                val az = m.positions[i1 * 3 + 2] - m.positions[i0 * 3 + 2]
                val bx = m.positions[i2 * 3] - m.positions[i0 * 3]
                val by = m.positions[i2 * 3 + 1] - m.positions[i0 * 3 + 1]
                val bz = m.positions[i2 * 3 + 2] - m.positions[i0 * 3 + 2]

                val cx = ay * bz - az * by
                val cy = az * bx - ax * bz
                val cz = ax * by - ay * bx

                val dot = cx * m.normals[i0 * 3] +
                    cy * m.normals[i0 * 3 + 1] +
                    cz * m.normals[i0 * 3 + 2]

                assertTrue(dot > 0f, "Fused part ${part.surface} triangle $t has inverted winding (dot=$dot)")
                t += 3
            }
        }
    }

    @Test
    fun `closet with doors generates dark shadow reveal cavity behind closed doors`() {
        val room = RoomSpec(
            clearWidthMm = 3000f,
            clearHeightMm = 2400f,
            clearDepthMm = 3000f,
        ).withAddedContainer(
            wall = com.closetstudio.core.model.WallIndex.NORTH,
            id = "c1",
            defaultWidthMm = 1200f,
            defaultHeightMm = 2000f,
            defaultDepthMm = 600f,
        )

        val container = room.containers.single()
        val withDoors = room.copy(
            containers = listOf(
                container.copy(
                    cabinetDoors = listOf(
                        com.closetstudio.core.model.CabinetDoorsSpec(
                            id = "door1",
                            bayIndex = 0,
                            elevationYMm = 0f,
                            heightMm = 1800f,
                            doorCount = 2,
                        ),
                    ),
                ),
            ),
        )

        val parts = RoomGeometryBuilder.build(withDoors, showDoors = true)
        val shadow = assertNotNull(parts.find { it.surface == RoomSurface.CONTAINER_SHADOW_REVEAL })
        assertTrue(shadow.mesh.vertexCount > 0)

        // When showDoors is false (editing interior), shadow reveal cavity should NOT be generated
        val partsWithoutDoors = RoomGeometryBuilder.build(withDoors, showDoors = false)
        assertEquals(null, partsWithoutDoors.find { it.surface == RoomSurface.CONTAINER_SHADOW_REVEAL })
    }
}
