package com.closetstudio.core.geometry

import com.closetstudio.core.model.RoomSpec

enum class RoomSurface {
    FLOOR,
    WALL,
}

data class RoomPart(
    val surface: RoomSurface,
    val mesh: MeshData,
)

/**
 * Procedural geometry for a blank architectural room corner.
 *
 * All coordinates in millimetres:
 * - X: 0 to widthMm (left to right)
 * - Y: 0 to heightMm (floor to ceiling)
 * - Z: 0 to depthMm (back wall to front opening)
 */
object RoomGeometryBuilder {

    fun build(room: RoomSpec, uvScaleMm: Float = 600f): List<RoomPart> {
        val w = room.clearWidthMm
        val h = room.clearHeightMm
        val d = room.clearDepthMm
        val slab = 40f // Slab thickness for walls and floor in mm

        val floorBuilder = MeshBuilder(uvScaleMm)
        // Floor slab: from y = -slab to y = 0
        floorBuilder.addBox(
            min = Vec3(-slab, -slab, 0f),
            max = Vec3(w + slab, 0f, d + slab),
        )

        val wallBuilder = MeshBuilder(uvScaleMm)
        // Back wall: from z = -slab to z = 0
        wallBuilder.addBox(
            min = Vec3(-slab, 0f, -slab),
            max = Vec3(w + slab, h + slab, 0f),
        )
        // Left side wall: from x = -slab to x = 0
        wallBuilder.addBox(
            min = Vec3(-slab, 0f, 0f),
            max = Vec3(0f, h + slab, d + slab),
        )

        return listOf(
            RoomPart(RoomSurface.FLOOR, floorBuilder.build()),
            RoomPart(RoomSurface.WALL, wallBuilder.build()),
        )
    }
}
