package com.closetstudio.core.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
data class SavedProject(
    val id: String,
    val name: String,
    val updatedAtEpochMs: Long,
    val room: RoomSpec = RoomSpec(),
)

enum class WallIndex(val degrees: Float, val label: String) {
    NORTH(0f, "Pared B"),
    EAST(90f, "Pared C"),
    SOUTH(180f, "Pared E - Puerta - D"),
    WEST(270f, "Pared A");

    fun next(): WallIndex = values()[(ordinal + 1) % values().size]
    fun prev(): WallIndex = values()[(ordinal - 1 + values().size) % values().size]
}

@Serializable
data class Vec2Mm(
    val x: Float,
    val z: Float,
)

@Serializable
data class ClosetContainerSpec(
    val id: String,
    val wall: WallIndex = WallIndex.NORTH,
    val offsetMm: Float = 900f,
    val widthMm: Float = 1800f,
    val heightMm: Float = 2200f,
    val depthMm: Float = 600f,
    val panelThicknessMm: Float = 18f,
    val plinthHeightMm: Float = 80f,
    val backThicknessMm: Float = 6f,
) {
    init {
        require(widthMm >= 0f) { "container width must be non-negative" }
        require(heightMm >= 0f) { "container height must be non-negative" }
        require(depthMm >= 0f) { "container depth must be non-negative" }
    }
}

@Serializable
data class DoorSpec(
    val wall: WallIndex = WallIndex.SOUTH,
    val offsetMm: Float = 1350f,
    val widthMm: Float = 900f,
    val heightMm: Float = 2100f,
    val frameThicknessMm: Float = 40f,
    val lockWidth: Boolean = true,
) {
    init {
        require(offsetMm >= 0f) { "door offset must be non-negative" }
        require(widthMm >= 0f) { "door width must be non-negative" }
        require(heightMm >= 0f) { "door height must be non-negative" }
    }
}

/**
 * Architectural specification of a 4-walled enclosed room supporting independent
 * wall lengths, corner angles, and parametric constraints (locks).
 *
 * All coordinates in millimetres:
 * - clearWidthMm: Wall A length (North / Front)
 * - clearHeightMm: Y dimension (Floor to Ceiling)
 * - clearDepthMm: Wall C length (West / Left)
 * - wallBMm: Wall B length (East / Right)
 * - angleACDeg: Interior angle at North-West corner (between Wall A and Wall C)
 * - angleABDeg: Interior angle at North-East corner (between Wall A and Wall B)
 */
@Serializable
data class RoomSpec(
    val clearWidthMm: Float = 3600f,
    val clearHeightMm: Float = 2600f,
    val clearDepthMm: Float = 3600f,
    val wallBMm: Float = clearDepthMm,
    val angleACDeg: Float = 90f,
    val angleABDeg: Float = 90f,
    val lockWallA: Boolean = false,
    val lockWallB: Boolean = false,
    val lockWallC: Boolean = false,
    val lockWallSouth: Boolean = false,
    val lockSegmentD: Boolean = false,
    val lockSegmentE: Boolean = false,
    val lockAngleAC: Boolean = true,
    val lockAngleAB: Boolean = true,
    val lockAngleBE: Boolean = false,
    val lockAngleCD: Boolean = false,
    val door: DoorSpec = DoorSpec(),
    val containers: List<ClosetContainerSpec> = emptyList(),
) {
    init {
        require(clearWidthMm >= 0f) { "Room width must be non-negative, got $clearWidthMm" }
        require(clearHeightMm >= 0f) { "Room height must be non-negative, got $clearHeightMm" }
        require(clearDepthMm >= 0f) { "Room depth must be non-negative, got $clearDepthMm" }
        require(wallBMm >= 0f) { "Wall B must be non-negative, got $wallBMm" }
    }

    val wallAMm: Float get() = clearWidthMm
    val wallCMm: Float get() = clearDepthMm

    /** North-West corner (A-C) at origin (0, 0). */
    val vertexNW: Vec2Mm get() = Vec2Mm(0f, 0f)

    /** North-East corner (A-B) at (wallAMm, 0). */
    val vertexNE: Vec2Mm get() = Vec2Mm(wallAMm, 0f)

    /** South-West corner (C-D) computed from Wall C length and angle A-C. */
    val vertexSW: Vec2Mm
        get() {
            val rad = Math.toRadians(angleACDeg.toDouble()).toFloat()
            return Vec2Mm(
                x = wallCMm * cos(rad),
                z = wallCMm * sin(rad),
            )
        }

    /** South-East corner (B-E) computed from Wall B length and angle A-B. */
    val vertexSE: Vec2Mm
        get() {
            val rad = Math.toRadians(angleABDeg.toDouble()).toFloat()
            return Vec2Mm(
                x = wallAMm - wallBMm * cos(rad),
                z = wallBMm * sin(rad),
            )
        }

    /** Resulting total length of the South Wall (Door wall: D + Door + E). */
    val wallSouthMm: Float
        get() {
            val sw = vertexSW
            val se = vertexSE
            val dx = se.x - sw.x
            val dz = se.z - sw.z
            return sqrt(dx * dx + dz * dz).coerceAtLeast(0f)
        }

    /** Resulting interior angle (in degrees) at the South-West corner (between Wall C and Segment D). */
    val angleCDDeg: Float
        get() {
            val sw = vertexSW
            val se = vertexSE
            val ux = -sw.x
            val uz = -sw.z
            val vx = se.x - sw.x
            val vz = se.z - sw.z
            val uLen = sqrt(ux * ux + uz * uz).coerceAtLeast(1e-4f)
            val vLen = sqrt(vx * vx + vz * vz).coerceAtLeast(1e-4f)
            val cosTheta = ((ux * vx + uz * vz) / (uLen * vLen)).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
        }

    /** Resulting interior angle (in degrees) at the South-East corner (between Wall B and Segment E). */
    val angleBEDeg: Float
        get() {
            val ne = vertexNE
            val se = vertexSE
            val sw = vertexSW
            val ux = ne.x - se.x
            val uz = ne.z - se.z
            val vx = sw.x - se.x
            val vz = sw.z - se.z
            val uLen = sqrt(ux * ux + uz * uz).coerceAtLeast(1e-4f)
            val vLen = sqrt(vx * vx + vz * vz).coerceAtLeast(1e-4f)
            val cosTheta = ((ux * vx + uz * vz) / (uLen * vLen)).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
        }

    /** Maximum offset allowed for the door on the specified wall. */
    fun maxDoorOffset(): Float {
        val wallLength = doorWallLengthMm
        return (wallLength - door.widthMm).coerceAtLeast(0f)
    }

    /** Total length of the wall containing the door. */
    val doorWallLengthMm: Float
        get() = when (door.wall) {
            WallIndex.SOUTH -> wallSouthMm
            WallIndex.NORTH -> wallAMm
            WallIndex.EAST -> wallBMm
            WallIndex.WEST -> wallCMm
        }

    /** Length of segment D (wall segment before the door). */
    val segmentDMm: Float
        get() = door.offsetMm.coerceIn(0f, maxDoorOffset())

    /** Length of segment E (wall segment after the door). */
    val segmentEMm: Float
        get() = (doorWallLengthMm - segmentDMm - door.widthMm).coerceAtLeast(0f)

    /** Returns true if the door is centered within 1mm tolerance. */
    val isDoorCentered: Boolean
        get() = abs(segmentDMm - segmentEMm) < 1f

    /**
     * Returns true if the South (Door) wall total length is locked, either explicitly (`lockWallSouth`)
     * or implicitly because all 3 of its segments (`Tramo E`, `Puerta`, and `Tramo D`) are locked.
     */
    val isSouthWallTotalLocked: Boolean
        get() = lockWallSouth || (lockSegmentD && door.lockWidth && lockSegmentE)

    /** Centers the door between segment D and E. */
    fun withCenteredDoor(): RoomSpec {
        if (lockSegmentD || lockSegmentE || lockWallSouth) return this
        val centered = ((doorWallLengthMm - door.widthMm) / 2f).coerceAtLeast(0f)
        return copy(door = door.copy(offsetMm = centered))
    }

    /**
     * Adapts the door opening and segments D/E when the room polygon changes:
     * - If `door.lockWidth == true` (default): `door.widthMm` stays strictly invariant (`🔒`)
     *   and does NOT grow or shrink with the walls; only segments D and E absorb the wall length change.
     * - If `door.lockWidth == false` (`🔓`): `door.widthMm` scales proportionally with the door wall length.
     * - If `lockSegmentD == true`: `segmentDMm` (UI Tramo E) stays fixed while `segmentEMm` absorbs the change.
     * - If `lockSegmentE == true`: `segmentEMm` (UI Tramo D) stays fixed while `segmentDMm` absorbs the change.
     */
    private fun applyDoorAdaptation(
        keepCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec {
        val currentWallLen = doorWallLengthMm.coerceAtLeast(0f)
        val prevSegmentEMm = (previousWallLengthMm - door.offsetMm - door.widthMm).coerceAtLeast(0f)
        val updatedWidth = if (door.lockWidth || previousWallLengthMm <= 1f) {
            door.widthMm
        } else {
            val ratio = currentWallLen / previousWallLengthMm
            ((door.widthMm * ratio / 10f).let { kotlin.math.round(it) * 10f })
                .coerceIn(0f, currentWallLen)
        }
        val maxOffset = (currentWallLen - updatedWidth).coerceAtLeast(0f)
        val updatedOffset = when {
            lockSegmentD && !lockSegmentE -> door.offsetMm.coerceIn(0f, maxOffset)
            lockSegmentE && !lockSegmentD -> (currentWallLen - updatedWidth - prevSegmentEMm).coerceIn(0f, maxOffset)
            keepCentered -> maxOffset / 2f
            else -> door.offsetMm.coerceIn(0f, maxOffset)
        }
        return copy(
            door = door.copy(widthMm = updatedWidth, offsetMm = updatedOffset),
            containers = containers.map { clampContainer(it) },
        )
    }

    /** Resets the room to orthogonal 90° corners with B = C and A = South. */
    fun withOrthogonalReset(keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        val prevDoorWallLen = doorWallLengthMm
        return copy(
            wallBMm = clearDepthMm,
            angleACDeg = 90f,
            angleABDeg = 90f,
        ).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
    }

    /**
     * Adjusts an unlocked wall or unlocked corner so that `wallSouthMm` equals [targetSouthMm],
     * strictly preserving any locked walls (`lockWallA`, `lockWallB`, `lockWallC`) and locked corners
     * (`lockAngleAC`, `lockAngleAB`). If the target is smaller than the geometric minimum imposed by
     * locked walls/angles (e.g. `Pared B` and top 90° angles are locked), snaps to the minimum possible
     * geometric length and stops decreasing so the 2D drawing and door scale never distort.
     */
    private fun solvePolygonForTargetSouth(
        targetSouthMm: Float,
        keepDoorCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec? {
        val clampedS = targetSouthMm.coerceAtLeast(0f)
        val sw = vertexSW
        val se = vertexSE
        val radAB = Math.toRadians(angleABDeg.toDouble()).toFloat()
        val radAC = Math.toRadians(angleACDeg.toDouble()).toFloat()

        // 1. If Wall A (UI Pared B - Top) is unlocked, adjust clearWidthMm directly
        if (!lockWallA) {
            val dz = se.z - sw.z
            val remSq = clampedS * clampedS - dz * dz
            if (remSq >= 0f) {
                val dx = sqrt(remSq)
                val newA = (sw.x + dx + wallBMm * cos(radAB)).coerceAtLeast(0f)
                return copy(clearWidthMm = newA)
                    .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
            }
            if (!lockWallB) {
                val minA = (sw.x + clampedS).coerceAtLeast(0f)
                val alignedB = if (sin(radAB) > 1e-4f) (sw.z / sin(radAB)).coerceAtLeast(0f) else wallBMm
                return copy(clearWidthMm = minA, wallBMm = alignedB)
                    .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
            }
            val minA = (sw.x + wallBMm * cos(radAB)).coerceAtLeast(0f)
            return copy(clearWidthMm = minA)
                .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
        }

        // 2. If Angle A-B (UI Corner B-C) is unlocked, tilt Wall B so vertexSE is at distance clampedS from vertexSW
        if (!lockAngleAB) {
            val dxN = wallAMm - sw.x
            val distN = hypot(dxN.toDouble(), sw.z.toDouble()).toFloat().coerceAtLeast(1f)
            val cosPhi = ((wallBMm * wallBMm + distN * distN - clampedS * clampedS) / (2f * (wallBMm * distN).coerceAtLeast(1f)))
            if (cosPhi in -0.995f..0.995f) {
                val phi = acos(cosPhi.toDouble()).toFloat()
                val alpha = kotlin.math.atan2(sw.z.toDouble(), dxN.toDouble()).toFloat()
                val deg1 = Math.toDegrees((alpha - phi).toDouble()).toFloat()
                val deg2 = Math.toDegrees((alpha + phi).toDouble()).toFloat()
                val candidates = listOf(deg1, deg2).filter { it in 5f..175f }
                val bestDeg = candidates.minByOrNull { abs(it - angleABDeg) }
                if (bestDeg != null) {
                    return copy(angleABDeg = bestDeg)
                        .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
                }
            }
        }

        // 3. If Angle A-C (UI Corner A-B) is unlocked, tilt Wall C so vertexSW is at distance clampedS from vertexSE
        if (!lockAngleAC) {
            val distSE = hypot(se.x.toDouble(), se.z.toDouble()).toFloat().coerceAtLeast(1f)
            val cosPhi = ((wallCMm * wallCMm + distSE * distSE - clampedS * clampedS) / (2f * (wallCMm * distSE).coerceAtLeast(1f)))
            if (cosPhi in -0.995f..0.995f) {
                val phi = acos(cosPhi.toDouble()).toFloat()
                val alpha = kotlin.math.atan2(se.z.toDouble(), se.x.toDouble()).toFloat()
                val deg1 = Math.toDegrees((alpha - phi).toDouble()).toFloat()
                val deg2 = Math.toDegrees((alpha + phi).toDouble()).toFloat()
                val candidates = listOf(deg1, deg2).filter { it in 5f..175f }
                val bestDeg = candidates.minByOrNull { abs(it - angleACDeg) }
                if (bestDeg != null) {
                    return copy(angleACDeg = bestDeg)
                        .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
                }
            }
        }

        // 4. If Wall B (UI Pared C - Right) is unlocked
        if (!lockWallB) {
            val ux = -cos(radAB)
            val uz = sin(radAB)
            val rx = wallAMm - sw.x
            val rz = -sw.z
            val bProj = rx * ux + rz * uz
            val cVal = rx * rx + rz * rz - clampedS * clampedS
            val disc = bProj * bProj - cVal
            if (disc >= 0f) {
                val sqrtDisc = sqrt(disc)
                val candidates = listOf(-bProj + sqrtDisc, -bProj - sqrtDisc).filter { it >= 0f }
                val bestB = candidates.minByOrNull { abs(it - wallBMm) }
                if (bestB != null) {
                    return copy(wallBMm = bestB)
                        .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
                }
            }
            // Geometrically cannot shrink below the perpendicular distance |rx| because Pared B and its angles are locked:
            // snap Wall B to the orthogonal projection (-bProj) where South wall reaches its exact geometric minimum, and stop there.
            val snappedB = (-bProj).coerceAtLeast(0f)
            return copy(wallBMm = snappedB)
                .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
        }

        // 5. If Wall C (UI Pared A - Left) is unlocked
        if (!lockWallC) {
            val ux = cos(radAC)
            val uz = sin(radAC)
            val rx = -se.x
            val rz = -se.z
            val bProj = rx * ux + rz * uz
            val cVal = rx * rx + rz * rz - clampedS * clampedS
            val disc = bProj * bProj - cVal
            if (disc >= 0f) {
                val sqrtDisc = sqrt(disc)
                val candidates = listOf(-bProj + sqrtDisc, -bProj - sqrtDisc).filter { it >= 0f }
                val bestC = candidates.minByOrNull { abs(it - wallCMm) }
                if (bestC != null) {
                    return copy(clearDepthMm = bestC)
                        .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
                }
            }
            val snappedC = (-bProj).coerceAtLeast(0f)
            return copy(clearDepthMm = snappedC)
                .applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
        }

        return null
    }

    /** Updates Wall A (North), propagating constraints if South wall or bottom corners are locked. */
    fun withWallA(newAMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallA) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedA = newAMm.coerceAtLeast(0f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val targetSouth = wallSouthMm
        val candidate = copy(clearWidthMm = clampedA)
        return when {
            isSouthWallTotalLocked -> {
                candidate.copy(lockWallA = true).solvePolygonForTargetSouth(targetSouth, keepDoorCentered, prevDoorWallLen)
                    ?.copy(lockWallA = lockWallA)
                    ?: this
            }
            lockAngleCD || lockAngleBE -> {
                val targetCD = if (lockAngleCD) oldCD else (360f - candidate.angleACDeg - candidate.angleABDeg - oldBE)
                candidate.solveWallForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen) ?: this
            }
            else -> candidate.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
        }
    }

    /** Updates Wall B (East) independently, adapting unlocked walls/angles to close polygon. */
    fun withWallB(newBMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallB) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedB = newBMm.coerceAtLeast(0f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val targetSouth = wallSouthMm
        val candidate = copy(wallBMm = clampedB)
        return when {
            isSouthWallTotalLocked -> {
                candidate.copy(lockWallB = true).solvePolygonForTargetSouth(targetSouth, keepDoorCentered, prevDoorWallLen)
                    ?.copy(lockWallB = lockWallB)
                    ?: this
            }
            lockAngleBE || lockAngleCD -> {
                if (lockWallC) {
                    this
                } else {
                    val targetBE = if (lockAngleBE) oldBE else (360f - candidate.angleACDeg - candidate.angleABDeg - oldCD)
                    candidate.solveCForTargetBE(targetBE, keepDoorCentered, prevDoorWallLen)
                }
            }
            else -> candidate.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
        }
    }

    /** Updates Wall C (West) independently, adapting unlocked walls/angles to close polygon. */
    fun withWallC(newCMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallC) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedC = newCMm.coerceAtLeast(0f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val targetSouth = wallSouthMm
        val candidate = copy(clearDepthMm = clampedC)
        return when {
            isSouthWallTotalLocked -> {
                candidate.copy(lockWallC = true).solvePolygonForTargetSouth(targetSouth, keepDoorCentered, prevDoorWallLen)
                    ?.copy(lockWallC = lockWallC)
                    ?: this
            }
            lockAngleCD || lockAngleBE -> {
                if (lockWallB) {
                    this
                } else {
                    val targetCD = if (lockAngleCD) oldCD else (360f - candidate.angleACDeg - candidate.angleABDeg - oldBE)
                    candidate.solveBForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
                }
            }
            else -> candidate.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
        }
    }

    /** Directly updates the total length of the South Wall (Door wall), adapting unlocked walls or angles. */
    fun withWallSouth(targetSouthMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (isSouthWallTotalLocked) return this
        return solvePolygonForTargetSouth(targetSouthMm, keepDoorCentered, doorWallLengthMm) ?: this
    }

    /**
     * Routes polygon angle closure to whichever side wall (`Wall B` or `Wall C`) is unlocked,
     * strictly preserving locked wall lengths (`lockWallB` / `lockWallC`).
     * Returns `null` if both `lockWallB` and `lockWallC` are locked.
     */
    private fun solveWallForTargetCD(
        targetCDDeg: Float,
        keepDoorCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec? {
        return when {
            !lockWallB -> solveBForTargetCD(targetCDDeg, keepDoorCentered, previousWallLengthMm)
            !lockWallC -> {
                val targetBE = 360f - angleACDeg - angleABDeg - targetCDDeg
                solveCForTargetBE(targetBE, keepDoorCentered, previousWallLengthMm)
            }
            else -> null
        }
    }

    /**
     * Intersects the South wall ray originating from `vertexSW` at interior angle [targetCDDeg]
     * with the Wall B ray originating from `vertexNE` at interior angle [angleABDeg],
     * adjusting ONLY `wallBMm` so that `angleACDeg`, `angleABDeg`, and `angleCDDeg` are exact,
     * and the remaining corner `angleBEDeg = 360° - (AC + AB + CD)`.
     */
    private fun solveBForTargetCD(
        targetCDDeg: Float,
        keepDoorCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec {
        val clampedCD = targetCDDeg.coerceIn(45f, 135f)
        val sw = vertexSW
        val ne = vertexNE
        val radAC = Math.toRadians(angleACDeg.toDouble()).toFloat()
        val radCD = Math.toRadians(clampedCD.toDouble()).toFloat()
        val radAB = Math.toRadians(angleABDeg.toDouble()).toFloat()

        // Unit vector along Wall C from V3 (SW) to V0 (NW)
        val ux = -cos(radAC)
        val uz = -sin(radAC)

        // Unit direction of South wall from V3 (SW) to V2 (SE)
        val dSurX = -uz * sin(radCD) + ux * cos(radCD)
        val dSurZ = ux * sin(radCD) + uz * cos(radCD)

        // Unit direction of Wall B from V1 (NE) to V2 (SE)
        val dBX = -cos(radAB)
        val dBZ = sin(radAB)

        val denom = dBZ * dSurX - dBX * dSurZ
        if (abs(denom) > 1e-4f) {
            val newB = (((sw.z - ne.z) * dSurX - (sw.x - ne.x) * dSurZ) / denom).coerceAtLeast(0f)
            return copy(wallBMm = newB).applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
        }
        return applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
    }

    /**
     * Intersects the South wall ray originating from `vertexSE` at interior angle [targetBEDeg]
     * with the Wall C ray originating from `vertexNW` at interior angle [angleACDeg],
     * adjusting ONLY `clearDepthMm` (Wall C) so that `angleACDeg`, `angleABDeg`, and `angleBEDeg` are exact,
     * and the remaining corner `angleCDDeg = 360° - (AC + AB + BE)`.
     */
    private fun solveCForTargetBE(
        targetBEDeg: Float,
        keepDoorCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec {
        val clampedBE = targetBEDeg.coerceIn(45f, 135f)
        val se = vertexSE
        val nw = vertexNW
        val radAB = Math.toRadians(angleABDeg.toDouble()).toFloat()
        val radBE = Math.toRadians(clampedBE.toDouble()).toFloat()
        val radAC = Math.toRadians(angleACDeg.toDouble()).toFloat()

        // Unit vector along Wall B from V2 (SE) to V1 (NE)
        val ux = cos(radAB)
        val uz = -sin(radAB)

        // Unit direction of South wall from V2 (SE) to V3 (SW)
        val dSurX = uz * sin(radBE) + ux * cos(radBE)
        val dSurZ = -ux * sin(radBE) + uz * cos(radBE)

        // Unit direction of Wall C from V0 (NW) to V3 (SW)
        val dCX = cos(radAC)
        val dCZ = sin(radAC)

        val denom = dCZ * dSurX - dCX * dSurZ
        if (abs(denom) > 1e-4f) {
            val newC = (((se.z - nw.z) * dSurX - (se.x - nw.x) * dSurZ) / denom).coerceAtLeast(0f)
            return copy(clearDepthMm = newC).applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
        }
        return applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
    }

    /**
     * Updates Corner A-C angle (North-West), strictly preserving any locked corners (`lockAngleAB`,
     * `lockAngleCD`, `lockAngleBE`) and locked walls (`lockWallA`, `lockWallB`, `lockWallC`, `isSouthWallTotalLocked`).
     */
    fun withAngleAC(newAngleDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockAngleAC) return this
        val prevDoorWallLen = doorWallLengthMm
        val clamped = newAngleDeg.coerceIn(65f, 115f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val delta = clamped - angleACDeg

        return when {
            // If both bottom corners are locked, unlocked Angle A-B must absorb -delta so CD and BE stay fixed
            lockAngleCD && lockAngleBE -> {
                if (!lockAngleAB) {
                    val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                    copy(angleACDeg = clamped, angleABDeg = newAB)
                        .solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                        ?: this
                } else {
                    // All other 3 corners are locked -> geometrically impossible to change 1 angle in a quad
                    this
                }
            }
            // Corner C-D is locked -> preserve oldCD and let unlocked Corner B-E (or A-B) absorb the change
            lockAngleCD -> {
                copy(angleACDeg = clamped).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                    ?: if (!lockAngleAB) {
                        val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                        copy(angleACDeg = clamped, angleABDeg = newAB).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
                    } else {
                        this
                    }
            }
            // Corner B-E is locked -> preserve oldBE and let unlocked Corner C-D absorb the change
            lockAngleBE -> {
                val targetCD = 360f - clamped - angleABDeg - oldBE
                copy(angleACDeg = clamped).solveWallForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
                    ?: if (!lockAngleAB) {
                        val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                        copy(angleACDeg = clamped, angleABDeg = newAB).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
                    } else {
                        this
                    }
            }
            // Both C-D and B-E are unlocked -> keep Wall C & B lengths or adapt South if locked
            else -> {
                val targetSouth = wallSouthMm
                val next = copy(angleACDeg = clamped)
                if (isSouthWallTotalLocked) {
                    next.copy(lockAngleAC = true).solvePolygonForTargetSouth(targetSouth, keepDoorCentered, prevDoorWallLen)
                        ?.copy(lockAngleAC = lockAngleAC)
                        ?: this
                } else {
                    next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
                }
            }
        }
    }

    /**
     * Updates Corner A-B angle (North-East), strictly preserving any locked corners (`lockAngleAC`,
     * `lockAngleCD`, `lockAngleBE`) and locked walls (`lockWallA`, `lockWallB`, `lockWallC`, `isSouthWallTotalLocked`).
     */
    fun withAngleAB(newAngleDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockAngleAB) return this
        val prevDoorWallLen = doorWallLengthMm
        val clamped = newAngleDeg.coerceIn(65f, 115f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val delta = clamped - angleABDeg

        return when {
            // If both bottom corners are locked, unlocked Angle A-C must absorb -delta so CD and BE stay fixed
            lockAngleCD && lockAngleBE -> {
                if (!lockAngleAC) {
                    val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                    copy(angleABDeg = clamped, angleACDeg = newAC)
                        .solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                        ?: this
                } else {
                    this
                }
            }
            // Corner B-E is locked -> preserve oldBE and let unlocked Corner C-D absorb the change
            lockAngleBE -> {
                val targetCD = 360f - angleACDeg - clamped - oldBE
                copy(angleABDeg = clamped).solveWallForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
                    ?: if (!lockAngleAC) {
                        val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                        copy(angleABDeg = clamped, angleACDeg = newAC).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
                    } else {
                        this
                    }
            }
            // Corner C-D is locked -> preserve oldCD and let unlocked Corner B-E absorb the change
            lockAngleCD -> {
                copy(angleABDeg = clamped).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                    ?: if (!lockAngleAC) {
                        val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                        copy(angleABDeg = clamped, angleACDeg = newAC).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
                    } else {
                        this
                    }
            }
            // Both C-D and B-E are unlocked
            else -> {
                val targetSouth = wallSouthMm
                val next = copy(angleABDeg = clamped)
                if (isSouthWallTotalLocked) {
                    next.copy(lockAngleAB = true).solvePolygonForTargetSouth(targetSouth, keepDoorCentered, prevDoorWallLen)
                        ?.copy(lockAngleAB = lockAngleAB)
                        ?: this
                } else {
                    next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
                }
            }
        }
    }

    /**
     * Updates Corner C-D angle (South-West), strictly preserving locked corners (`lockAngleAC`,
     * `lockAngleAB`, `lockAngleBE`) and locked walls (`lockWallA`, `lockWallB`, `lockWallC`).
     */
    fun withAngleCD(targetCDDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockAngleCD) return this
        val prevDoorWallLen = doorWallLengthMm
        val clamped = targetCDDeg.coerceIn(65f, 115f)
        val delta = clamped - angleCDDeg

        return when {
            // If Corner B-E is unlocked and at least one side wall (B or C) can adapt
            !lockAngleBE && (!lockWallB || !lockWallC) -> {
                solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen) ?: this
            }
            // If Corner B-E is locked (or both side walls are locked) and Angle A-C is unlocked -> A-C absorbs -delta
            !lockAngleAC -> {
                val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                copy(angleACDeg = newAC).solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen)
                    ?: copy(angleACDeg = newAC).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
            }
            // If Corner B-E and A-C are locked, but Angle A-B is unlocked -> A-B absorbs -delta
            !lockAngleAB -> {
                val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                copy(angleABDeg = newAB).solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen)
                    ?: copy(angleABDeg = newAB).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
            }
            else -> {
                // All other 3 corners or required walls are locked -> cannot change 1 angle alone
                this
            }
        }
    }

    /**
     * Updates Corner B-E angle (South-East), strictly preserving locked corners (`lockAngleAC`,
     * `lockAngleAB`, `lockAngleCD`) and locked walls (`lockWallA`, `lockWallB`, `lockWallC`).
     */
    fun withAngleBE(targetBEDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockAngleBE) return this
        val prevDoorWallLen = doorWallLengthMm
        val clamped = targetBEDeg.coerceIn(65f, 115f)
        val oldCD = angleCDDeg
        val delta = clamped - angleBEDeg

        return when {
            // If Corner C-D is unlocked and at least one side wall (C or B) can adapt
            !lockAngleCD && (!lockWallC || !lockWallB) -> {
                if (!lockWallC) {
                    solveCForTargetBE(clamped, keepDoorCentered, prevDoorWallLen)
                } else {
                    val targetCD = 360f - angleACDeg - angleABDeg - clamped
                    solveBForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
                }
            }
            // If Corner C-D is locked (or both side walls are locked) and Angle A-B is unlocked -> A-B absorbs -delta
            !lockAngleAB -> {
                val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                copy(angleABDeg = newAB).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                    ?: copy(angleABDeg = newAB).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
            }
            // If Corner C-D and A-B are locked, but Angle A-C is unlocked -> A-C absorbs -delta
            !lockAngleAC -> {
                val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                copy(angleACDeg = newAC).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                    ?: copy(angleACDeg = newAC).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
            }
            else -> {
                // All other 3 corners or required walls are locked -> cannot change 1 angle alone
                this
            }
        }
    }

    /**
     * Updates segment D length (UI Tramo E):
     * - If `lockSegmentE` (UI Tramo D) is locked and `!lockWallSouth`: keeps `segmentEMm` fixed and resizes the total wall!
     * - Otherwise: adapts `segmentEMm` within the current wall length so door width is immutable.
     */
    fun withSegmentD(newDMm: Float): RoomSpec {
        if (lockSegmentD) return this
        if (lockSegmentE && !lockWallSouth) {
            val clampedD = newDMm.coerceAtLeast(0f)
            val fixedE = segmentEMm
            val targetTotal = (clampedD + door.widthMm + fixedE).coerceAtLeast(0f)
            val resized = solvePolygonForTargetSouth(targetTotal, keepDoorCentered = false, previousWallLengthMm = doorWallLengthMm)
                ?: return this
            val maxOff = resized.maxDoorOffset()
            return resized.copy(door = resized.door.copy(offsetMm = (resized.doorWallLengthMm - resized.door.widthMm - fixedE).coerceIn(0f, maxOff)))
        }
        if (lockSegmentE && lockWallSouth) return this
        val clampedD = newDMm.coerceIn(0f, maxDoorOffset())
        return copy(door = door.copy(offsetMm = clampedD))
    }

    /**
     * Updates segment E length (UI Tramo D):
     * - If `lockSegmentD` (UI Tramo E) is locked and `!lockWallSouth`: keeps `segmentDMm` fixed and resizes the total wall!
     * - Otherwise: adapts `segmentDMm` within the current wall length so door width is immutable.
     */
    fun withSegmentE(newEMm: Float): RoomSpec {
        if (lockSegmentE) return this
        if (lockSegmentD && !lockWallSouth) {
            val clampedE = newEMm.coerceAtLeast(0f)
            val fixedD = segmentDMm
            val targetTotal = (fixedD + door.widthMm + clampedE).coerceAtLeast(0f)
            val resized = solvePolygonForTargetSouth(targetTotal, keepDoorCentered = false, previousWallLengthMm = doorWallLengthMm)
                ?: return this
            val maxOff = resized.maxDoorOffset()
            return resized.copy(door = resized.door.copy(offsetMm = fixedD.coerceIn(0f, maxOff)))
        }
        if (lockSegmentD && lockWallSouth) return this
        val newD = (doorWallLengthMm - door.widthMm - newEMm).coerceIn(0f, maxDoorOffset())
        return copy(door = door.copy(offsetMm = newD))
    }

    /** Updates door width while keeping it valid on the wall (or resizing the wall if both segments D & E are locked). */
    fun withDoorWidth(newWidthMm: Float, keepCentered: Boolean = false): RoomSpec {
        if (door.lockWidth) return this
        if (lockSegmentD && lockSegmentE && !lockWallSouth) {
            val clampedW = newWidthMm.coerceAtLeast(0f)
            val fixedD = segmentDMm
            val fixedE = segmentEMm
            val targetTotal = (fixedD + clampedW + fixedE).coerceAtLeast(0f)
            val resized = solvePolygonForTargetSouth(targetTotal, keepDoorCentered = false, previousWallLengthMm = doorWallLengthMm)
                ?: return this
            val maxOff = (resized.doorWallLengthMm - clampedW).coerceAtLeast(0f)
            return resized.copy(door = resized.door.copy(widthMm = clampedW, offsetMm = fixedD.coerceIn(0f, maxOff)))
        }
        val maxW = doorWallLengthMm.coerceAtLeast(0f)
        val clampedW = newWidthMm.coerceIn(0f, maxW)
        val maxOffset = (doorWallLengthMm - clampedW).coerceAtLeast(0f)
        val newOffset = when {
            lockSegmentD && !lockSegmentE -> door.offsetMm.coerceIn(0f, maxOffset)
            lockSegmentE && !lockSegmentD -> (doorWallLengthMm - clampedW - segmentEMm).coerceIn(0f, maxOffset)
            keepCentered -> maxOffset / 2f
            else -> door.offsetMm.coerceIn(0f, maxOffset)
        }
        return copy(door = door.copy(widthMm = clampedW, offsetMm = newOffset))
    }

    /** Length in mm of the specified [wall]. */
    fun wallLengthMm(wall: WallIndex): Float = when (wall) {
        WallIndex.NORTH -> wallAMm
        WallIndex.EAST -> wallBMm
        WallIndex.SOUTH -> wallSouthMm
        WallIndex.WEST -> wallCMm
    }

    /**
     * Maximum safe depth (in mm) for a closet container placed against [wall] so it never
     * overflows or exceeds the adjacent walls.
     */
    fun maxContainerDepthForWall(wall: WallIndex): Float = when (wall) {
        WallIndex.NORTH, WallIndex.SOUTH -> minOf(wallCMm, wallBMm).coerceAtLeast(100f)
        WallIndex.WEST, WallIndex.EAST -> minOf(wallAMm, wallSouthMm).coerceAtLeast(100f)
    }

    /**
     * Clamps [container] so its width, height, depth, and offset never overflow its host wall,
     * the ceiling (`clearHeightMm`), or the adjacent walls (`maxContainerDepthForWall`).
     */
    fun clampContainer(container: ClosetContainerSpec): ClosetContainerSpec {
        val maxW = wallLengthMm(container.wall).coerceAtLeast(0f)
        val maxH = clearHeightMm.coerceAtLeast(0f)
        val maxD = maxContainerDepthForWall(container.wall).coerceAtLeast(0f)

        val clampedW = container.widthMm.coerceIn(0f, maxW)
        val clampedH = container.heightMm.coerceIn(0f, maxH)
        val clampedD = container.depthMm.coerceIn(0f, maxD)
        val maxOff = (maxW - clampedW).coerceAtLeast(0f)
        val clampedOff = container.offsetMm.coerceIn(0f, maxOff)

        return container.copy(
            widthMm = clampedW,
            heightMm = clampedH,
            depthMm = clampedD,
            offsetMm = clampedOff,
        )
    }

    /**
     * Creates and adds a new [ClosetContainerSpec] with predefined dimensions (`1800 x 2200 x 600 mm`)
     * centered on [wall] (or placed on the largest available segment if [wall] has the door),
     * automatically clamped so it never overflows the wall or adjacent walls.
     */
    fun withAddedContainer(
        wall: WallIndex,
        id: String = "container_${containers.size + 1}_${wall.name.lowercase()}",
        defaultWidthMm: Float = 1800f,
        defaultHeightMm: Float = 2200f,
        defaultDepthMm: Float = 600f,
    ): RoomSpec {
        val wallLen = wallLengthMm(wall).coerceAtLeast(0f)
        val maxH = clearHeightMm.coerceAtLeast(0f)
        val maxD = maxContainerDepthForWall(wall).coerceAtLeast(0f)

        val w = defaultWidthMm.coerceAtMost(wallLen)
        val h = defaultHeightMm.coerceAtMost(maxH)
        val d = defaultDepthMm.coerceAtMost(maxD)
        val centeredOffset = ((wallLen - w) * 0.5f).coerceAtLeast(0f)

        val raw = ClosetContainerSpec(
            id = id,
            wall = wall,
            offsetMm = centeredOffset,
            widthMm = w,
            heightMm = h,
            depthMm = d,
        )
        val clamped = clampContainer(raw)
        return copy(containers = containers + clamped)
    }

    /** Updates an existing container by [id], clamping its bounds to the room. */
    fun withUpdatedContainer(id: String, transform: (ClosetContainerSpec) -> ClosetContainerSpec): RoomSpec {
        return copy(
            containers = containers.map { item ->
                if (item.id == id) clampContainer(transform(item)) else clampContainer(item)
            },
        )
    }

    /** Duplicates an existing container by [id], shifting it slightly along the wall if space allows. */
    fun withDuplicatedContainer(
        sourceId: String,
        newId: String = "container_${System.currentTimeMillis()}_${containers.size + 1}",
    ): Pair<RoomSpec, String?> {
        val source = containers.firstOrNull { it.id == sourceId } ?: return this to null
        val wallLen = wallLengthMm(source.wall).coerceAtLeast(0f)
        val maxOff = (wallLen - source.widthMm).coerceAtLeast(0f)
        val shiftedOffset = if (source.offsetMm + source.widthMm + 40f <= wallLen) {
            source.offsetMm + source.widthMm + 20f
        } else {
            (source.offsetMm + 120f).coerceIn(0f, maxOff)
        }
        val copyItem = clampContainer(
            source.copy(
                id = newId,
                offsetMm = shiftedOffset,
            ),
        )
        return copy(containers = containers + copyItem) to copyItem.id
    }

    /** Removes a container by [id]. */
    fun withRemovedContainer(id: String): RoomSpec {
        return copy(containers = containers.filterNot { it.id == id })
    }
}
