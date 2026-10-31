package com.closetstudio.core.geometry

import com.closetstudio.core.model.ClosetContainerSpec
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.Vec2Mm
import com.closetstudio.core.model.WallIndex

enum class RoomSurface {
    FLOOR,
    WALL,
    CEILING,
    DOOR_FRAME,
    DOOR_LEAF,
    DOOR_HANDLE,
    CONTAINER_BODY,
    CONTAINER_SIDE_PANEL,
    CONTAINER_FRONT_EDGE,
    CONTAINER_BACK,
    CONTAINER_SELECTED_OUTLINE,
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

    fun build(
        room: RoomSpec,
        uvScaleMm: Float = 600f,
        selectedContainerId: String? = null,
    ): List<RoomPart> {
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

        if (room.containers.isNotEmpty()) {
            val horizPanelsBuilder = MeshBuilder(uvScaleMm)
            val vertPanelsBuilder = MeshBuilder(uvScaleMm)
            val frontEdgesBuilder = MeshBuilder(uvScaleMm)
            val containerBackBuilder = MeshBuilder(uvScaleMm)
            val selectedOutlineBuilder = MeshBuilder(uvScaleMm)

            for (rawContainer in room.containers) {
                val container = room.clampContainer(rawContainer)
                if (container.widthMm < 40f || container.heightMm < 40f || container.depthMm < 40f) continue
                addContainerToBuilders(
                    room = room,
                    container = container,
                    horizPanelsBuilder = horizPanelsBuilder,
                    vertPanelsBuilder = vertPanelsBuilder,
                    frontEdgesBuilder = frontEdgesBuilder,
                    backBuilder = containerBackBuilder,
                    selectedOutlineBuilder = if (container.id == selectedContainerId) selectedOutlineBuilder else null,
                )
            }

            val horizMesh = horizPanelsBuilder.build()
            if (horizMesh.vertexCount > 0) {
                parts += RoomPart(RoomSurface.CONTAINER_BODY, horizMesh)
            }
            val vertMesh = vertPanelsBuilder.build()
            if (vertMesh.vertexCount > 0) {
                parts += RoomPart(RoomSurface.CONTAINER_SIDE_PANEL, vertMesh)
            }
            val frontEdgeMesh = frontEdgesBuilder.build()
            if (frontEdgeMesh.vertexCount > 0) {
                parts += RoomPart(RoomSurface.CONTAINER_FRONT_EDGE, frontEdgeMesh)
            }
            val backMesh = containerBackBuilder.build()
            if (backMesh.vertexCount > 0) {
                parts += RoomPart(RoomSurface.CONTAINER_BACK, backMesh)
            }
            val outlineMesh = selectedOutlineBuilder.build()
            if (outlineMesh.vertexCount > 0) {
                parts += RoomPart(RoomSurface.CONTAINER_SELECTED_OUTLINE, outlineMesh)
            }
        }

        return parts
    }

    /**
     * Builds a 3D oriented box in local wall coordinates:
     * - `s0..s1`: distance along the wall from its start vertex (`pStart` -> `pEnd`)
     * - `y0..y1`: vertical height from floor (`0`) to ceiling
     * - `d0..d1`: depth protruding inward from the wall along `inwardNormal`
     *
     * If [frontEdgeBuilder] is supplied, the front face (`d = d1`, facing the room) is emitted into
     * [frontEdgeBuilder] so the 28mm board thickness edge (`canto frontal`) has a crisp distinct
     * contrast against the deep interior side walls.
     */
    private fun addWallAlignedBox(
        builder: MeshBuilder,
        pStart: Vec2Mm,
        ux: Float,
        uz: Float,
        nx: Float,
        nz: Float,
        s0: Float,
        s1: Float,
        y0: Float,
        y1: Float,
        d0: Float,
        d1: Float,
        frontEdgeBuilder: MeshBuilder? = null,
    ) {
        if (s1 <= s0 || y1 <= y0 || d1 <= d0) return

        fun pt(s: Float, y: Float, d: Float): Vec3 = Vec3(
            x = pStart.x + s * ux + d * nx,
            y = y,
            z = pStart.z + s * uz + d * nz,
        )

        val nFront = Vec3(nx, 0f, nz)
        val nBack = Vec3(-nx, 0f, -nz)
        val nRight = Vec3(ux, 0f, uz)
        val nLeft = Vec3(-ux, 0f, -uz)

        // All 6 faces wound strictly Counter-Clockwise (CCW) when viewed from outside:
        // cross(b - a, c - a) points in the exact direction of each outward normal!

        // 1. Front face (d = d1, outward normal +n pointing into room)
        (frontEdgeBuilder ?: builder).addQuad(
            a = pt(s0, y0, d1),
            b = pt(s1, y0, d1),
            c = pt(s1, y1, d1),
            d = pt(s0, y1, d1),
            normal = nFront,
        )
        // 2. Back face (d = d0, outward normal -n pointing toward wall)
        builder.addQuad(
            a = pt(s1, y0, d0),
            b = pt(s0, y0, d0),
            c = pt(s0, y1, d0),
            d = pt(s1, y1, d0),
            normal = nBack,
        )
        // 3. Left face (s = s0, outward normal -u)
        builder.addQuad(
            a = pt(s0, y0, d0),
            b = pt(s0, y0, d1),
            c = pt(s0, y1, d1),
            d = pt(s0, y1, d0),
            normal = nLeft,
        )
        // 4. Right face (s = s1, outward normal +u)
        builder.addQuad(
            a = pt(s1, y0, d1),
            b = pt(s1, y0, d0),
            c = pt(s1, y1, d0),
            d = pt(s1, y1, d1),
            normal = nRight,
        )
        // 5. Top face (y = y1, outward normal +Y)
        builder.addQuad(
            a = pt(s0, y1, d1),
            b = pt(s1, y1, d1),
            c = pt(s1, y1, d0),
            d = pt(s0, y1, d0),
            normal = Vec3.UNIT_Y,
        )
        // 6. Bottom face (y = y0, outward normal -Y)
        builder.addQuad(
            a = pt(s0, y0, d0),
            b = pt(s1, y0, d0),
            c = pt(s1, y0, d1),
            d = pt(s0, y0, d1),
            normal = Vec3(0f, -1f, 0f),
        )
    }

    private fun addContainerToBuilders(
        room: RoomSpec,
        container: ClosetContainerSpec,
        horizPanelsBuilder: MeshBuilder,
        vertPanelsBuilder: MeshBuilder,
        frontEdgesBuilder: MeshBuilder,
        backBuilder: MeshBuilder,
        selectedOutlineBuilder: MeshBuilder?,
    ) {
        val nw = room.vertexNW
        val ne = room.vertexNE
        val se = room.vertexSE
        val sw = room.vertexSW

        val (pStart, pEnd) = when (container.wall) {
            WallIndex.NORTH -> nw to ne
            WallIndex.EAST -> ne to se
            WallIndex.SOUTH -> se to sw
            WallIndex.WEST -> sw to nw
        }

        val dx = pEnd.x - pStart.x
        val dz = pEnd.z - pStart.z
        val wallLen = kotlin.math.sqrt(dx * dx + dz * dz)
        if (wallLen < 10f) return

        val ux = dx / wallLen
        val uz = dz / wallLen
        // Inward normal pointing into the room interior
        val nx = -uz
        val nz = ux

        val s0 = container.offsetMm
        val s1 = s0 + container.widthMm
        val totalH = container.heightMm
        val totalD = container.depthMm.coerceAtLeast(350f)
        // Visible architectural carcass board thickness (28mm min so edges read clearly in 3D)
        val t = container.panelThicknessMm.coerceAtLeast(28f).coerceAtMost((container.widthMm * 0.2f).coerceAtLeast(28f))
        val plinthH = container.plinthHeightMm.coerceAtLeast(90f).coerceAtMost((totalH * 0.25f).coerceAtLeast(90f))
        val backT = container.backThicknessMm.coerceIn(8f, 18f)

        val wallGap = 4f // 4mm offset from wall surface to prevent Z-fighting

        // 1. Left Side Panel (full height 0..totalH, full depth d0..d1)
        addWallAlignedBox(
            builder = vertPanelsBuilder,
            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
            s0 = s0, s1 = s0 + t,
            y0 = 0f, y1 = totalH,
            d0 = wallGap, d1 = wallGap + totalD,
            frontEdgeBuilder = frontEdgesBuilder,
        )

        // 2. Right Side Panel (full height 0..totalH, full depth d0..d1)
        addWallAlignedBox(
            builder = vertPanelsBuilder,
            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
            s0 = s1 - t, s1 = s1,
            y0 = 0f, y1 = totalH,
            d0 = wallGap, d1 = wallGap + totalD,
            frontEdgeBuilder = frontEdgesBuilder,
        )

        // 3. Top Roof Panel (between side panels, y = totalH - t .. totalH)
        if (s1 - t > s0 + t && totalH > t) {
            addWallAlignedBox(
                builder = horizPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 + t, s1 = s1 - t,
                y0 = totalH - t, y1 = totalH,
                d0 = wallGap + backT, d1 = wallGap + totalD,
                frontEdgeBuilder = frontEdgesBuilder,
            )
        }

        // 4. Bottom Floor Panel (elevated above plinth, y = plinthH .. plinthH + t)
        if (s1 - t > s0 + t && totalH > plinthH + t * 2f) {
            addWallAlignedBox(
                builder = horizPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 + t, s1 = s1 - t,
                y0 = plinthH, y1 = plinthH + t,
                d0 = wallGap + backT, d1 = wallGap + totalD,
                frontEdgeBuilder = frontEdgesBuilder,
            )
        }

        // 5. Front Plinth / Toe-Kick (zocalo inferior recessed 15mm from front edge, y = 0 .. plinthH)
        if (s1 - t > s0 + t && plinthH > 2f) {
            val plinthSetback = 16f
            val dFront = (wallGap + totalD - plinthSetback).coerceAtLeast(wallGap + t)
            addWallAlignedBox(
                builder = vertPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 + t, s1 = s1 - t,
                y0 = 0f, y1 = plinthH,
                d0 = (dFront - t).coerceAtLeast(wallGap), d1 = dFront,
            )
        }

        // 6. Back Panel (espaldar claro recessed at the back of the 600mm deep cavity)
        if (s1 - t > s0 + t && totalH > plinthH + t) {
            addWallAlignedBox(
                builder = backBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 + t, s1 = s1 - t,
                y0 = plinthH, y1 = totalH - t,
                d0 = wallGap, d1 = wallGap + backT,
            )
        }

        // 7. Selection Highlight Outline around the front rim when selected
        if (selectedOutlineBuilder != null) {
            val rim = 6f
            val dOut0 = wallGap + totalD - 2f
            val dOut1 = wallGap + totalD + 6f
            // Left vertical highlight
            addWallAlignedBox(
                builder = selectedOutlineBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 - rim, s1 = s0 + rim,
                y0 = 0f, y1 = totalH + rim,
                d0 = dOut0, d1 = dOut1,
            )
            // Right vertical highlight
            addWallAlignedBox(
                builder = selectedOutlineBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s1 - rim, s1 = s1 + rim,
                y0 = 0f, y1 = totalH + rim,
                d0 = dOut0, d1 = dOut1,
            )
            // Top horizontal highlight
            addWallAlignedBox(
                builder = selectedOutlineBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 - rim, s1 = s1 + rim,
                y0 = totalH - rim, y1 = totalH + rim,
                d0 = dOut0, d1 = dOut1,
            )
            // Bottom horizontal highlight
            addWallAlignedBox(
                builder = selectedOutlineBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0 - rim, s1 = s1 + rim,
                y0 = 0f, y1 = rim * 2f,
                d0 = dOut0, d1 = dOut1,
            )
        }
    }
}
