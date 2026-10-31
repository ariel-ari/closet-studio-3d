package com.closetstudio.core.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
        require(widthMm > 0f) { "door width must be positive" }
        require(heightMm > 0f) { "door height must be positive" }
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
    val lockAngleAC: Boolean = true,
    val lockAngleAB: Boolean = true,
    val lockAngleBE: Boolean = false,
    val lockAngleCD: Boolean = false,
    val door: DoorSpec = DoorSpec(),
) {
    init {
        require(clearWidthMm >= 500f) { "Room width must be at least 500 mm, got $clearWidthMm" }
        require(clearHeightMm >= 1000f) { "Room height must be at least 1000 mm, got $clearHeightMm" }
        require(clearDepthMm >= 300f) { "Room depth must be at least 300 mm, got $clearDepthMm" }
        require(wallBMm >= 300f) { "Wall B must be at least 300 mm, got $wallBMm" }
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
            return sqrt(dx * dx + dz * dz).coerceAtLeast(500f)
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

    /** Centers the door between segment D and E. */
    fun withCenteredDoor(): RoomSpec {
        val centered = ((doorWallLengthMm - door.widthMm) / 2f).coerceAtLeast(0f)
        return copy(door = door.copy(offsetMm = centered))
    }

    /**
     * Adapts the door opening and segments D/E when the room polygon changes:
     * - If `door.lockWidth == true` (default): `door.widthMm` stays strictly invariant (`🔒`)
     *   and does NOT grow or shrink with the walls; only segments D and E absorb the wall length change.
     * - If `door.lockWidth == false` (`🔓`): `door.widthMm` scales proportionally with the door wall length.
     */
    private fun applyDoorAdaptation(
        keepCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec {
        val currentWallLen = doorWallLengthMm
        val updatedWidth = if (door.lockWidth || previousWallLengthMm <= 1f) {
            door.widthMm
        } else {
            val ratio = currentWallLen / previousWallLengthMm
            ((door.widthMm * ratio / 10f).let { kotlin.math.round(it) * 10f })
                .coerceIn(500f, (currentWallLen - 200f).coerceAtLeast(500f))
        }
        val maxOffset = (currentWallLen - updatedWidth).coerceAtLeast(0f)
        val updatedOffset = if (keepCentered) {
            maxOffset / 2f
        } else {
            door.offsetMm.coerceIn(0f, maxOffset)
        }
        return copy(door = door.copy(widthMm = updatedWidth, offsetMm = updatedOffset))
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

    /** Updates Wall A (North), propagating constraints if South wall or bottom corners are locked. */
    fun withWallA(newAMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallA) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedA = newAMm.coerceIn(1500f, 6000f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        var next = copy(clearWidthMm = clampedA)
        next = when {
            lockAngleCD -> next.solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
            lockAngleBE -> {
                val targetCD = 360f - next.angleACDeg - next.angleABDeg - oldBE
                next.solveWallForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
            }
            lockWallSouth && !lockWallB -> {
                val targetSouth = wallSouthMm
                val sw = next.vertexSW
                val radAB = Math.toRadians(next.angleABDeg.toDouble()).toFloat()
                val dx = clampedA - sw.x
                val dzSq = (targetSouth * targetSouth - dx * dx).coerceAtLeast(0f)
                val targetZ = sw.z + sqrt(dzSq)
                val sinAB = sin(radAB).coerceAtLeast(0.2f)
                val newB = (targetZ / sinAB).coerceIn(1500f, 6000f)
                next.copy(wallBMm = newB)
            }
            else -> next
        }
        return next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
    }

    /** Updates Wall B (East) independently, adapting unlocked walls/angles to close polygon. */
    fun withWallB(newBMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallB) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedB = newBMm.coerceIn(1500f, 6000f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val targetSouth = wallSouthMm
        var next = copy(wallBMm = clampedB)
        next = when {
            lockAngleBE && !lockWallC -> next.solveCForTargetBE(oldBE, keepDoorCentered, prevDoorWallLen)
            lockAngleCD && !lockWallC -> {
                val targetBE = 360f - next.angleACDeg - next.angleABDeg - oldCD
                next.solveCForTargetBE(targetBE, keepDoorCentered, prevDoorWallLen)
            }
            lockWallSouth && !lockWallA -> {
                val sw = next.vertexSW
                val radAB = Math.toRadians(next.angleABDeg.toDouble()).toFloat()
                val z2 = clampedB * sin(radAB)
                val dz = z2 - sw.z
                val dx = sqrt((targetSouth * targetSouth - dz * dz).coerceAtLeast(250_000f))
                val newA = (sw.x + dx + clampedB * cos(radAB)).coerceIn(1500f, 6000f)
                next.copy(clearWidthMm = newA)
            }
            lockWallSouth && lockWallA && !lockWallC -> {
                val se = next.vertexSE
                val radAC = Math.toRadians(next.angleACDeg.toDouble()).toFloat()
                val sinAC = sin(radAC).coerceAtLeast(0.2f)
                val dx = se.x - clearDepthMm * cos(radAC)
                val dz = sqrt((targetSouth * targetSouth - dx * dx).coerceAtLeast(0f))
                val newC = ((se.z - dz) / sinAC).coerceIn(1500f, 6000f)
                next.copy(clearDepthMm = newC)
            }
            else -> next
        }
        return next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
    }

    /** Updates Wall C (West) independently, adapting unlocked walls/angles to close polygon. */
    fun withWallC(newCMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallC) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedC = newCMm.coerceIn(1500f, 6000f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val targetSouth = wallSouthMm
        var next = copy(clearDepthMm = clampedC)
        next = when {
            lockAngleCD && !lockWallB -> next.solveBForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
            lockAngleBE && !lockWallB -> {
                val targetCD = 360f - next.angleACDeg - next.angleABDeg - oldBE
                next.solveBForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
            }
            lockWallSouth && !lockWallA -> {
                val sw = next.vertexSW
                val se = next.vertexSE
                val radAB = Math.toRadians(next.angleABDeg.toDouble()).toFloat()
                val dz = se.z - sw.z
                val dx = sqrt((targetSouth * targetSouth - dz * dz).coerceAtLeast(250_000f))
                val newA = (sw.x + dx + wallBMm * cos(radAB)).coerceIn(1500f, 6000f)
                next.copy(clearWidthMm = newA)
            }
            lockWallSouth && lockWallA && !lockWallB -> {
                val sw = next.vertexSW
                val se = next.vertexSE
                val radAB = Math.toRadians(next.angleABDeg.toDouble()).toFloat()
                val sinAB = sin(radAB).coerceAtLeast(0.2f)
                val dx = se.x - sw.x
                val dz = sqrt((targetSouth * targetSouth - dx * dx).coerceAtLeast(0f))
                val newB = ((sw.z + dz) / sinAB).coerceIn(1500f, 6000f)
                next.copy(wallBMm = newB)
            }
            else -> next
        }
        return next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
    }

    /** Directly updates the total length of the South Wall (Door wall), adapting unlocked Wall A or Wall B. */
    fun withWallSouth(targetSouthMm: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        if (lockWallSouth) return this
        val prevDoorWallLen = doorWallLengthMm
        val clampedS = targetSouthMm.coerceIn(1500f, 6000f)
        val sw = vertexSW
        val se = vertexSE
        val radAB = Math.toRadians(angleABDeg.toDouble()).toFloat()
        val dz = se.z - sw.z
        val dx = sqrt((clampedS * clampedS - dz * dz).coerceAtLeast(250_000f))
        val newA = (sw.x + dx + wallBMm * cos(radAB)).coerceIn(1500f, 6000f)
        return copy(clearWidthMm = newA).applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
    }

    /**
     * Routes polygon angle closure to whichever side wall (`Wall B` or `Wall C`) is unlocked,
     * preserving locked wall lengths (`lockWallB` / `lockWallC`).
     */
    private fun solveWallForTargetCD(
        targetCDDeg: Float,
        keepDoorCentered: Boolean,
        previousWallLengthMm: Float = doorWallLengthMm,
    ): RoomSpec {
        return if (lockWallB && !lockWallC) {
            val targetBE = 360f - angleACDeg - angleABDeg - targetCDDeg
            solveCForTargetBE(targetBE, keepDoorCentered, previousWallLengthMm)
        } else {
            solveBForTargetCD(targetCDDeg, keepDoorCentered, previousWallLengthMm)
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
            val newB = (((sw.z - ne.z) * dSurX - (sw.x - ne.x) * dSurZ) / denom).coerceIn(500f, 9000f)
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
            val newC = (((se.z - nw.z) * dSurX - (se.x - nw.x) * dSurZ) / denom).coerceIn(500f, 9000f)
            return copy(clearDepthMm = newC).applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
        }
        return applyDoorAdaptation(keepDoorCentered, previousWallLengthMm)
    }

    /**
     * Updates Corner A-C angle (North-West), strictly preserving any locked corners (`lockAngleAB`,
     * `lockAngleCD`, `lockAngleBE`) by routing polygon closure (`Σ = 360°`) only to unlocked corners
     * and adapting the required wall lengths (`Wall B` or `Wall C`).
     */
    fun withAngleAC(newAngleDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        val prevDoorWallLen = doorWallLengthMm
        val clamped = newAngleDeg.coerceIn(65f, 115f)
        val oldCD = angleCDDeg
        val oldBE = angleBEDeg
        val delta = clamped - angleACDeg

        return when {
            // If both bottom corners are locked, unlock Angle A-B must absorb -delta so CD and BE stay fixed
            lockAngleCD && lockAngleBE -> {
                if (!lockAngleAB) {
                    val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                    copy(angleACDeg = clamped, angleABDeg = newAB).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                } else {
                    // All other 3 corners are locked -> geometrically impossible to change 1 angle in a quad
                    this
                }
            }
            // Corner C-D is locked -> preserve oldCD and let unlocked Corner B-E (or A-B) absorb the change
            lockAngleCD -> {
                copy(angleACDeg = clamped).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
            }
            // Corner B-E is locked -> preserve oldBE and let unlocked Corner C-D absorb the change
            lockAngleBE -> {
                val targetCD = 360f - clamped - angleABDeg - oldBE
                copy(angleACDeg = clamped).solveWallForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
            }
            // Both C-D and B-E are unlocked -> keep Wall C & B lengths or adapt South if locked
            else -> {
                val targetSouth = wallSouthMm
                var next = copy(angleACDeg = clamped)
                if (lockWallSouth && !lockWallA) {
                    val sw = next.vertexSW
                    val se = next.vertexSE
                    val radAB = Math.toRadians(next.angleABDeg.toDouble()).toFloat()
                    val dz = se.z - sw.z
                    val dx = sqrt((targetSouth * targetSouth - dz * dz).coerceAtLeast(250_000f))
                    val newA = (sw.x + dx + wallBMm * cos(radAB)).coerceIn(1500f, 6000f)
                    next = next.copy(clearWidthMm = newA)
                }
                next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
            }
        }
    }

    /**
     * Updates Corner A-B angle (North-East), strictly preserving any locked corners (`lockAngleAC`,
     * `lockAngleCD`, `lockAngleBE`) by routing polygon closure (`Σ = 360°`) only to unlocked corners
     * and adapting the required wall lengths (`Wall B` or `Wall C`).
     */
    fun withAngleAB(newAngleDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
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
                    copy(angleABDeg = clamped, angleACDeg = newAC).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
                } else {
                    this
                }
            }
            // Corner B-E is locked -> preserve oldBE and let unlocked Corner C-D absorb the change
            lockAngleBE -> {
                val targetCD = 360f - angleACDeg - clamped - oldBE
                copy(angleABDeg = clamped).solveWallForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
            }
            // Corner C-D is locked -> preserve oldCD and let unlocked Corner B-E absorb the change
            lockAngleCD -> {
                copy(angleABDeg = clamped).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
            }
            // Both C-D and B-E are unlocked
            else -> {
                val targetSouth = wallSouthMm
                var next = copy(angleABDeg = clamped)
                if (lockWallSouth && !lockWallA) {
                    val sw = next.vertexSW
                    val se = next.vertexSE
                    val radAB = Math.toRadians(clamped.toDouble()).toFloat()
                    val dz = se.z - sw.z
                    val dx = sqrt((targetSouth * targetSouth - dz * dz).coerceAtLeast(250_000f))
                    val newA = (sw.x + dx + wallBMm * cos(radAB)).coerceIn(1500f, 6000f)
                    next = next.copy(clearWidthMm = newA)
                }
                next.applyDoorAdaptation(keepDoorCentered, prevDoorWallLen)
            }
        }
    }

    /**
     * Updates Corner C-D angle (South-West), strictly preserving locked corners (`lockAngleAC`,
     * `lockAngleAB`, `lockAngleBE`).
     */
    fun withAngleCD(targetCDDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        val prevDoorWallLen = doorWallLengthMm
        val clamped = targetCDDeg.coerceIn(65f, 115f)
        val delta = clamped - angleCDDeg

        return when {
            // If Corner B-E is unlocked (and unless Wall B is locked while Angle A-C is unlocked),
            // keep both top angles AC and AB intact and adapt Wall B (or Wall C) so B-E absorbs -delta
            !lockAngleBE && (!lockWallB || !lockWallC || lockAngleAC) -> {
                solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen)
            }
            // If Corner B-E is locked (or Wall B is locked) and Angle A-C is unlocked -> A-C absorbs -delta, preserving B-E!
            !lockAngleAC -> {
                val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                copy(angleACDeg = newAC).solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen)
            }
            // If Corner B-E and A-C are locked, but Angle A-B is unlocked -> A-B absorbs -delta, preserving B-E and A-C!
            !lockAngleAB -> {
                val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                copy(angleABDeg = newAB).solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen)
            }
            // Fallback if B-E is unlocked
            !lockAngleBE -> {
                solveWallForTargetCD(clamped, keepDoorCentered, prevDoorWallLen)
            }
            else -> {
                // All other 3 corners (AC, AB, BE) are locked -> cannot change 1 angle alone
                this
            }
        }
    }

    /**
     * Updates Corner B-E angle (South-East), strictly preserving locked corners (`lockAngleAC`,
     * `lockAngleAB`, `lockAngleCD`).
     */
    fun withAngleBE(targetBEDeg: Float, keepDoorCentered: Boolean = isDoorCentered): RoomSpec {
        val prevDoorWallLen = doorWallLengthMm
        val clamped = targetBEDeg.coerceIn(65f, 115f)
        val oldCD = angleCDDeg
        val delta = clamped - angleBEDeg

        return when {
            // If Corner C-D is unlocked (and unless Wall C is locked while Angle A-B is unlocked),
            // keep both top angles AC and AB intact and adapt Wall C (or Wall B) so C-D absorbs -delta
            !lockAngleCD && (!lockWallC || !lockWallB || lockAngleAB) -> {
                if (!lockWallC || lockWallB) {
                    solveCForTargetBE(clamped, keepDoorCentered, prevDoorWallLen)
                } else {
                    val targetCD = 360f - angleACDeg - angleABDeg - clamped
                    solveBForTargetCD(targetCD, keepDoorCentered, prevDoorWallLen)
                }
            }
            // If Corner C-D is locked (or Wall C is locked) and Angle A-B is unlocked -> A-B absorbs -delta, preserving C-D!
            !lockAngleAB -> {
                val newAB = (angleABDeg - delta).coerceIn(65f, 115f)
                copy(angleABDeg = newAB).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
            }
            // If Corner C-D and A-B are locked, but Angle A-C is unlocked -> A-C absorbs -delta, preserving C-D and A-B!
            !lockAngleAC -> {
                val newAC = (angleACDeg - delta).coerceIn(65f, 115f)
                copy(angleACDeg = newAC).solveWallForTargetCD(oldCD, keepDoorCentered, prevDoorWallLen)
            }
            // Fallback if C-D is unlocked
            !lockAngleCD -> {
                solveCForTargetBE(clamped, keepDoorCentered, prevDoorWallLen)
            }
            else -> {
                // All other 3 corners (AC, AB, CD) are locked -> cannot change 1 angle alone
                this
            }
        }
    }

    /** Updates segment D length, adapting segment E so door width is immutable. */
    fun withSegmentD(newDMm: Float): RoomSpec {
        val clampedD = newDMm.coerceIn(0f, maxDoorOffset())
        return copy(door = door.copy(offsetMm = clampedD))
    }

    /** Updates segment E length, adapting segment D so door width is immutable. */
    fun withSegmentE(newEMm: Float): RoomSpec {
        val newD = (doorWallLengthMm - door.widthMm - newEMm).coerceIn(0f, maxDoorOffset())
        return copy(door = door.copy(offsetMm = newD))
    }

    /** Updates door width while keeping it valid on the wall. */
    fun withDoorWidth(newWidthMm: Float, keepCentered: Boolean = false): RoomSpec {
        val maxW = doorWallLengthMm.coerceAtLeast(100f)
        val clampedW = newWidthMm.coerceIn(500f, maxW)
        val maxOffset = (doorWallLengthMm - clampedW).coerceAtLeast(0f)
        val newOffset = if (keepCentered) {
            maxOffset / 2f
        } else {
            door.offsetMm.coerceIn(0f, maxOffset)
        }
        return copy(door = door.copy(widthMm = clampedW, offsetMm = newOffset))
    }
}
