package com.closetstudio.core.geometry

import com.closetstudio.core.model.ClosetContainerSpec
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomCorner
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.Vec2Mm
import com.closetstudio.core.model.WallIndex
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    PARTITION_SELECTED_OUTLINE,
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
        selectedPartitionId: String? = null,
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
            val partitionOutlineBuilder = MeshBuilder(uvScaleMm)

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
                    selectedPartitionId = selectedPartitionId,
                    partitionOutlineBuilder = partitionOutlineBuilder,
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
            val partOutlineMesh = partitionOutlineBuilder.build()
            if (partOutlineMesh.vertexCount > 0) {
                parts += RoomPart(RoomSurface.PARTITION_SELECTED_OUTLINE, partOutlineMesh)
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

    /**
     * Builds a horizontal slab (e.g. top roof or bottom shelf floor) whose back edge (`d = d0`)
     * spans `s0Back..s1Back` along the wall and whose front edge (`d = d1`) spans `s0Front..s1Front`.
     * This forms a clean mitered trapezoid at fused corners (`90°`, `<90°`, or `>90°`) with zero Z-fighting!
     */
    private fun addMiteredHorizontalSlab(
        builder: MeshBuilder,
        frontEdgeBuilder: MeshBuilder,
        pStart: Vec2Mm,
        ux: Float,
        uz: Float,
        nx: Float,
        nz: Float,
        s0Back: Float,
        s1Back: Float,
        s0Front: Float,
        s1Front: Float,
        y0: Float,
        y1: Float,
        d0: Float,
        d1: Float,
    ) {
        if (s1Back <= s0Back && s1Front <= s0Front) return
        if (y1 <= y0 || d1 <= d0) return

        val sb0 = s0Back.coerceAtMost(s1Back - 1f)
        val sb1 = s1Back.coerceAtLeast(sb0 + 1f)
        val sf0 = s0Front.coerceAtMost(s1Front - 1f)
        val sf1 = s1Front.coerceAtLeast(sf0 + 1f)

        fun pt(s: Float, y: Float, d: Float): Vec3 = Vec3(
            x = pStart.x + s * ux + d * nx,
            y = y,
            z = pStart.z + s * uz + d * nz,
        )

        val nFront = Vec3(nx, 0f, nz)
        val nBack = Vec3(-nx, 0f, -nz)

        // 1. Front edge face (d = d1, facing into room)
        frontEdgeBuilder.addQuad(
            a = pt(sf0, y0, d1),
            b = pt(sf1, y0, d1),
            c = pt(sf1, y1, d1),
            d = pt(sf0, y1, d1),
            normal = nFront,
        )
        // 2. Back face (d = d0, facing wall)
        builder.addQuad(
            a = pt(sb1, y0, d0),
            b = pt(sb0, y0, d0),
            c = pt(sb0, y1, d0),
            d = pt(sb1, y1, d0),
            normal = nBack,
        )
        // 3. Left end face (from (sb0, d0) to (sf0, d1))
        val l0 = pt(sb0, y0, d0)
        val l1 = pt(sf0, y0, d1)
        val ldx = l1.x - l0.x
        val ldz = l1.z - l0.z
        val lLen = sqrt(ldx * ldx + ldz * ldz).coerceAtLeast(1e-3f)
        val nLeft = Vec3(-ldz / lLen, 0f, ldx / lLen)
        builder.addQuad(
            a = pt(sb0, y0, d0),
            b = pt(sf0, y0, d1),
            c = pt(sf0, y1, d1),
            d = pt(sb0, y1, d0),
            normal = nLeft,
        )
        // 4. Right end face (from (sf1, d1) to (sb1, d0))
        val r0 = pt(sf1, y0, d1)
        val r1 = pt(sb1, y0, d0)
        val rdx = r1.x - r0.x
        val rdz = r1.z - r0.z
        val rLen = sqrt(rdx * rdx + rdz * rdz).coerceAtLeast(1e-3f)
        val nRight = Vec3(-rdz / rLen, 0f, rdx / rLen)
        builder.addQuad(
            a = pt(sf1, y0, d1),
            b = pt(sb1, y0, d0),
            c = pt(sb1, y1, d0),
            d = pt(sf1, y1, d1),
            normal = nRight,
        )
        // 5. Top face (y = y1, outward normal +Y)
        builder.addQuad(
            a = pt(sf0, y1, d1),
            b = pt(sf1, y1, d1),
            c = pt(sb1, y1, d0),
            d = pt(sb0, y1, d0),
            normal = Vec3.UNIT_Y,
        )
        // 6. Bottom face (y = y0, outward normal -Y)
        builder.addQuad(
            a = pt(sb0, y0, d0),
            b = pt(sb1, y0, d0),
            c = pt(sf1, y0, d1),
            d = pt(sf0, y0, d1),
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
        selectedPartitionId: String? = null,
        partitionOutlineBuilder: MeshBuilder? = null,
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
        val wallLen = sqrt(dx * dx + dz * dz)
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

        // Check if this container's start corner or end corner is fused with a neighbor container:
        val startCorner = RoomCorner.values().first { it.outgoingWall == container.wall }
        val endCorner = RoomCorner.values().first { it.incomingWall == container.wall }

        val fusedPrevNeighbor = if (container.fuseStartCorner) {
            room.containers
                .filter { it.wall == startCorner.incomingWall && it.fuseEndCorner }
                .map { room.clampContainer(it) }
                .firstOrNull()
        } else {
            null
        }

        val fusedNextNeighbor = if (container.fuseEndCorner) {
            room.containers
                .filter { it.wall == endCorner.outgoingWall && it.fuseStartCorner }
                .map { room.clampContainer(it) }
                .firstOrNull()
        } else {
            null
        }

        // Exact miter front-edge distance along the wall for start corner (s0Front) and end corner (s1Front):
        val s0Front = if (fusedPrevNeighbor != null) {
            val thetaRad = Math.toRadians(room.cornerAngleDeg(startCorner).coerceIn(15f, 165f).toDouble()).toFloat()
            val sinT = sin(thetaRad).coerceAtLeast(0.15f)
            val cosT = cos(thetaRad)
            val dPrev = fusedPrevNeighbor.depthMm.coerceAtLeast(350f) + wallGap
            val dCurr = totalD + wallGap
            ((dPrev + dCurr * cosT) / sinT).coerceIn(t, (s1 - t * 2f).coerceAtLeast(t))
        } else {
            s0 + t
        }

        val s1Front = if (fusedNextNeighbor != null) {
            val thetaRad = Math.toRadians(room.cornerAngleDeg(endCorner).coerceIn(15f, 165f).toDouble()).toFloat()
            val sinT = sin(thetaRad).coerceAtLeast(0.15f)
            val cosT = cos(thetaRad)
            val dNext = fusedNextNeighbor.depthMm.coerceAtLeast(350f) + wallGap
            val dCurr = totalD + wallGap
            val cutFromEnd = (dNext + dCurr * cosT) / sinT
            (wallLen - cutFromEnd).coerceIn((s0Front + t).coerceAtMost(s1 - t), s1 - t)
        } else {
            s1 - t
        }

        val s0Back = if (fusedPrevNeighbor != null) 0f else s0 + t
        val s1Back = if (fusedNextNeighbor != null) wallLen else s1 - t

        // 1. Left Side Panel:
        // - If NOT fused at start corner: normal full outer side panel at s0..s0+t
        // - If FUSED at start corner: omit vertical panel so the inside corner forms an open, continuous L!
        if (fusedPrevNeighbor == null) {
            addWallAlignedBox(
                builder = vertPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0, s1 = s0 + t,
                y0 = 0f, y1 = totalH,
                d0 = wallGap, d1 = wallGap + totalD,
                frontEdgeBuilder = frontEdgesBuilder,
            )
        }

        // 2. Right Side Panel:
        // - If NOT fused at end corner: normal full outer side panel at s1-t..s1
        // - If FUSED at end corner: omit vertical panel so the inside corner forms an open, continuous L!
        if (fusedNextNeighbor == null) {
            addWallAlignedBox(
                builder = vertPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s1 - t, s1 = s1,
                y0 = 0f, y1 = totalH,
                d0 = wallGap, d1 = wallGap + totalD,
                frontEdgeBuilder = frontEdgesBuilder,
            )
        }

        // 3. Top Roof Panel (seamless mitered trapezoid when corner-fused, normal rectangle otherwise)
        if (totalH > t) {
            addMiteredHorizontalSlab(
                builder = horizPanelsBuilder,
                frontEdgeBuilder = frontEdgesBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0Back = s0Back, s1Back = s1Back,
                s0Front = s0Front, s1Front = s1Front,
                y0 = totalH - t, y1 = totalH,
                d0 = wallGap + backT, d1 = wallGap + totalD,
            )
        }

        // 4. Bottom Floor Panel (elevated above plinth, seamless mitered trapezoid when corner-fused)
        if (totalH > plinthH + t * 2f) {
            addMiteredHorizontalSlab(
                builder = horizPanelsBuilder,
                frontEdgeBuilder = frontEdgesBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0Back = s0Back, s1Back = s1Back,
                s0Front = s0Front, s1Front = s1Front,
                y0 = plinthH, y1 = plinthH + t,
                d0 = wallGap + backT, d1 = wallGap + totalD,
            )
        }

        // 5. Front Plinth / Toe-Kick (trimmed to s0Plinth..s1Plinth so the L-plinth meets cleanly at the inner corner)
        val plinthSetback = 16f
        val dCurrPlinth = (wallGap + totalD - plinthSetback).coerceAtLeast(wallGap + t)
        val s0Plinth = if (fusedPrevNeighbor != null) {
            val thetaRad = Math.toRadians(room.cornerAngleDeg(startCorner).coerceIn(15f, 165f).toDouble()).toFloat()
            val sinT = sin(thetaRad).coerceAtLeast(0.15f)
            val cosT = cos(thetaRad)
            val dPrevPlinth = (fusedPrevNeighbor.depthMm.coerceAtLeast(350f) + wallGap - plinthSetback).coerceAtLeast(wallGap + t)
            ((dPrevPlinth + dCurrPlinth * cosT) / sinT).coerceIn(0f, s1 - t)
        } else {
            s0 + t
        }

        val s1Plinth = if (fusedNextNeighbor != null) {
            val thetaRad = Math.toRadians(room.cornerAngleDeg(endCorner).coerceIn(15f, 165f).toDouble()).toFloat()
            val sinT = sin(thetaRad).coerceAtLeast(0.15f)
            val cosT = cos(thetaRad)
            val dNextPlinth = (fusedNextNeighbor.depthMm.coerceAtLeast(350f) + wallGap - plinthSetback).coerceAtLeast(wallGap + t)
            val cutFromEnd = (dNextPlinth + dCurrPlinth * cosT) / sinT
            (wallLen - cutFromEnd).coerceIn(s0Plinth, s1 - t)
        } else {
            s1 - t
        }

        if (s1Plinth > s0Plinth && plinthH > 2f) {
            addWallAlignedBox(
                builder = vertPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0Plinth, s1 = s1Plinth,
                y0 = 0f, y1 = plinthH,
                d0 = (dCurrPlinth - t).coerceAtLeast(wallGap), d1 = dCurrPlinth,
            )
        }

        // 6. Back Panel (extends into the corner vertex when fused so there is no gap at the corner wall)
        if (s1Back > s0Back && totalH > plinthH + t) {
            addWallAlignedBox(
                builder = backBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = s0Back, s1 = s1Back,
                y0 = plinthH, y1 = totalH - t,
                d0 = wallGap, d1 = wallGap + backT,
            )
        }

        // 7. Interior Partitions: Vertical Dividers & Horizontal Shelves
        val s0Inner = s0 + t
        val bays = container.getBays()

        // 7a. Vertical Dividers
        for (div in container.dividers) {
            val divS0 = s0Inner + div.offsetXMm
            val divS1 = divS0 + div.thicknessMm
            val divY0 = plinthH + t
            val divY1 = totalH - t
            val divD0 = wallGap + backT
            val divD1 = wallGap + totalD

            if (divS1 > divS0 && divY1 > divY0) {
                addWallAlignedBox(
                    builder = vertPanelsBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = divS0, s1 = divS1,
                    y0 = divY0, y1 = divY1,
                    d0 = divD0, d1 = divD1,
                    frontEdgeBuilder = frontEdgesBuilder,
                )

                // Selection Highlight Outline (Crisp Purple Rim matching user screenshot)
                if (div.id == selectedPartitionId && partitionOutlineBuilder != null) {
                    val pRim = 5f
                    val dOut0 = divD1 - 2f
                    val dOut1 = divD1 + 6f
                    // Front outline
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = divS0 - pRim, s1 = divS1 + pRim,
                        y0 = divY0 - pRim, y1 = divY1 + pRim,
                        d0 = dOut0, d1 = dOut1,
                    )
                    // Depth top edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = divS0 - pRim, s1 = divS1 + pRim,
                        y0 = divY1 - pRim, y1 = divY1 + pRim,
                        d0 = divD0, d1 = divD1,
                    )
                    // Depth bottom edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = divS0 - pRim, s1 = divS1 + pRim,
                        y0 = divY0 - pRim, y1 = divY0 + pRim,
                        d0 = divD0, d1 = divD1,
                    )
                    // Back vertical edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = divS0 - pRim, s1 = divS1 + pRim,
                        y0 = divY0, y1 = divY1,
                        d0 = divD0 - pRim, d1 = divD0 + pRim,
                    )
                }
            }
        }

        // 7b. Horizontal Shelves (Automatically adapted from left to right of its containing bay)
        for (shelf in container.shelves) {
            val bay = bays.getOrElse(shelf.bayIndex) { bays.last() }
            val shelfS0 = s0Inner + bay.startXMm
            val shelfS1 = s0Inner + bay.endXMm
            val shelfY0 = plinthH + t + shelf.elevationYMm
            val shelfY1 = shelfY0 + shelf.thicknessMm
            val shelfD0 = wallGap + backT
            val shelfD1 = wallGap + totalD

            if (shelfS1 > shelfS0 && shelfY1 > shelfY0 && shelfY1 <= totalH - t) {
                addWallAlignedBox(
                    builder = horizPanelsBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = shelfS0, s1 = shelfS1,
                    y0 = shelfY0, y1 = shelfY1,
                    d0 = shelfD0, d1 = shelfD1,
                    frontEdgeBuilder = frontEdgesBuilder,
                )

                // Selection Highlight Outline (Crisp Purple Rim matching user screenshot)
                if (shelf.id == selectedPartitionId && partitionOutlineBuilder != null) {
                    val pRim = 5f
                    val dOut0 = shelfD1 - 2f
                    val dOut1 = shelfD1 + 6f
                    // Front edge outline
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = shelfS0 - pRim, s1 = shelfS1 + pRim,
                        y0 = shelfY0 - pRim, y1 = shelfY1 + pRim,
                        d0 = dOut0, d1 = dOut1,
                    )
                    // Left depth edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = shelfS0 - pRim, s1 = shelfS0 + pRim,
                        y0 = shelfY0 - pRim, y1 = shelfY1 + pRim,
                        d0 = shelfD0, d1 = shelfD1,
                    )
                    // Right depth edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = shelfS1 - pRim, s1 = shelfS1 + pRim,
                        y0 = shelfY0 - pRim, y1 = shelfY1 + pRim,
                        d0 = shelfD0, d1 = shelfD1,
                    )
                    // Back depth edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = shelfS0, s1 = shelfS1,
                        y0 = shelfY0 - pRim, y1 = shelfY1 + pRim,
                        d0 = shelfD0 - pRim, d1 = shelfD0 + pRim,
                    )
                }
            }
        }

        // 8. Container Selection Highlight Outline around the front rim when selected
        if (selectedOutlineBuilder != null) {
            val rim = 6f
            val dOut0 = wallGap + totalD - 2f
            val dOut1 = wallGap + totalD + 6f
            // Left vertical highlight (only on free end, not in fused corner)
            if (fusedPrevNeighbor == null) {
                addWallAlignedBox(
                    builder = selectedOutlineBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = s0 - rim, s1 = s0 + rim,
                    y0 = 0f, y1 = totalH + rim,
                    d0 = dOut0, d1 = dOut1,
                )
            }
            // Right vertical highlight (only on free end, not in fused corner)
            if (fusedNextNeighbor == null) {
                addWallAlignedBox(
                    builder = selectedOutlineBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = s1 - rim, s1 = s1 + rim,
                    y0 = 0f, y1 = totalH + rim,
                    d0 = dOut0, d1 = dOut1,
                )
            }
            // Top horizontal highlight
            val hlS0 = if (fusedPrevNeighbor != null) s0Front else s0 - rim
            val hlS1 = if (fusedNextNeighbor != null) s1Front else s1 + rim
            if (hlS1 > hlS0) {
                addWallAlignedBox(
                    builder = selectedOutlineBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = hlS0, s1 = hlS1,
                    y0 = totalH - rim, y1 = totalH + rim,
                    d0 = dOut0, d1 = dOut1,
                )
                // Bottom horizontal highlight
                addWallAlignedBox(
                    builder = selectedOutlineBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = hlS0, s1 = hlS1,
                    y0 = 0f, y1 = rim * 2f,
                    d0 = dOut0, d1 = dOut1,
                )
            }
        }
    }
}
