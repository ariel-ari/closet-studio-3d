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
        val h = room.clearHeightMm
        val slab = 40f

        val nw = room.vertexNW
        val ne = room.vertexNE
        val se = room.vertexSE
        val sw = room.vertexSW

        val parts = mutableListOf<RoomPart>()

        // 1. Floor (Polygonal quad matching exact room vertices)
        val floorBuilder = MeshBuilder(uvScaleMm)
        floorBuilder.addQuad(
            a = Vec3(nw.x, 0f, nw.z),
            b = Vec3(sw.x, 0f, sw.z),
            c = Vec3(se.x, 0f, se.z),
            d = Vec3(ne.x, 0f, ne.z),
            normal = Vec3.UNIT_Y,
        )
        floorBuilder.addQuad(
            a = Vec3(nw.x, -slab, nw.z),
            b = Vec3(ne.x, -slab, ne.z),
            c = Vec3(se.x, -slab, se.z),
            d = Vec3(sw.x, -slab, sw.z),
            normal = Vec3(0f, -1f, 0f),
        )
        parts += RoomPart(RoomSurface.FLOOR, floorBuilder.build())

        // 2. Ceiling (inward-facing quad with normal -Y)
        val ceilingBuilder = MeshBuilder(uvScaleMm)
        ceilingBuilder.addQuad(
            a = Vec3(nw.x, h, nw.z),
            b = Vec3(ne.x, h, ne.z),
            c = Vec3(se.x, h, se.z),
            d = Vec3(sw.x, h, sw.z),
            normal = Vec3(0f, -1f, 0f),
        )
        parts += RoomPart(RoomSurface.CEILING, ceilingBuilder.build())

        // 3. Walls (Inward-facing quads following exact vertices and angles)
        val wallBuilder = MeshBuilder(uvScaleMm)
        val frameBuilder = MeshBuilder(uvScaleMm)
        val leafBuilder = MeshBuilder(uvScaleMm)
        val handleBuilder = MeshBuilder(uvScaleMm)

        fun addWallSegment(x1: Float, z1: Float, x2: Float, z2: Float, y0: Float, y1: Float) {
            val dx = x2 - x1
            val dz = z2 - z1
            val len = kotlin.math.sqrt(dx * dx + dz * dz)
            if (len < 1f) return
            val normal = Vec3(-dz / len, 0f, dx / len)
            wallBuilder.addQuad(
                a = Vec3(x1, y0, z1),
                b = Vec3(x2, y0, z2),
                c = Vec3(x2, y1, z2),
                d = Vec3(x1, y1, z1),
                normal = normal,
            )
        }

        // North Wall (A: NW -> NE)
        addWallSegment(nw.x, nw.z, ne.x, ne.z, 0f, h)

        // East Wall (B: NE -> SE)
        addWallSegment(ne.x, ne.z, se.x, se.z, 0f, h)

        // West Wall (C: SW -> NW)
        addWallSegment(sw.x, sw.z, nw.x, nw.z, 0f, h)

        // South Wall (Door Wall: SW -> SE along vector u, inward normal pointing into room)
        val southDx = se.x - sw.x
        val southDz = se.z - sw.z
        val southLen = kotlin.math.sqrt(southDx * southDx + southDz * southDz).coerceAtLeast(100f)
        val ux = southDx / southLen
        val uz = southDz / southLen
        val inNx = uz
        val inNz = -ux
        val southNormal = Vec3(inNx, 0f, inNz)

        fun southPt(distFromSW: Float, y: Float, inwardOffset: Float = 0f): Vec3 {
            return Vec3(
                x = sw.x + distFromSW * ux + inwardOffset * inNx,
                y = y,
                z = sw.z + distFromSW * uz + inwardOffset * inNz,
            )
        }

        val door = room.door
        val doorOffset = door.offsetMm.coerceIn(0f, (southLen - door.widthMm).coerceAtLeast(0f))
        val doorW = door.widthMm.coerceAtMost(southLen)
        val doorH = door.heightMm.coerceAtMost(h - 50f)
        val frameT = door.frameThicknessMm

        // Segment D (before door: 0 to doorOffset)
        if (doorOffset > 1f) {
            wallBuilder.addQuad(
                a = southPt(doorOffset, 0f),
                b = southPt(0f, 0f),
                c = southPt(0f, h),
                d = southPt(doorOffset, h),
                normal = southNormal,
            )
        }

        // Lintel segment (over door: doorOffset to doorOffset + doorW, Y from doorH to h)
        wallBuilder.addQuad(
            a = southPt(doorOffset + doorW, doorH),
            b = southPt(doorOffset, doorH),
            c = southPt(doorOffset, h),
            d = southPt(doorOffset + doorW, h),
            normal = southNormal,
        )

        // Segment E (after door: doorOffset + doorW to southLen)
        if (doorOffset + doorW < southLen - 1f) {
            wallBuilder.addQuad(
                a = southPt(southLen, 0f),
                b = southPt(doorOffset + doorW, 0f),
                c = southPt(doorOffset + doorW, h),
                d = southPt(southLen, h),
                normal = southNormal,
            )
        }

        // Door Frame (slightly proud at 2mm inward)
        frameBuilder.addQuad(
            a = southPt(doorOffset + frameT, 0f, 2f),
            b = southPt(doorOffset, 0f, 2f),
            c = southPt(doorOffset, doorH, 2f),
            d = southPt(doorOffset + frameT, doorH, 2f),
            normal = southNormal,
        )
        frameBuilder.addQuad(
            a = southPt(doorOffset + doorW, 0f, 2f),
            b = southPt(doorOffset + doorW - frameT, 0f, 2f),
            c = southPt(doorOffset + doorW - frameT, doorH, 2f),
            d = southPt(doorOffset + doorW, doorH, 2f),
            normal = southNormal,
        )
        frameBuilder.addQuad(
            a = southPt(doorOffset + doorW, doorH - frameT, 2f),
            b = southPt(doorOffset, doorH - frameT, 2f),
            c = southPt(doorOffset, doorH, 2f),
            d = southPt(doorOffset + doorW, doorH, 2f),
            normal = southNormal,
        )

        // Door Leaf & Handle
        val leafMinS = doorOffset + frameT + 4f
        val leafMaxS = doorOffset + doorW - frameT - 4f
        val leafMaxY = doorH - frameT - 4f
        if (leafMaxS > leafMinS && leafMaxY > 0f) {
            leafBuilder.addQuad(
                a = southPt(leafMaxS, 0f),
                b = southPt(leafMinS, 0f),
                c = southPt(leafMinS, leafMaxY),
                d = southPt(leafMaxS, leafMaxY),
                normal = southNormal,
            )

            val handleY = 1000f
            val handleS = leafMaxS - 60f
            handleBuilder.addQuad(
                a = southPt(handleS + 15f, handleY - 15f, 25f),
                b = southPt(handleS - 15f, handleY - 15f, 25f),
                c = southPt(handleS - 15f, handleY + 15f, 25f),
                d = southPt(handleS + 15f, handleY + 15f, 25f),
                normal = southNormal,
            )
        }

        parts += RoomPart(RoomSurface.WALL, wallBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_FRAME, frameBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_LEAF, leafBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_HANDLE, handleBuilder.build())

        return parts
    }
}
