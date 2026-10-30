package com.closetstudio.core.geometry

import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex

enum class RoomSurface {
    FLOOR,
    WALL,
    CEILING,
    DOOR_FRAME,
    DOOR_LEAF,
    DOOR_HANDLE,
}

data class RoomPart(
    val surface: RoomSurface,
    val mesh: MeshData,
)

/**
 * Procedural geometry builder for an enclosed 4-walled architectural room
 * with a parametric door opening.
 *
 * Coordinate system:
 * - Origin (0, 0, 0): Front-Left floor corner (North-West)
 * - X: 0 to clearWidthMm (West to East)
 * - Y: 0 to clearHeightMm (Floor to Ceiling)
 * - Z: 0 to clearDepthMm (North to South)
 */
object RoomGeometryBuilder {

    fun build(room: RoomSpec, uvScaleMm: Float = 600f): List<RoomPart> {
        val w = room.clearWidthMm
        val h = room.clearHeightMm
        val d = room.clearDepthMm
        val slab = 40f // Wall/Floor slab thickness in mm

        val parts = mutableListOf<RoomPart>()

        // 1. Floor
        val floorBuilder = MeshBuilder(uvScaleMm)
        floorBuilder.addBox(
            min = Vec3(-slab, -slab, -slab),
            max = Vec3(w + slab, 0f, d + slab),
        )
        parts += RoomPart(RoomSurface.FLOOR, floorBuilder.build())

        // 2. Ceiling (inward-facing quad with normal -Y)
        // Visible when looking up from inside; naturally backface-culled when orbiting from above (Dollhouse)
        val ceilingBuilder = MeshBuilder(uvScaleMm)
        ceilingBuilder.addQuad(
            a = Vec3(-slab, h, -slab),
            b = Vec3(w + slab, h, -slab),
            c = Vec3(w + slab, h, d + slab),
            d = Vec3(-slab, h, d + slab),
            normal = Vec3(0f, -1f, 0f),
        )
        parts += RoomPart(RoomSurface.CEILING, ceilingBuilder.build())

        // 3. Walls (Inward-facing quads)
        // Fully visible from inside; automatically backface-culled from outside (Dollhouse / Maqueta)
        val wallBuilder = MeshBuilder(uvScaleMm)
        val frameBuilder = MeshBuilder(uvScaleMm)
        val leafBuilder = MeshBuilder(uvScaleMm)
        val handleBuilder = MeshBuilder(uvScaleMm)

        // North Wall (Z = 0) - Inward normal +Z (0, 0, 1)
        wallBuilder.addQuad(
            a = Vec3(0f, 0f, 0f),
            b = Vec3(w, 0f, 0f),
            c = Vec3(w, h, 0f),
            d = Vec3(0f, h, 0f),
            normal = Vec3(0f, 0f, 1f),
        )

        // West Wall (X = 0) - Inward normal +X (1, 0, 0)
        wallBuilder.addQuad(
            a = Vec3(0f, 0f, d),
            b = Vec3(0f, 0f, 0f),
            c = Vec3(0f, h, 0f),
            d = Vec3(0f, h, d),
            normal = Vec3(1f, 0f, 0f),
        )

        // East Wall (X = W) - Inward normal -X (-1, 0, 0)
        wallBuilder.addQuad(
            a = Vec3(w, 0f, 0f),
            b = Vec3(w, 0f, d),
            c = Vec3(w, h, d),
            d = Vec3(w, h, 0f),
            normal = Vec3(-1f, 0f, 0f),
        )

        // South Wall (Z = D) - Inward normal -Z (0, 0, -1) with door opening
        val door = room.door
        val doorOffset = door.offsetMm.coerceIn(0f, (w - door.widthMm).coerceAtLeast(0f))
        val doorW = door.widthMm.coerceAtMost(w)
        val doorH = door.heightMm.coerceAtMost(h - 50f)
        val frameT = door.frameThicknessMm

        // Left wall segment (before door: X from 0 to doorOffset)
        if (doorOffset > 1f) {
            wallBuilder.addQuad(
                a = Vec3(doorOffset, 0f, d),
                b = Vec3(0f, 0f, d),
                c = Vec3(0f, h, d),
                d = Vec3(doorOffset, h, d),
                normal = Vec3(0f, 0f, -1f),
            )
        }

        // Lintel segment (over door: X from doorOffset to doorOffset + doorW, Y from doorH to h)
        wallBuilder.addQuad(
            a = Vec3(doorOffset + doorW, doorH, d),
            b = Vec3(doorOffset, doorH, d),
            c = Vec3(doorOffset, h, d),
            d = Vec3(doorOffset + doorW, h, d),
            normal = Vec3(0f, 0f, -1f),
        )

        // Right wall segment (after door: X from doorOffset + doorW to w)
        if (doorOffset + doorW < w - 1f) {
            wallBuilder.addQuad(
                a = Vec3(w, 0f, d),
                b = Vec3(doorOffset + doorW, 0f, d),
                c = Vec3(doorOffset + doorW, h, d),
                d = Vec3(w, h, d),
                normal = Vec3(0f, 0f, -1f),
            )
        }

        // Door Frame (Facing -Z into the room, slightly proud at Z = d - 2f)
        val frameZ = d - 2f
        // Left jamb
        frameBuilder.addQuad(
            a = Vec3(doorOffset + frameT, 0f, frameZ),
            b = Vec3(doorOffset, 0f, frameZ),
            c = Vec3(doorOffset, doorH, frameZ),
            d = Vec3(doorOffset + frameT, doorH, frameZ),
            normal = Vec3(0f, 0f, -1f),
        )
        // Right jamb
        frameBuilder.addQuad(
            a = Vec3(doorOffset + doorW, 0f, frameZ),
            b = Vec3(doorOffset + doorW - frameT, 0f, frameZ),
            c = Vec3(doorOffset + doorW - frameT, doorH, frameZ),
            d = Vec3(doorOffset + doorW, doorH, frameZ),
            normal = Vec3(0f, 0f, -1f),
        )
        // Head / top lintel frame
        frameBuilder.addQuad(
            a = Vec3(doorOffset + doorW, doorH - frameT, frameZ),
            b = Vec3(doorOffset, doorH - frameT, frameZ),
            c = Vec3(doorOffset, doorH, frameZ),
            d = Vec3(doorOffset + doorW, doorH, frameZ),
            normal = Vec3(0f, 0f, -1f),
        )

        // Door Leaf (fitted inside frame, facing -Z)
        val leafMinX = doorOffset + frameT + 4f
        val leafMaxX = doorOffset + doorW - frameT - 4f
        val leafMaxY = doorH - frameT - 4f
        if (leafMaxX > leafMinX && leafMaxY > 0f) {
            leafBuilder.addQuad(
                a = Vec3(leafMaxX, 0f, d),
                b = Vec3(leafMinX, 0f, d),
                c = Vec3(leafMinX, leafMaxY, d),
                d = Vec3(leafMaxX, leafMaxY, d),
                normal = Vec3(0f, 0f, -1f),
            )

            // Door handle (facing -Z, slightly proud at d - 25f)
            val handleY = 1000f
            val handleX = leafMaxX - 60f
            handleBuilder.addQuad(
                a = Vec3(handleX + 15f, handleY - 15f, d - 25f),
                b = Vec3(handleX - 15f, handleY - 15f, d - 25f),
                c = Vec3(handleX - 15f, handleY + 15f, d - 25f),
                d = Vec3(handleX + 15f, handleY + 15f, d - 25f),
                normal = Vec3(0f, 0f, -1f),
            )
        }

        parts += RoomPart(RoomSurface.WALL, wallBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_FRAME, frameBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_LEAF, leafBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_HANDLE, handleBuilder.build())

        return parts
    }
}
