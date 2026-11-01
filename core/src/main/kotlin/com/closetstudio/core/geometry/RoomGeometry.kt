package com.closetstudio.core.geometry

import com.closetstudio.core.model.ClosetContainerSpec
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.DrawerFrontType
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
    val textureKey: String? = null,
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
        showDoors: Boolean = true,
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
        val floorTextureKey = room.floorTextureId?.let { "textures/floors/$it" }
        parts += RoomPart(RoomSurface.FLOOR, floorBuilder.build(), textureKey = floorTextureKey)

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

        val wallTextureKey = room.wallTextureId?.let { "textures/walls/$it" }
        parts += RoomPart(RoomSurface.WALL, wallBuilder.build(), textureKey = wallTextureKey)
        parts += RoomPart(RoomSurface.DOOR_FRAME, frameBuilder.build())
        parts += RoomPart(RoomSurface.DOOR_LEAF, leafBuilder.build())

        if (room.containers.isNotEmpty()) {
            val selectedOutlineBuilder = MeshBuilder(uvScaleMm)
            val partitionOutlineBuilder = MeshBuilder(uvScaleMm)

            for (rawContainer in room.containers) {
                val container = room.clampContainer(rawContainer)
                if (container.widthMm < 40f || container.heightMm < 40f || container.depthMm < 40f) continue

                val textureKey = (container.materialTextureId ?: room.defaultMelamineTextureId)?.let {
                    "textures/melamines/$it"
                }
                val grainRotated = container.isGrainHorizontal

                val cHorizPanelsBuilder = MeshBuilder(uvScaleMm, rotateUv90 = grainRotated)
                val cVertPanelsBuilder = MeshBuilder(uvScaleMm, rotateUv90 = grainRotated)
                val cFrontEdgesBuilder = MeshBuilder(uvScaleMm, rotateUv90 = grainRotated)
                val cBackBuilder = MeshBuilder(uvScaleMm, rotateUv90 = grainRotated)

                addContainerToBuilders(
                    room = room,
                    container = container,
                    horizPanelsBuilder = cHorizPanelsBuilder,
                    vertPanelsBuilder = cVertPanelsBuilder,
                    frontEdgesBuilder = cFrontEdgesBuilder,
                    backBuilder = cBackBuilder,
                    selectedOutlineBuilder = if (container.id == selectedContainerId) selectedOutlineBuilder else null,
                    selectedPartitionId = selectedPartitionId,
                    partitionOutlineBuilder = partitionOutlineBuilder,
                    handleBuilder = handleBuilder,
                    showDoors = showDoors,
                )

                val horizMesh = cHorizPanelsBuilder.build()
                if (horizMesh.vertexCount > 0) {
                    parts += RoomPart(RoomSurface.CONTAINER_BODY, horizMesh, textureKey = textureKey)
                }
                val vertMesh = cVertPanelsBuilder.build()
                if (vertMesh.vertexCount > 0) {
                    parts += RoomPart(RoomSurface.CONTAINER_SIDE_PANEL, vertMesh, textureKey = textureKey)
                }
                val frontEdgeMesh = cFrontEdgesBuilder.build()
                if (frontEdgeMesh.vertexCount > 0) {
                    parts += RoomPart(RoomSurface.CONTAINER_FRONT_EDGE, frontEdgeMesh, textureKey = textureKey)
                }
                val backMesh = cBackBuilder.build()
                if (backMesh.vertexCount > 0) {
                    parts += RoomPart(RoomSurface.CONTAINER_BACK, backMesh, textureKey = textureKey)
                }
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

        val handleMesh = handleBuilder.build()
        if (handleMesh.vertexCount > 0) {
            parts += RoomPart(RoomSurface.DOOR_HANDLE, handleMesh)
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

    private fun addWallAlignedInclinedBox(
        builder: MeshBuilder,
        pStart: Vec2Mm,
        ux: Float,
        uz: Float,
        nx: Float,
        nz: Float,
        s0: Float,
        s1: Float,
        yFront0: Float,
        yFront1: Float,
        yBack0: Float,
        yBack1: Float,
        d0: Float,
        d1: Float,
        frontEdgeBuilder: MeshBuilder? = null,
    ) {
        if (s1 <= s0 || d1 <= d0) return

        fun pt(s: Float, y: Float, d: Float): Vec3 = Vec3(
            x = pStart.x + s * ux + d * nx,
            y = y,
            z = pStart.z + s * uz + d * nz,
        )

        val deltaD = d1 - d0
        val deltaY = yFront1 - yBack1
        val slopeLen = kotlin.math.sqrt(deltaD * deltaD + deltaY * deltaY).coerceAtLeast(0.001f)
        val nTopY = deltaD / slopeLen
        val nTopD = -deltaY / slopeLen
        val nTop = Vec3(nx * nTopD, nTopY, nz * nTopD)
        val nBot = Vec3(-nx * nTopD, -nTopY, -nz * nTopD)

        val nFront = Vec3(nx, 0f, nz)
        val nBack = Vec3(-nx, 0f, -nz)
        val nRight = Vec3(ux, 0f, uz)
        val nLeft = Vec3(-ux, 0f, -uz)

        // 1. Top face (inclined sloping down towards front opening)
        builder.addQuad(
            a = pt(s0, yFront1, d1),
            b = pt(s1, yFront1, d1),
            c = pt(s1, yBack1, d0),
            d = pt(s0, yBack1, d0),
            normal = nTop,
        )
        // 2. Bottom face
        builder.addQuad(
            a = pt(s0, yBack0, d0),
            b = pt(s1, yBack0, d0),
            c = pt(s1, yFront0, d1),
            d = pt(s0, yFront0, d1),
            normal = nBot,
        )
        // 3. Front face (at d = d1)
        (frontEdgeBuilder ?: builder).addQuad(
            a = pt(s0, yFront0, d1),
            b = pt(s1, yFront0, d1),
            c = pt(s1, yFront1, d1),
            d = pt(s0, yFront1, d1),
            normal = nFront,
        )
        // 4. Back face (at d = d0)
        builder.addQuad(
            a = pt(s1, yBack0, d0),
            b = pt(s0, yBack0, d0),
            c = pt(s0, yBack1, d0),
            d = pt(s1, yBack1, d0),
            normal = nBack,
        )
        // 5. Left face (at s = s0)
        builder.addQuad(
            a = pt(s0, yBack0, d0),
            b = pt(s0, yFront0, d1),
            c = pt(s0, yFront1, d1),
            d = pt(s0, yBack1, d0),
            normal = nLeft,
        )
        // 6. Right face (at s = s1)
        builder.addQuad(
            a = pt(s1, yFront0, d1),
            b = pt(s1, yBack0, d0),
            c = pt(s1, yBack1, d0),
            d = pt(s1, yFront1, d1),
            normal = nRight,
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

    private fun addWallAlignedCylinder(
        builder: MeshBuilder,
        pStart: Vec2Mm,
        ux: Float,
        uz: Float,
        nx: Float,
        nz: Float,
        s0: Float,
        s1: Float,
        centerY: Float,
        centerD: Float,
        radiusMm: Float,
        segments: Int = 12,
    ) {
        if (s1 <= s0 || radiusMm <= 0f) return
        fun pt(s: Float, y: Float, d: Float): Vec3 = Vec3(
            x = pStart.x + s * ux + d * nx,
            y = y,
            z = pStart.z + s * uz + d * nz,
        )

        val angleStep = (2.0 * Math.PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * angleStep
            val a1 = (i + 1) * angleStep
            val y0 = centerY + radiusMm * kotlin.math.sin(a0)
            val d0 = centerD + radiusMm * kotlin.math.cos(a0)
            val y1 = centerY + radiusMm * kotlin.math.sin(a1)
            val d1 = centerD + radiusMm * kotlin.math.cos(a1)

            val midA = (a0 + a1) * 0.5f
            val nY = kotlin.math.sin(midA)
            val nD = kotlin.math.cos(midA)
            val norm = Vec3(nD * nx, nY, nD * nz)

            builder.addQuad(
                a = pt(s0, y0, d0),
                b = pt(s1, y0, d0),
                c = pt(s1, y1, d1),
                d = pt(s0, y1, d1),
                normal = norm,
            )
        }
    }

    private fun addVerticalCylinder(
        builder: MeshBuilder,
        pStart: Vec2Mm,
        ux: Float,
        uz: Float,
        nx: Float,
        nz: Float,
        y0: Float,
        y1: Float,
        centerS: Float,
        centerD: Float,
        radiusMm: Float,
        segments: Int = 12,
    ) {
        if (y1 <= y0 || radiusMm <= 0f) return
        fun pt(s: Float, y: Float, d: Float): Vec3 = Vec3(
            x = pStart.x + s * ux + d * nx,
            y = y,
            z = pStart.z + s * uz + d * nz,
        )

        val angleStep = (2.0 * Math.PI / segments).toFloat()
        for (i in 0 until segments) {
            val a0 = i * angleStep
            val a1 = (i + 1) * angleStep
            val s0 = centerS + radiusMm * kotlin.math.sin(a0)
            val d0 = centerD + radiusMm * kotlin.math.cos(a0)
            val s1 = centerS + radiusMm * kotlin.math.sin(a1)
            val d1 = centerD + radiusMm * kotlin.math.cos(a1)

            val midA = (a0 + a1) * 0.5f
            val nS = kotlin.math.sin(midA)
            val nD = kotlin.math.cos(midA)
            val norm = Vec3(nS * ux + nD * nx, 0f, nS * uz + nD * nz)

            builder.addQuad(
                a = pt(s0, y0, d0),
                b = pt(s1, y0, d1),
                c = pt(s1, y1, d1),
                d = pt(s0, y1, d0),
                normal = norm,
            )
        }
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
        handleBuilder: MeshBuilder? = null,
        showDoors: Boolean = true,
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
        val t = container.effectivePanelThicknessMm
        val plinthH = container.effectivePlinthHeightMm
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
        // Strict Carpentry Containment Invariant: interior bounds are strictly between bottom and top panels,
        // and between left and right side panels. ZERO penetration into carcass boards.
        val interiorBottomY = plinthH + t
        val interiorTopY = (totalH - t).coerceAtLeast(interiorBottomY)
        val interiorLeftS = s0 + t
        val interiorRightS = (s1 - t).coerceAtLeast(interiorLeftS)

        val s0Inner = s0 + t
        val bays = container.getBays()

        // 7a. Vertical Dividers
        for (div in container.dividers) {
            val divS0 = (s0Inner + div.offsetXMm).coerceIn(interiorLeftS, (interiorRightS - div.thicknessMm).coerceAtLeast(interiorLeftS))
            val divS1 = (divS0 + div.thicknessMm).coerceIn(divS0, interiorRightS)
            val divY0 = (interiorBottomY + div.effectiveStartY()).coerceIn(interiorBottomY, interiorTopY)
            val divY1 = (interiorBottomY + div.effectiveEndY(container.interiorHeightMm)).coerceIn(divY0, interiorTopY)
            val divD0 = wallGap + backT
            val divD1 = wallGap + backT + div.effectiveDepth(container.interiorDepthMm)

            if (divS1 > divS0 && divY1 > divY0) {
                addWallAlignedBox(
                    builder = vertPanelsBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = divS0, s1 = divS1,
                    y0 = divY0, y1 = divY1,
                    d0 = divD0, d1 = divD1,
                    frontEdgeBuilder = frontEdgesBuilder,
                )

                // Selection & Smart Alignment Highlight Outline (Crisp Purple Rim matching user screenshot, strictly within carcass interior)
                val selectedDiv = if (selectedPartitionId != null) container.dividers.firstOrNull { it.id == selectedPartitionId } else null
                val isDivSelected = div.id == selectedPartitionId
                val isDivAligned = selectedDiv != null && div.id != selectedDiv.id && kotlin.math.abs(div.offsetXMm - selectedDiv.offsetXMm) < 1.0f

                if ((isDivSelected || isDivAligned) && partitionOutlineBuilder != null) {
                    val pRim = 4f
                    val dOut0 = divD1 - 2f
                    val dOut1 = divD1 + 6f
                    val outS0 = (divS0 - pRim).coerceAtLeast(interiorLeftS)
                    val outS1 = (divS1 + pRim).coerceAtMost(interiorRightS)
                    val outY0 = (divY0 - pRim).coerceAtLeast(interiorBottomY)
                    val outY1 = (divY1 + pRim).coerceAtMost(interiorTopY)
                    // Front outline
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = outS0, s1 = outS1,
                        y0 = outY0, y1 = outY1,
                        d0 = dOut0, d1 = dOut1,
                    )
                    // Depth top edge (staying strictly flush under top roof panel)
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = outS0, s1 = outS1,
                        y0 = (divY1 - pRim).coerceAtLeast(interiorBottomY), y1 = divY1,
                        d0 = divD0, d1 = divD1,
                    )
                    // Depth bottom edge (staying strictly flush above bottom floor panel)
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = outS0, s1 = outS1,
                        y0 = divY0, y1 = (divY0 + pRim).coerceAtMost(interiorTopY),
                        d0 = divD0, d1 = divD1,
                    )
                    // Back vertical edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = outS0, s1 = outS1,
                        y0 = divY0, y1 = divY1,
                        d0 = divD0 - 2f, d1 = divD0 + 4f,
                    )
                }
            }
        }

        // 7b. Horizontal Shelves (Automatically adapted from left to right of its containing bay)
        for (shelf in container.shelves) {
            val bay = bays.getOrElse(shelf.bayIndex) { bays.last() }
            val shelfS0 = (s0Inner + bay.startXMm).coerceIn(interiorLeftS, interiorRightS)
            val shelfS1 = (s0Inner + bay.endXMm).coerceIn(shelfS0, interiorRightS)
            val shelfY0 = (interiorBottomY + shelf.elevationYMm).coerceIn(interiorBottomY, (interiorTopY - shelf.thicknessMm).coerceAtLeast(interiorBottomY))
            val shelfY1 = (shelfY0 + shelf.thicknessMm).coerceIn(shelfY0, interiorTopY)
            val shelfD0 = wallGap + backT
            val shelfD1 = wallGap + backT + shelf.effectiveDepth(container.interiorDepthMm)

            if (shelfS1 > shelfS0 && shelfY1 > shelfY0) {
                addWallAlignedBox(
                    builder = horizPanelsBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = shelfS0, s1 = shelfS1,
                    y0 = shelfY0, y1 = shelfY1,
                    d0 = shelfD0, d1 = shelfD1,
                    frontEdgeBuilder = frontEdgesBuilder,
                )

                // Selection & Smart Alignment Highlight Outline (Crisp Purple Rim matching user screenshot, strictly within carcass interior)
                val selectedShelf = if (selectedPartitionId != null) container.shelves.firstOrNull { it.id == selectedPartitionId } else null
                val isShelfSelected = shelf.id == selectedPartitionId
                val isShelfAligned = selectedShelf != null && shelf.id != selectedShelf.id && kotlin.math.abs(shelf.elevationYMm - selectedShelf.elevationYMm) < 1.0f

                if ((isShelfSelected || isShelfAligned) && partitionOutlineBuilder != null) {
                    val pRim = 4f
                    val dOut0 = shelfD1 - 2f
                    val dOut1 = shelfD1 + 6f
                    val outS0 = (shelfS0 - pRim).coerceAtLeast(interiorLeftS)
                    val outS1 = (shelfS1 + pRim).coerceAtMost(interiorRightS)
                    val outY0 = (shelfY0 - pRim).coerceAtLeast(interiorBottomY)
                    val outY1 = (shelfY1 + pRim).coerceAtMost(interiorTopY)
                    // Front edge outline
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = outS0, s1 = outS1,
                        y0 = outY0, y1 = outY1,
                        d0 = dOut0, d1 = dOut1,
                    )
                    // Left depth edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = shelfS0, s1 = (shelfS0 + pRim).coerceAtMost(shelfS1),
                        y0 = outY0, y1 = outY1,
                        d0 = shelfD0, d1 = shelfD1,
                    )
                    // Right depth edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = (shelfS1 - pRim).coerceAtLeast(shelfS0), s1 = shelfS1,
                        y0 = outY0, y1 = outY1,
                        d0 = shelfD0, d1 = shelfD1,
                    )
                    // Back depth edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = shelfS0, s1 = shelfS1,
                        y0 = outY0, y1 = outY1,
                        d0 = shelfD0 - 2f, d1 = shelfD0 + 4f,
                    )
                }
            }
        }

        // 7c. Drawers (Cabinetry drawer box with front face, chrome handle, and modular containment)
        for (drawer in container.drawers) {
            val bay = bays.getOrElse(drawer.bayIndex) { bays.last() }
            val drawerS0 = (s0Inner + bay.startXMm).coerceIn(interiorLeftS, interiorRightS)
            val drawerS1 = (s0Inner + bay.endXMm).coerceIn(drawerS0, interiorRightS)
            val drawerY0 = (interiorBottomY + drawer.elevationYMm).coerceIn(interiorBottomY, (interiorTopY - drawer.heightMm).coerceAtLeast(interiorBottomY))
            val drawerY1 = (drawerY0 + drawer.heightMm).coerceIn(drawerY0, interiorTopY)
            val drawerD0 = wallGap + backT + 10f
            val drawerD1 = wallGap + backT + drawer.effectiveDepth(container.interiorDepthMm)

            if (drawerS1 > drawerS0 && drawerY1 > drawerY0) {
                // 1. Front face board (with 2mm side clearance from bay sides for realistic cabinetry slide)
                val fS0 = (drawerS0 + 2f).coerceAtMost(drawerS1)
                val fS1 = (drawerS1 - 2f).coerceAtLeast(fS0)
                val fD0 = drawerD1 - 18f
                val fD1 = drawerD1

                if (drawer.frontType == DrawerFrontType.NOTCH) {
                    // Finger pull notch cutout in top center
                    val notchWidth = ((fS1 - fS0) * 0.35f).coerceIn(120f, 260f)
                    val nS0 = (fS0 + fS1 - notchWidth) * 0.5f
                    val nS1 = nS0 + notchWidth
                    val notchCutoutH = 35f.coerceAtMost((drawerY1 - drawerY0) * 0.40f)
                    val notchY = drawerY1 - notchCutoutH

                    // Left piece (full height)
                    if (nS0 > fS0) {
                        addWallAlignedBox(
                            builder = horizPanelsBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = fS0, s1 = nS0,
                            y0 = drawerY0, y1 = drawerY1,
                            d0 = fD0, d1 = fD1,
                            frontEdgeBuilder = frontEdgesBuilder,
                        )
                    }
                    // Right piece (full height)
                    if (fS1 > nS1) {
                        addWallAlignedBox(
                            builder = horizPanelsBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = nS1, s1 = fS1,
                            y0 = drawerY0, y1 = drawerY1,
                            d0 = fD0, d1 = fD1,
                            frontEdgeBuilder = frontEdgesBuilder,
                        )
                    }
                    // Center bottom piece (below cutout)
                    if (notchY > drawerY0 && nS1 > nS0) {
                        addWallAlignedBox(
                            builder = horizPanelsBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = nS0, s1 = nS1,
                            y0 = drawerY0, y1 = notchY,
                            d0 = fD0, d1 = fD1,
                            frontEdgeBuilder = frontEdgesBuilder,
                        )
                    }
                } else {
                    addWallAlignedBox(
                        builder = horizPanelsBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS0, s1 = fS1,
                        y0 = drawerY0, y1 = drawerY1,
                        d0 = fD0, d1 = fD1,
                        frontEdgeBuilder = frontEdgesBuilder,
                    )

                    // 2. Handle on front face (if enabled)
                    if (drawer.hasHandle && handleBuilder != null && fS1 > fS0 + 100f) {
                        val sMid = (fS0 + fS1) * 0.5f
                        val yMid = (drawerY0 + drawerY1) * 0.5f
                        val handleHalfLen = 65f
                        val handleHalfH = 6f
                        // Horizontal chrome handle bar
                        addWallAlignedBox(
                            builder = handleBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = sMid - handleHalfLen, s1 = sMid + handleHalfLen,
                            y0 = yMid - handleHalfH, y1 = yMid + handleHalfH,
                            d0 = fD1 + 14f, d1 = fD1 + 22f,
                        )
                        // Left mounting standoff post
                        addWallAlignedBox(
                            builder = handleBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = sMid - 55f, s1 = sMid - 45f,
                            y0 = yMid - 4f, y1 = yMid + 4f,
                            d0 = fD1, d1 = fD1 + 15f,
                        )
                        // Right mounting standoff post
                        addWallAlignedBox(
                            builder = handleBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = sMid + 45f, s1 = sMid + 55f,
                            y0 = yMid - 4f, y1 = yMid + 4f,
                            d0 = fD1, d1 = fD1 + 15f,
                        )
                    }
                }

                // 3. Drawer box (interior open body: left/right sides, back panel, bottom base)
                val boxY0 = drawerY0 + 10f
                val boxY1 = (drawerY1 - 12f).coerceAtLeast(boxY0 + 20f)
                val boxD0 = drawerD0
                val boxD1 = fD0

                if (fS1 > fS0 + 40f && boxD1 > boxD0 + 40f) {
                    // Left box side
                    addWallAlignedBox(
                        builder = vertPanelsBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS0 + 14f, s1 = fS0 + 28f,
                        y0 = boxY0, y1 = boxY1,
                        d0 = boxD0, d1 = boxD1,
                    )
                    // Right box side
                    addWallAlignedBox(
                        builder = vertPanelsBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS1 - 28f, s1 = fS1 - 14f,
                        y0 = boxY0, y1 = boxY1,
                        d0 = boxD0, d1 = boxD1,
                    )
                    // Back box panel
                    addWallAlignedBox(
                        builder = horizPanelsBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS0 + 28f, s1 = fS1 - 28f,
                        y0 = boxY0, y1 = boxY1,
                        d0 = boxD0, d1 = boxD0 + 14f,
                    )
                    // Bottom base floor panel (fondo del cajón)
                    addWallAlignedBox(
                        builder = backBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS0 + 14f, s1 = fS1 - 14f,
                        y0 = boxY0, y1 = boxY0 + 10f,
                        d0 = boxD0, d1 = boxD1,
                    )
                }

                // 4. Selection & Smart Alignment Highlight Outline (Crisp Purple Rim matching user screenshot)
                val selectedDrawer = if (selectedPartitionId != null) container.drawers.firstOrNull { it.id == selectedPartitionId } else null
                val isDrawerSelected = drawer.id == selectedPartitionId
                val isDrawerAligned = selectedDrawer != null && drawer.id != selectedDrawer.id && kotlin.math.abs(drawer.elevationYMm - selectedDrawer.elevationYMm) < 1.0f

                if ((isDrawerSelected || isDrawerAligned) && partitionOutlineBuilder != null) {
                    val pRim = 4f
                    // Front top rim
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = (fS0 - pRim).coerceAtLeast(interiorLeftS),
                        s1 = (fS1 + pRim).coerceAtMost(interiorRightS),
                        y0 = (drawerY1 - pRim).coerceAtLeast(interiorBottomY),
                        y1 = (drawerY1 + pRim).coerceAtMost(interiorTopY),
                        d0 = fD0, d1 = fD1 + 6f,
                    )
                    // Left box top edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS0 + 12f, s1 = fS0 + 30f,
                        y0 = (boxY1 - pRim).coerceAtLeast(interiorBottomY), y1 = boxY1,
                        d0 = boxD0, d1 = boxD1,
                    )
                    // Right box top edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS1 - 30f, s1 = fS1 - 12f,
                        y0 = (boxY1 - pRim).coerceAtLeast(interiorBottomY), y1 = boxY1,
                        d0 = boxD0, d1 = boxD1,
                    )
                    // Back box top edge
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = fS0 + 14f, s1 = fS1 - 14f,
                        y0 = (boxY1 - pRim).coerceAtLeast(interiorBottomY), y1 = boxY1,
                        d0 = boxD0 - 2f, d1 = boxD0 + 16f,
                    )
                }
            }
        }

        // 7d. Clothes Rails / Hanger Rods (Polished chrome tube with mounting brackets)
        for (rail in container.clothesRails) {
            val bay = bays.getOrElse(rail.bayIndex) { bays.last() }
            val railS0 = (s0Inner + bay.startXMm).coerceIn(interiorLeftS, interiorRightS)
            val railS1 = (s0Inner + bay.endXMm).coerceIn(railS0, interiorRightS)
            val railY = (interiorBottomY + rail.elevationYMm).coerceIn(interiorBottomY, interiorTopY)
            val railDepth = rail.effectiveDepth(container.interiorDepthMm)
            val railD = wallGap + backT + railDepth
            val radius = rail.diameterMm * 0.5f

            if (railS1 > railS0 + 20f && handleBuilder != null) {
                val bracketR = (radius * 1.6f).coerceIn(18f, 26f)
                val bracketThick = 8f

                // 1. Left mounting flange bracket against bay left wall
                addWallAlignedCylinder(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = railS0, s1 = railS0 + bracketThick,
                    centerY = railY, centerD = railD,
                    radiusMm = bracketR,
                    segments = 12,
                )
                // Left bracket cap facing into bay (+S)
                addWallAlignedBox(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = railS0 + bracketThick - 1f, s1 = railS0 + bracketThick,
                    y0 = railY - bracketR, y1 = railY + bracketR,
                    d0 = railD - bracketR, d1 = railD + bracketR,
                )

                // 2. Right mounting flange bracket against bay right wall
                addWallAlignedCylinder(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = railS1 - bracketThick, s1 = railS1,
                    centerY = railY, centerD = railD,
                    radiusMm = bracketR,
                    segments = 12,
                )
                // Right bracket cap facing into bay (-S)
                addWallAlignedBox(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = railS1 - bracketThick, s1 = railS1 - bracketThick + 1f,
                    y0 = railY - bracketR, y1 = railY + bracketR,
                    d0 = railD - bracketR, d1 = railD + bracketR,
                )

                // 3. Central chrome hanger rod spanning between brackets
                addWallAlignedCylinder(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = railS0 + bracketThick - 2f, s1 = railS1 - bracketThick + 2f,
                    centerY = railY, centerD = railD,
                    radiusMm = radius,
                    segments = 12,
                )

                // 4. Selection & Alignment Highlight Outline (Crisp Purple Rim matching user screenshot)
                val selectedRail = if (selectedPartitionId != null) container.clothesRails.firstOrNull { it.id == selectedPartitionId } else null
                val isRailSelected = rail.id == selectedPartitionId
                val isRailAligned = selectedRail != null && rail.id != selectedRail.id && kotlin.math.abs(rail.elevationYMm - selectedRail.elevationYMm) < 1.0f

                if ((isRailSelected || isRailAligned) && partitionOutlineBuilder != null) {
                    val pRim = 3f
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = (railS0 - pRim).coerceAtLeast(interiorLeftS),
                        s1 = (railS1 + pRim).coerceAtMost(interiorRightS),
                        y0 = (railY - radius - pRim).coerceAtLeast(interiorBottomY),
                        y1 = (railY + radius + pRim).coerceAtMost(interiorTopY),
                        d0 = railD - radius - pRim,
                        d1 = railD + radius + pRim,
                    )
                }
            }
        }

        // 7e. Vertical Poles / Posts (Polished chrome tube with top and bottom mounting brackets)
        for (pole in container.verticalPoles) {
            val bay = bays.getOrElse(pole.bayIndex) { bays.last() }
            val poleX = pole.effectiveX(bay.startXMm, bay.endXMm)
            val poleS = s0Inner + poleX
            val (compY0, compY1) = pole.effectiveYRange(0f, container.interiorHeightMm)
            val poleY0 = interiorBottomY + compY0
            val poleY1 = (interiorBottomY + compY1).coerceAtMost(interiorTopY)
            val poleDepth = pole.effectiveDepth(container.interiorDepthMm)
            val poleD = wallGap + backT + poleDepth
            val radius = pole.diameterMm * 0.5f

            if (poleY1 > poleY0 + 20f && handleBuilder != null) {
                val bracketR = (radius * 1.6f).coerceIn(18f, 26f)
                val bracketThick = 8f

                // 1. Bottom mounting flange bracket on floor or lower shelf
                addVerticalCylinder(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    y0 = poleY0, y1 = poleY0 + bracketThick,
                    centerS = poleS, centerD = poleD,
                    radiusMm = bracketR,
                    segments = 12,
                )
                // Bottom bracket cap facing +Y
                addWallAlignedBox(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = poleS - bracketR, s1 = poleS + bracketR,
                    y0 = poleY0 + bracketThick - 1f, y1 = poleY0 + bracketThick,
                    d0 = poleD - bracketR, d1 = poleD + bracketR,
                )

                // 2. Top mounting flange bracket against underside of roof or upper shelf
                addVerticalCylinder(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    y0 = poleY1 - bracketThick, y1 = poleY1,
                    centerS = poleS, centerD = poleD,
                    radiusMm = bracketR,
                    segments = 12,
                )
                // Top bracket cap facing -Y
                addWallAlignedBox(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = poleS - bracketR, s1 = poleS + bracketR,
                    y0 = poleY1 - bracketThick, y1 = poleY1 - bracketThick + 1f,
                    d0 = poleD - bracketR, d1 = poleD + bracketR,
                )

                // 3. Central chrome post spanning between brackets
                addVerticalCylinder(
                    builder = handleBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    y0 = poleY0 + bracketThick - 2f, y1 = poleY1 - bracketThick + 2f,
                    centerS = poleS, centerD = poleD,
                    radiusMm = radius,
                    segments = 12,
                )

                // 4. Selection Highlight Outline (Crisp Purple Box)
                val selectedPole = if (selectedPartitionId != null) container.verticalPoles.firstOrNull { it.id == selectedPartitionId } else null
                val isPoleSelected = pole.id == selectedPartitionId
                val isPoleAligned = selectedPole != null && pole.id != selectedPole.id && kotlin.math.abs(poleX - selectedPole.effectiveX(bay.startXMm, bay.endXMm)) < 1.0f

                if ((isPoleSelected || isPoleAligned) && partitionOutlineBuilder != null) {
                    val pRim = 3f
                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = (poleS - radius - pRim).coerceAtLeast(interiorLeftS),
                        s1 = (poleS + radius + pRim).coerceAtMost(interiorRightS),
                        y0 = (poleY0 - pRim).coerceAtLeast(interiorBottomY),
                        y1 = (poleY1 + pRim).coerceAtMost(interiorTopY),
                        d0 = poleD - radius - pRim,
                        d1 = poleD + radius + pRim,
                    )
                }
            }
        }

        // 7f. Inclined Shoe Racks with Solid Wood Retaining Lip
        for (rack in container.shoeRacks) {
            val bay = bays.getOrElse(rack.bayIndex) { bays.last() }
            val rackS0 = (s0Inner + bay.startXMm).coerceIn(interiorLeftS, interiorRightS)
            val rackS1 = (s0Inner + bay.endXMm).coerceIn(rackS0, interiorRightS)
            if (rackS1 <= rackS0) continue

            val d0 = wallGap + backT
            val effDepth = rack.effectiveDepth(container.interiorDepthMm)
            val d1 = d0 + effDepth
            val lipThick = rack.lipThicknessMm.coerceIn(12f, 30f)
            val dSlanted1 = (d1 - lipThick).coerceAtLeast(d0 + 50f)

            val rad = Math.toRadians(rack.inclinationAngleDeg.toDouble()).toFloat()
            val vertThick = rack.thicknessMm / kotlin.math.cos(rad).coerceAtLeast(0.1f)
            val deltaY = (dSlanted1 - d0) * kotlin.math.tan(rad)

            val yFront0 = (interiorBottomY + rack.elevationYMm).coerceIn(interiorBottomY, (interiorTopY - 10f).coerceAtLeast(interiorBottomY))
            val yFront1 = (yFront0 + vertThick).coerceAtMost(interiorTopY)
            val yBack0 = (yFront0 + deltaY).coerceIn(interiorBottomY, (interiorTopY - 10f).coerceAtLeast(interiorBottomY))
            val yBack1 = (yBack0 + vertThick).coerceAtMost(interiorTopY)

            // 1. Slanted melamina shelf slab
            addWallAlignedInclinedBox(
                builder = horizPanelsBuilder,
                pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                s0 = rackS0, s1 = rackS1,
                yFront0 = yFront0, yFront1 = yFront1,
                yBack0 = yBack0, yBack1 = yBack1,
                d0 = d0, d1 = dSlanted1,
                frontEdgeBuilder = frontEdgesBuilder,
            )

            // 2. Solid Wood Front Retaining Lip (Zócalo / tope frontal de melamina sólida que retiene los zapatos)
            val lipY0 = yFront0
            val lipY1 = (yFront1 + rack.lipHeightMm).coerceAtMost(interiorTopY)
            if (lipY1 > lipY0) {
                addWallAlignedBox(
                    builder = horizPanelsBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = rackS0, s1 = rackS1,
                    y0 = lipY0, y1 = lipY1,
                    d0 = dSlanted1, d1 = d1,
                    frontEdgeBuilder = frontEdgesBuilder,
                )
            }

            // 3. Selection & Smart Alignment Highlight Outline (Crisp Purple Box)
            val selectedRack = if (selectedPartitionId != null) container.shoeRacks.firstOrNull { it.id == selectedPartitionId } else null
            val isRackSelected = rack.id == selectedPartitionId
            val isRackAligned = selectedRack != null && rack.id != selectedRack.id && kotlin.math.abs(rack.elevationYMm - selectedRack.elevationYMm) < 1.0f

            if ((isRackSelected || isRackAligned) && partitionOutlineBuilder != null) {
                val pRim = 4f
                val dOut0 = d1 - 2f
                val dOut1 = d1 + 6f
                val outS0 = (rackS0 - pRim).coerceAtLeast(interiorLeftS)
                val outS1 = (rackS1 + pRim).coerceAtMost(interiorRightS)
                val outY0 = (lipY0 - pRim).coerceAtLeast(interiorBottomY)
                val outY1 = (lipY1 + pRim).coerceAtMost(interiorTopY)

                addWallAlignedBox(
                    builder = partitionOutlineBuilder,
                    pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                    s0 = outS0, s1 = outS1,
                    y0 = outY0, y1 = outY1,
                    d0 = dOut0, d1 = dOut1,
                )
            }
        }

        // 7g. Cabinet Doors (Puertas de armario: 1 o 2 hojas frontales con tiradores cromados verticales)
        if (showDoors) {
            for (doors in container.cabinetDoors) {
                val bay = bays.getOrElse(doors.bayIndex) { bays.last() }
                val bayS0 = (s0Inner + bay.startXMm).coerceIn(interiorLeftS, interiorRightS)
                val bayS1 = (s0Inner + bay.endXMm).coerceIn(bayS0, interiorRightS)
                if (bayS1 <= bayS0 + 20f) continue

                val doorY0 = (interiorBottomY + doors.elevationYMm).coerceIn(interiorBottomY, (interiorTopY - 10f).coerceAtLeast(interiorBottomY))
                val doorY1 = (doorY0 + doors.heightMm).coerceAtMost(interiorTopY)
                if (doorY1 <= doorY0 + 20f) continue

                val d0 = (wallGap + totalD - doors.thicknessMm).coerceAtLeast(0f)
                val d1 = wallGap + totalD

                val leaves = if (doors.doorCount == 1) {
                    listOf(bayS0 to bayS1)
                } else {
                    val gap = doors.doorGapMm.coerceIn(1f, 8f)
                    val leafW = ((bayS1 - bayS0) - gap) * 0.5f
                    if (leafW > 10f) {
                        listOf(
                            bayS0 to (bayS0 + leafW),
                            (bayS1 - leafW) to bayS1,
                        )
                    } else {
                        listOf(bayS0 to bayS1)
                    }
                }

                for ((leafS0, leafS1) in leaves) {
                    addWallAlignedBox(
                        builder = vertPanelsBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = leafS0, s1 = leafS1,
                        y0 = doorY0, y1 = doorY1,
                        d0 = d0, d1 = d1,
                        frontEdgeBuilder = frontEdgesBuilder,
                    )
                }

                // Handles (Tiradores verticales cromados tipo barra)
                if (doors.hasHandles && handleBuilder != null) {
                    val hLen = doors.handleLengthMm.coerceIn(60f, (doorY1 - doorY0 - 40f).coerceAtLeast(60f))
                    val yMid = (doorY0 + doorY1) * 0.5f
                    val hY0 = yMid - hLen * 0.5f
                    val hY1 = yMid + hLen * 0.5f
                    val hBarD0 = d1 + 18f
                    val hBarD1 = d1 + 28f

                    val handlePositions = if (doors.doorCount == 1) {
                        listOf(bayS1 - 35f)
                    } else {
                        val gap = doors.doorGapMm.coerceIn(1f, 8f)
                        val leafW = ((bayS1 - bayS0) - gap) * 0.5f
                        listOf(
                            (bayS0 + leafW - 32f), // Left door handle near right edge
                            (bayS1 - leafW + 32f), // Right door handle near left edge
                        )
                    }

                    for (hS in handlePositions) {
                        val hs0 = hS - 5f
                        val hs1 = hS + 5f
                        // Main vertical bar
                        addWallAlignedBox(
                            builder = handleBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = hs0, s1 = hs1,
                            y0 = hY0, y1 = hY1,
                            d0 = hBarD0, d1 = hBarD1,
                        )
                        // Top standoff post
                        addWallAlignedBox(
                            builder = handleBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = hs0 + 1f, s1 = hs1 - 1f,
                            y0 = hY1 - 12f, y1 = hY1 - 2f,
                            d0 = d1, d1 = hBarD0,
                        )
                        // Bottom standoff post
                        addWallAlignedBox(
                            builder = handleBuilder,
                            pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                            s0 = hs0 + 1f, s1 = hs1 - 1f,
                            y0 = hY0 + 2f, y1 = hY0 + 12f,
                            d0 = d1, d1 = hBarD0,
                        )
                    }
                }

                // Selection & Smart Alignment Highlight Outline (Crisp Purple Box)
                val selectedDoors = if (selectedPartitionId != null) container.cabinetDoors.firstOrNull { it.id == selectedPartitionId } else null
                val isDoorsSelected = doors.id == selectedPartitionId
                val isDoorsAligned = selectedDoors != null && doors.id != selectedDoors.id && kotlin.math.abs(doors.elevationYMm - selectedDoors.elevationYMm) < 1.0f

                if ((isDoorsSelected || isDoorsAligned) && partitionOutlineBuilder != null) {
                    val pRim = 4f
                    val dOut0 = d1 - 2f
                    val dOut1 = d1 + 6f
                    val outS0 = (bayS0 - pRim).coerceAtLeast(interiorLeftS)
                    val outS1 = (bayS1 + pRim).coerceAtMost(interiorRightS)
                    val outY0 = (doorY0 - pRim).coerceAtLeast(interiorBottomY)
                    val outY1 = (doorY1 + pRim).coerceAtMost(interiorTopY)

                    addWallAlignedBox(
                        builder = partitionOutlineBuilder,
                        pStart = pStart, ux = ux, uz = uz, nx = nx, nz = nz,
                        s0 = outS0, s1 = outS1,
                        y0 = outY0, y1 = outY1,
                        d0 = dOut0, d1 = dOut1,
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
