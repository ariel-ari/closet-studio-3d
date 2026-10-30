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

        // 2. Ceiling
        val ceilingBuilder = MeshBuilder(uvScaleMm)
        ceilingBuilder.addBox(
            min = Vec3(-slab, h, -slab),
            max = Vec3(w + slab, h + slab, d + slab),
        )
        parts += RoomPart(RoomSurface.CEILING, ceilingBuilder.build())

        // 3. Walls
        val wallBuilder = MeshBuilder(uvScaleMm)
        val frameBuilder = MeshBuilder(uvScaleMm)
        val leafBuilder = MeshBuilder(uvScaleMm)
        val handleBuilder = MeshBuilder(uvScaleMm)

        // North Wall (Z = 0) - Solid
        wallBuilder.addBox(
            min = Vec3(-slab, 0f, -slab),
            max = Vec3(w + slab, h, 0f),
        )

        // West Wall (X = 0) - Solid
        wallBuilder.addBox(
            min = Vec3(-slab, 0f, 0f),
            max = Vec3(0f, h, d),
        )

        // East Wall (X = W) - Solid
        wallBuilder.addBox(
            min = Vec3(w, 0f, 0f),
            max = Vec3(w + slab, h, d),
        )

        // South Wall (Z = D) - With Parametric Door Opening
        val door = room.door
        val doorOffset = door.offsetMm.coerceIn(0f, (w - door.widthMm).coerceAtLeast(0f))
        val doorW = door.widthMm.coerceAtMost(w)
        val doorH = door.heightMm.coerceAtMost(h - 50f)
        val frameT = door.frameThicknessMm

        // Left wall segment (before door)
        if (doorOffset > 1f) {
            wallBuilder.addBox(
                min = Vec3(-slab, 0f, d),
                max = Vec3(doorOffset, h, d + slab),
            )
        }

        // Lintel segment (over door)
        wallBuilder.addBox(
            min = Vec3(doorOffset, doorH, d),
            max = Vec3(doorOffset + doorW, h, d + slab),
        )

        // Right wall segment (after door)
        if (doorOffset + doorW < w - 1f) {
            wallBuilder.addBox(
                min = Vec3(doorOffset + doorW, 0f, d),
                max = Vec3(w + slab, h, d + slab),
            )
        }

        // Door Frame
        // Left jamb
        frameBuilder.addBox(
            min = Vec3(doorOffset, 0f, d - 10f),
            max = Vec3(doorOffset + frameT, doorH, d + slab + 10f),
        )
        // Right jamb
        frameBuilder.addBox(
            min = Vec3(doorOffset + doorW - frameT, 0f, d - 10f),
            max = Vec3(doorOffset + doorW, doorH, d + slab + 10f),
        )
        // Head / top lintel frame
        frameBuilder.addBox(
            min = Vec3(doorOffset, doorH - frameT, d - 10f),
            max = Vec3(doorOffset + doorW, doorH, d + slab + 10f),
        )

        // Door Leaf (fitted inside frame)
        val leafMinX = doorOffset + frameT + 4f
        val leafMaxX = doorOffset + doorW - frameT - 4f
        val leafMaxY = doorH - frameT - 4f
        if (leafMaxX > leafMinX && leafMaxY > 0f) {
            leafBuilder.addBox(
                min = Vec3(leafMinX, 0f, d + (slab * 0.5f) - 20f),
                max = Vec3(leafMaxX, leafMaxY, d + (slab * 0.5f) + 20f),
            )

            // Door handle
            val handleY = 1000f
            val handleX = leafMaxX - 60f
            handleBuilder.addBox(
                min = Vec3(handleX - 15f, handleY - 15f, d - 20f),
                max = Vec3(handleX + 15f, handleY + 15f, d + slab + 20f),
            )
        }

        parts += RoomPart(RoomSurface.WALL, wallBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_FRAME, frameBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_LEAF, leafBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_HANDLE, handleBuilder.build())

        return parts
    }
}
