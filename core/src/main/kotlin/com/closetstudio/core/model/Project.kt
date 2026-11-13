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
data class Vec3Mm(
    val x: Float,
    val y: Float,
    val z: Float,
)

@Serializable
enum class RoomCorner(
    val incomingWall: WallIndex,
    val outgoingWall: WallIndex,
    val codeLabel: String,
) {
    NW(WallIndex.WEST, WallIndex.NORTH, "A-B"),
    NE(WallIndex.NORTH, WallIndex.EAST, "B-C"),
    SE(WallIndex.EAST, WallIndex.SOUTH, "C-D"),
    SW(WallIndex.SOUTH, WallIndex.WEST, "E-A"),
}

data class CornerIntersectionCandidate(
    val corner: RoomCorner,
    val incomingContainer: ClosetContainerSpec,
    val outgoingContainer: ClosetContainerSpec,
    val isFused: Boolean,
)

@Serializable
data class ContainerBay(
    val index: Int,
    val startXMm: Float,
    val endXMm: Float,
) {
    val widthMm: Float get() = (endXMm - startXMm).coerceAtLeast(0f)
}

@Serializable
data class BayCompartment(
    val index: Int = 0,
    val bottomYMm: Float,
    val topYMm: Float,
) {
    val heightMm: Float get() = (topYMm - bottomYMm).coerceAtLeast(0f)
}

@Serializable
data class VerticalDividerSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val offsetXMm: Float = 0f,
    val thicknessMm: Float = 18f,
    val startYMm: Float = 0f,
    val endYMm: Float? = null,
    val depthMm: Float? = null,
) {
    init {
        require(offsetXMm >= 0f) { "divider offset must be non-negative" }
        require(thicknessMm > 0f) { "divider thickness must be positive" }
        require(startYMm >= 0f) { "divider startY must be non-negative" }
        if (endYMm != null) {
            require(endYMm > startYMm) { "divider endY must be greater than startY" }
        }
        if (depthMm != null) {
            require(depthMm > 0f) { "divider depth must be positive" }
        }
    }

    fun effectiveStartY(): Float = startYMm.coerceAtLeast(0f)
    fun effectiveEndY(interiorHeightMm: Float): Float =
        (endYMm ?: interiorHeightMm).coerceIn(effectiveStartY() + 10f, interiorHeightMm)
    fun effectiveHeight(interiorHeightMm: Float): Float =
        (effectiveEndY(interiorHeightMm) - effectiveStartY()).coerceAtLeast(10f)
    fun effectiveDepth(interiorDepthMm: Float): Float =
        (depthMm ?: interiorDepthMm).coerceAtLeast(50f)
}

@Serializable
data class HorizontalShelfSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bayIndex: Int = 0,
    val elevationYMm: Float = 600f,
    val thicknessMm: Float = 18f,
    val depthMm: Float? = null,
) {
    init {
        require(bayIndex >= 0) { "shelf bayIndex must be non-negative" }
        require(elevationYMm >= 0f) { "shelf elevation must be non-negative" }
        require(thicknessMm > 0f) { "shelf thickness must be positive" }
        if (depthMm != null) {
            require(depthMm > 0f) { "shelf depth must be positive" }
        }
    }

    fun effectiveDepth(interiorDepthMm: Float): Float = (depthMm ?: interiorDepthMm).coerceAtLeast(50f)
}

@Serializable
enum class DrawerFrontType {
    HANDLE,
    NOTCH,
}

@Serializable
data class DrawerSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bayIndex: Int = 0,
    val elevationYMm: Float = 0f,
    val heightMm: Float = 200f,
    val depthMm: Float? = null,
    val frontType: DrawerFrontType = DrawerFrontType.HANDLE,
    val hasHandle: Boolean = (frontType == DrawerFrontType.HANDLE),
) {
    init {
        require(bayIndex >= 0) { "drawer bayIndex must be non-negative" }
        require(elevationYMm >= 0f) { "drawer elevation must be non-negative" }
        require(heightMm > 0f) { "drawer height must be positive" }
        if (depthMm != null) {
            require(depthMm > 0f) { "drawer depth must be positive" }
        }
    }

    fun effectiveDepth(interiorDepthMm: Float): Float = (depthMm ?: interiorDepthMm).coerceAtLeast(100f)
}

@Serializable
data class ClothesRailSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bayIndex: Int = 0,
    val elevationYMm: Float = 0f,
    val depthOffsetMm: Float? = null,
    val diameterMm: Float = 25f,
) {
    init {
        require(bayIndex >= 0) { "rail bayIndex must be non-negative" }
        require(elevationYMm >= 0f) { "rail elevation must be non-negative" }
        require(diameterMm > 0f) { "rail diameter must be positive" }
        if (depthOffsetMm != null) {
            require(depthOffsetMm > 0f) { "rail depthOffset must be positive" }
        }
    }

    fun effectiveDepth(interiorDepthMm: Float): Float =
        (depthOffsetMm ?: (interiorDepthMm * 0.5f)).coerceIn(50f, (interiorDepthMm - 50f).coerceAtLeast(50f))
}

@Serializable
data class VerticalPoleSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bayIndex: Int = 0,
    val offsetXMm: Float? = null,
    val depthOffsetMm: Float? = null,
    val diameterMm: Float = 25f,
    val bottomElevationYMm: Float? = null,
    val topElevationYMm: Float? = null,
) {
    init {
        require(bayIndex >= 0) { "pole bayIndex must be non-negative" }
        require(diameterMm > 0f) { "pole diameter must be positive" }
        if (offsetXMm != null) {
            require(offsetXMm >= 0f) { "pole offsetXMm must be non-negative" }
        }
        if (depthOffsetMm != null) {
            require(depthOffsetMm > 0f) { "pole depthOffset must be positive" }
        }
    }

    fun effectiveX(bayStartXMm: Float, bayEndXMm: Float): Float {
        val radius = diameterMm * 0.5f
        val minX = bayStartXMm + radius
        val maxX = (bayEndXMm - radius).coerceAtLeast(minX)
        return (offsetXMm ?: ((bayStartXMm + bayEndXMm) * 0.5f)).coerceIn(minX, maxX)
    }

    fun effectiveX(container: ClosetContainerSpec): Float {
        val bays = container.getBays()
        val bay = bays.getOrElse(bayIndex) { bays.last() }
        return effectiveX(bay.startXMm, bay.endXMm)
    }

    fun effectiveDepth(interiorDepthMm: Float): Float =
        (depthOffsetMm ?: (interiorDepthMm * 0.5f)).coerceIn(50f, (interiorDepthMm - 50f).coerceAtLeast(50f))

    fun effectiveYRange(compY0: Float, compY1: Float): Pair<Float, Float> {
        val y0 = bottomElevationYMm ?: compY0
        val y1 = (topElevationYMm ?: compY1).coerceAtLeast(y0 + 50f)
        return y0 to y1
    }

    fun effectiveYRange(container: ClosetContainerSpec): Pair<Float, Float> {
        val y0 = bottomElevationYMm ?: 0f
        val y1 = (topElevationYMm ?: container.interiorHeightMm).coerceAtLeast(y0 + 50f)
        return y0 to y1
    }
}

@Serializable
data class ShoeRackSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bayIndex: Int = 0,
    val elevationYMm: Float = 300f,
    val thicknessMm: Float = 18f,
    val depthMm: Float? = null,
    val inclinationAngleDeg: Float = 18f,
    val lipHeightMm: Float = 40f,
    val lipThicknessMm: Float = 18f,
) {
    init {
        require(bayIndex >= 0) { "shoe rack bayIndex must be non-negative" }
        require(elevationYMm >= 0f) { "shoe rack elevation must be non-negative" }
        require(thicknessMm > 0f) { "shoe rack thickness must be positive" }
        require(inclinationAngleDeg in 0f..45f) { "shoe rack angle must be between 0 and 45 degrees" }
        require(lipHeightMm >= 0f) { "shoe rack lip height must be non-negative" }
        require(lipThicknessMm > 0f) { "shoe rack lip thickness must be positive" }
        if (depthMm != null) {
            require(depthMm > 0f) { "shoe rack depth must be positive" }
        }
    }

    fun effectiveDepth(interiorDepthMm: Float): Float =
        (depthMm ?: interiorDepthMm).coerceIn(150f, interiorDepthMm.coerceAtLeast(150f))

    fun deltaElevationY(interiorDepthMm: Float): Float {
        val rad = Math.toRadians(inclinationAngleDeg.toDouble()).toFloat()
        return effectiveDepth(interiorDepthMm) * kotlin.math.tan(rad)
    }

    fun totalVerticalSpanMm(interiorDepthMm: Float): Float {
        val deltaY = deltaElevationY(interiorDepthMm)
        val rad = Math.toRadians(inclinationAngleDeg.toDouble()).toFloat()
        val vertThick = thicknessMm / kotlin.math.cos(rad).coerceAtLeast(0.1f)
        return maxOf(deltaY + vertThick, vertThick + lipHeightMm)
    }

    fun maxElevation(interiorHeightMm: Float, interiorDepthMm: Float): Float =
        (interiorHeightMm - totalVerticalSpanMm(interiorDepthMm)).coerceAtLeast(0f)
}

@Serializable
data class CabinetDoorsSpec(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bayIndex: Int = 0,
    val doorCount: Int = 2,
    val elevationYMm: Float = 0f,
    val heightMm: Float = 1200f,
    val thicknessMm: Float = 18f,
    val doorGapMm: Float = 2f,
    val hasHandles: Boolean = true,
    val handleLengthMm: Float = 160f,
) {
    init {
        require(bayIndex >= 0) { "doors bayIndex must be non-negative" }
        require(doorCount in 1..2) { "doorCount must be 1 or 2" }
        require(elevationYMm >= 0f) { "doors elevation must be non-negative" }
        require(heightMm > 0f) { "doors height must be positive" }
        require(thicknessMm > 0f) { "doors thickness must be positive" }
        require(doorGapMm >= 0f) { "doorGapMm must be non-negative" }
        require(handleLengthMm > 0f) { "handleLengthMm must be positive" }
    }
}

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
    val fuseStartCorner: Boolean = false,
    val fuseEndCorner: Boolean = false,
    val dividers: List<VerticalDividerSpec> = emptyList(),
    val shelves: List<HorizontalShelfSpec> = emptyList(),
    val drawers: List<DrawerSpec> = emptyList(),
    val clothesRails: List<ClothesRailSpec> = emptyList(),
    val verticalPoles: List<VerticalPoleSpec> = emptyList(),
    val shoeRacks: List<ShoeRackSpec> = emptyList(),
    val cabinetDoors: List<CabinetDoorsSpec> = emptyList(),
    val materialTextureId: String? = null,
    val isGrainHorizontal: Boolean = false,
) {
    init {
        require(widthMm >= 0f) { "container width must be non-negative" }
        require(heightMm >= 0f) { "container height must be non-negative" }
        require(depthMm >= 0f) { "container depth must be non-negative" }
    }

    val effectivePanelThicknessMm: Float
        get() = panelThicknessMm.coerceIn(12f, 50f)

    val effectivePlinthHeightMm: Float
        get() = plinthHeightMm.coerceAtLeast(0f)

    val interiorWidthMm: Float
        get() = (widthMm - effectivePanelThicknessMm * 2f).coerceAtLeast(0f)

    val interiorHeightMm: Float
        get() = (heightMm - effectivePlinthHeightMm - effectivePanelThicknessMm * 2f).coerceAtLeast(0f)

    val interiorDepthMm: Float
        get() = (depthMm - backThicknessMm).coerceAtLeast(0f)

    /**
     * Computes the vertical bays (cajas / columnas) partitioned by the sorted vertical dividers.
     * With N dividers there are N + 1 bays.
     */
    fun getBays(): List<ContainerBay> {
        val innerW = interiorWidthMm
        if (innerW <= 10f) return listOf(ContainerBay(0, 0f, 0f))
        val sortedDividers = dividers.sortedBy { it.offsetXMm }
        if (sortedDividers.isEmpty()) {
            return listOf(ContainerBay(0, 0f, innerW))
        }

        val bays = mutableListOf<ContainerBay>()
        var currentX = 0f
        var bayIdx = 0

        for (div in sortedDividers) {
            val clampedDivOffset = div.offsetXMm.coerceIn(currentX, (innerW - div.thicknessMm).coerceAtLeast(currentX))
            bays.add(ContainerBay(index = bayIdx++, startXMm = currentX, endXMm = clampedDivOffset))
            currentX = (clampedDivOffset + div.thicknessMm).coerceAtMost(innerW)
        }
        bays.add(ContainerBay(index = bayIdx, startXMm = currentX, endXMm = innerW))
        return bays
    }

    fun findBayAtX(xMm: Float): ContainerBay {
        val bays = getBays()
        return bays.firstOrNull { xMm >= it.startXMm && xMm <= it.endXMm } ?: bays.lastOrNull() ?: ContainerBay(0, 0f, interiorWidthMm)
    }

    fun findVerticalCompartmentRange(bayIndex: Int, targetYMm: Float): Pair<Float, Float> {
        val innerH = interiorHeightMm
        val bayShelves = shelves.filter { it.bayIndex == bayIndex }.sortedBy { it.elevationYMm }
        if (bayShelves.isEmpty()) {
            return 0f to innerH
        }

        var prevY = 0f
        for (shelf in bayShelves) {
            val shelfBottom = shelf.elevationYMm
            val shelfTop = shelfBottom + shelf.thicknessMm
            if (targetYMm < shelfBottom) {
                return prevY to shelfBottom
            }
            if (targetYMm in shelfBottom..shelfTop) {
                val mid = (shelfBottom + shelfTop) * 0.5f
                return if (targetYMm <= mid) {
                    prevY to shelfBottom
                } else {
                    val nextShelfBottom = bayShelves.firstOrNull { it.elevationYMm > shelfTop }?.elevationYMm ?: innerH
                    shelfTop to nextShelfBottom
                }
            }
            prevY = shelfTop
        }
        return prevY to innerH
    }

    /**
     * Returns the vertical compartments (usable vertical sections) within [bayIndex].
     * Takes into account:
     * - Bottom drawers (elevationYMm <= 50mm): doors sit on top of consecutive bottom drawers.
     * - Dividing shelves and shoe racks: each separates the bay into upper and lower compartments.
     * - Internal elevated drawers: act as solid dividers.
     */
    fun getBayCompartments(bayIndex: Int): List<BayCompartment> {
        val innerH = interiorHeightMm
        val bayDrawers = drawers.filter { it.bayIndex == bayIndex }.sortedBy { it.elevationYMm }
        val bayShelves = shelves.filter { it.bayIndex == bayIndex }.sortedBy { it.elevationYMm }
        val bayShoeRacks = shoeRacks.filter { it.bayIndex == bayIndex }.sortedBy { it.elevationYMm }

        var baseFloorY = 0f
        for (d in bayDrawers) {
            if (d.elevationYMm <= baseFloorY + 30f) {
                baseFloorY = kotlin.math.max(baseFloorY, d.elevationYMm + d.heightMm)
            }
        }

        val dividingShelves = bayShelves.filter { it.elevationYMm >= (baseFloorY - 5f) && (it.elevationYMm + it.thicknessMm) <= innerH }
        val dividingShoeRacks = bayShoeRacks.filter { it.elevationYMm >= (baseFloorY - 5f) && (it.elevationYMm + it.thicknessMm) <= innerH }
        val elevatedDrawers = bayDrawers.filter { it.elevationYMm > (baseFloorY + 10f) && (it.elevationYMm + it.heightMm) <= innerH }

        if (dividingShelves.isEmpty() && dividingShoeRacks.isEmpty() && elevatedDrawers.isEmpty()) {
            return listOf(BayCompartment(0, baseFloorY, innerH))
        }

        data class DividerBlock(val bottomY: Float, val topY: Float)
        val blocks = (dividingShelves.map { DividerBlock(it.elevationYMm, it.elevationYMm + it.thicknessMm) } +
            dividingShoeRacks.map { DividerBlock(it.elevationYMm, it.elevationYMm + it.thicknessMm) } +
            elevatedDrawers.map { DividerBlock(it.elevationYMm, it.elevationYMm + it.heightMm) })
            .sortedBy { it.bottomY }

        val comps = mutableListOf<BayCompartment>()
        var curY = baseFloorY
        var compIdx = 0

        for (block in blocks) {
            if (block.bottomY > curY + 20f) {
                comps.add(BayCompartment(compIdx++, curY, block.bottomY))
            }
            curY = kotlin.math.max(curY, block.topY)
        }
        if (curY < innerH - 20f) {
            comps.add(BayCompartment(compIdx, curY, innerH))
        }

        return if (comps.isNotEmpty()) comps else listOf(BayCompartment(0, baseFloorY, innerH))
    }

    /**
     * Determines the vertical door span (bottomY to topY) based on user interaction point [targetYMm].
     * Adapts intelligently to:
     * - Single compartment (when hovering inside a compartment)
     * - Spanning adjacent compartments (when hovering over a dividing shelf)
     * - Spanning the entire bay / all compartments (when hovering over middle of bay or bay center)
     */
    fun findDoorsPlacementSpan(bayIndex: Int, targetYMm: Float): Pair<Float, Float> {
        val innerH = interiorHeightMm
        val comps = getBayCompartments(bayIndex)
        if (comps.isEmpty()) return 0f to innerH
        if (comps.size == 1) return comps[0].bottomYMm to comps[0].topYMm

        val fullBottom = comps.first().bottomYMm
        val fullTop = comps.last().topYMm

        // 1. Check if hovering near the boundary (dividing shelf) between comp[i] and comp[i+1]
        for (i in 0 until comps.size - 1) {
            val dividerBottom = comps[i].topYMm
            val dividerTop = comps[i + 1].bottomYMm
            val dividerMid = (dividerBottom + dividerTop) * 0.5f
            if (kotlin.math.abs(targetYMm - dividerMid) <= 65f) {
                // Hovered right over the divider!
                // If there are exactly 2 compartments, spanning both means full bay!
                // If there are 3+ compartments, it spans the 2 adjacent compartments (comp[i] and comp[i+1])!
                return comps[i].bottomYMm to comps[i + 1].topYMm
            }
        }

        // 2. If 3 or more compartments and hovering near the overall center of the bay, span all
        val fullMid = (fullBottom + fullTop) * 0.5f
        if (comps.size >= 3 && kotlin.math.abs(targetYMm - fullMid) <= 45f) {
            return fullBottom to fullTop
        }

        // 3. Otherwise find which individual compartment targetYMm is located in
        for (comp in comps) {
            if (targetYMm in comp.bottomYMm..comp.topYMm) {
                return comp.bottomYMm to comp.topYMm
            }
        }

        // 4. Closest compartment fallback
        val closest = comps.minByOrNull { kotlin.math.abs(targetYMm - (it.bottomYMm + it.topYMm) * 0.5f) }
        return if (closest != null) closest.bottomYMm to closest.topYMm else fullBottom to fullTop
    }

    /**
     * Calculates the magnetically snapped elevation for a horizontal shelf:
     * 1. Priority 1 (Cabinetry alignment): Snaps to the elevation of any existing shelf
     *    in any other compartment or bay within [snapThresholdMm].
     * 2. Priority 2 (Structural divisions): Snaps to the vertical midpoint (50% interior height)
     *    or equal thirds / quarters if within [snapThresholdMm].
     * If no target is within [snapThresholdMm], returns [rawElevationYMm].
     */
    fun findSnapElevation(
        currentShelfId: String?,
        rawElevationYMm: Float,
        targetBayIndex: Int? = null,
        shelfThicknessMm: Float = effectivePanelThicknessMm,
        snapThresholdMm: Float = 28f,
    ): Float {
        if (snapThresholdMm <= 0f) return rawElevationYMm
        val innerH = interiorHeightMm

        // Priority 1: Match existing elements across the entire container
        val existingTargets = mutableListOf<Float>()
        val isRail = clothesRails.any { it.id == currentShelfId }
        val isShoeRack = shoeRacks.any { it.id == currentShelfId }
        val isDoors = currentShelfId?.startsWith("doors") == true || cabinetDoors.any { it.id == currentShelfId }

        if (isRail) {
            // Clothes rail Priority 1: Coplanar alignment with other rails across bays
            for (r in clothesRails) {
                if (r.id != currentShelfId) {
                    existingTargets.add(r.elevationYMm)
                }
            }
            // Clothes rail Priority 2: Snaps to the vertical center of the compartment in target bay
            val targetBay = targetBayIndex ?: clothesRails.firstOrNull { it.id == currentShelfId }?.bayIndex ?: 0
            val (compY0, compY1) = findVerticalCompartmentRange(targetBay, rawElevationYMm)
            existingTargets.add((compY0 + compY1) * 0.5f)
        } else if (isShoeRack) {
            // Shoe rack Priority 1: Coplanar alignment with other shoe racks across bays
            for (sr in shoeRacks) {
                if (sr.id != currentShelfId) {
                    existingTargets.add(sr.elevationYMm)
                    // Stacking shoe racks in the same bay with comfortable shoe clearance
                    if (targetBayIndex == null || sr.bayIndex == targetBayIndex) {
                        existingTargets.add(sr.elevationYMm + sr.totalVerticalSpanMm(interiorDepthMm) + 30f)
                        val under = sr.elevationYMm - 180f
                        if (under >= 0f) existingTargets.add(under)
                    }
                }
            }
            // Flush on bottom floor panel or shelves
            existingTargets.add(0f)
            for (other in shelves) {
                existingTargets.add(other.elevationYMm + other.thicknessMm)
            }
        } else if (isDoors) {
            // Cabinet doors Priority 1: Coplanar alignment with other doors across bays
            for (cd in cabinetDoors) {
                if (cd.id != currentShelfId) {
                    existingTargets.add(cd.elevationYMm)
                    if (targetBayIndex == null || cd.bayIndex == targetBayIndex) {
                        existingTargets.add(cd.elevationYMm + cd.heightMm)
                        val under = cd.elevationYMm - shelfThicknessMm
                        if (under >= 0f) existingTargets.add(under)
                    }
                }
            }
            // Flush on bottom floor panel or top of drawers/shelves in the same bay
            existingTargets.add(0f)
            for (other in shelves) {
                if (targetBayIndex == null || other.bayIndex == targetBayIndex) {
                    existingTargets.add(other.elevationYMm + other.thicknessMm)
                    existingTargets.add(other.elevationYMm)
                }
            }
            for (d in drawers) {
                if (targetBayIndex == null || d.bayIndex == targetBayIndex) {
                    existingTargets.add(d.elevationYMm + d.heightMm)
                }
            }
            for (sr in shoeRacks) {
                if (targetBayIndex == null || sr.bayIndex == targetBayIndex) {
                    existingTargets.add(sr.elevationYMm + sr.thicknessMm)
                    existingTargets.add(sr.elevationYMm)
                }
            }
        } else {
            for (other in shelves) {
                if (other.id != currentShelfId) {
                    // Coplanar shelf alignment across bays
                    existingTargets.add(other.elevationYMm)
                    if (other.thicknessMm != shelfThicknessMm) {
                        existingTargets.add(other.elevationYMm + other.thicknessMm - shelfThicknessMm)
                    }
                    // Stacking directly on top of a shelf in the same bay
                    if (targetBayIndex == null || other.bayIndex == targetBayIndex) {
                        existingTargets.add(other.elevationYMm + other.thicknessMm)
                    }
                }
            }
            for (d in drawers) {
                if (d.id != currentShelfId) {
                    // Coplanar alignment
                    existingTargets.add(d.elevationYMm)
                    // Stacking directly on top or underneath a drawer in the same bay
                    if (targetBayIndex == null || d.bayIndex == targetBayIndex) {
                        existingTargets.add(d.elevationYMm + d.heightMm)
                        val under = d.elevationYMm - shelfThicknessMm
                        if (under >= 0f) existingTargets.add(under)
                    }
                }
            }
            for (r in clothesRails) {
                if (r.id != currentShelfId) {
                    // Coplanar clothes rail alignment across bays
                    existingTargets.add(r.elevationYMm)
                }
            }
            for (sr in shoeRacks) {
                if (sr.id != currentShelfId) {
                    existingTargets.add(sr.elevationYMm)
                    if (targetBayIndex == null || sr.bayIndex == targetBayIndex) {
                        existingTargets.add(sr.elevationYMm + sr.totalVerticalSpanMm(interiorDepthMm))
                    }
                }
            }
            for (cd in cabinetDoors) {
                if (cd.id != currentShelfId) {
                    existingTargets.add(cd.elevationYMm)
                    if (targetBayIndex == null || cd.bayIndex == targetBayIndex) {
                        existingTargets.add(cd.elevationYMm + cd.heightMm)
                    }
                }
            }
            // Flush on bottom floor panel
            existingTargets.add(0f)
        }

        var bestMatch: Float? = null
        var bestDist = Float.MAX_VALUE

        for (target in existingTargets) {
            val dist = kotlin.math.abs(rawElevationYMm - target)
            if (dist <= snapThresholdMm && dist < bestDist) {
                bestDist = dist
                bestMatch = target
            }
        }
        if (bestMatch != null) return bestMatch

        // Priority 2: Structural divisions (midpoint, thirds, quarters)
        val structuralTargets = mutableListOf<Float>()

        // Half height (two equal clear openings)
        val midH = (innerH - shelfThicknessMm) * 0.5f
        if (midH in 20f..(innerH - shelfThicknessMm - 20f)) {
            structuralTargets.add(midH)
        }

        // Thirds (three equal clear openings)
        val thirdClearH = (innerH - 2f * shelfThicknessMm) / 3f
        if (thirdClearH > 50f) {
            val t1 = thirdClearH
            val t2 = thirdClearH * 2f + shelfThicknessMm
            if (t1 in 20f..(innerH - shelfThicknessMm - 20f)) structuralTargets.add(t1)
            if (t2 in 20f..(innerH - shelfThicknessMm - 20f)) structuralTargets.add(t2)
        }

        // Quarters (four equal clear openings)
        val quarterClearH = (innerH - 3f * shelfThicknessMm) / 4f
        if (quarterClearH > 50f) {
            val q1 = quarterClearH
            val q2 = quarterClearH * 2f + shelfThicknessMm
            val q3 = quarterClearH * 3f + 2f * shelfThicknessMm
            if (q1 in 20f..(innerH - shelfThicknessMm - 20f)) structuralTargets.add(q1)
            if (q2 in 20f..(innerH - shelfThicknessMm - 20f)) structuralTargets.add(q2)
            if (q3 in 20f..(innerH - shelfThicknessMm - 20f)) structuralTargets.add(q3)
        }

        for (target in structuralTargets) {
            val dist = kotlin.math.abs(rawElevationYMm - target)
            if (dist <= snapThresholdMm && dist < bestDist) {
                bestDist = dist
                bestMatch = target
            }
        }

        return bestMatch ?: rawElevationYMm
    }

    /**
     * Calculates the magnetically snapped horizontal offset for a vertical divider:
     * 1. Priority 1: Snaps to existing vertical dividers (e.g., partial dividers above/below shelves)
     *    within [snapThresholdMm] to preserve clean vertical column continuity.
     * 2. Priority 2: Snaps to container center (two equal bays), thirds (three equal bays),
     *    or midpoints of existing bays within [snapThresholdMm].
     * If no target is within [snapThresholdMm], returns [rawOffsetXMm].
     */
    fun findSnapDividerOffset(
        currentDividerId: String?,
        rawOffsetXMm: Float,
        dividerThicknessMm: Float = effectivePanelThicknessMm,
        snapThresholdMm: Float = 28f,
    ): Float {
        if (snapThresholdMm <= 0f) return rawOffsetXMm
        val innerW = interiorWidthMm

        // Priority 1: Other existing vertical dividers (for stacked / partial dividers)
        val existingTargets = dividers
            .filter { it.id != currentDividerId }
            .map { it.offsetXMm }

        var bestMatch: Float? = null
        var bestDist = Float.MAX_VALUE

        for (target in existingTargets) {
            val dist = kotlin.math.abs(rawOffsetXMm - target)
            if (dist <= snapThresholdMm && dist < bestDist) {
                bestDist = dist
                bestMatch = target
            }
        }
        if (bestMatch != null) return bestMatch

        // Priority 2: Structural divisions (midpoint, thirds, bay centers)
        val structuralTargets = mutableListOf<Float>()

        // Half width (two equal clear bays)
        val midW = (innerW - dividerThicknessMm) * 0.5f
        if (midW in 20f..(innerW - dividerThicknessMm - 20f)) {
            structuralTargets.add(midW)
        }

        // Thirds (three equal clear bays)
        val thirdClearW = (innerW - 2f * dividerThicknessMm) / 3f
        if (thirdClearW > 50f) {
            val t1 = thirdClearW
            val t2 = thirdClearW * 2f + dividerThicknessMm
            if (t1 in 20f..(innerW - dividerThicknessMm - 20f)) structuralTargets.add(t1)
            if (t2 in 20f..(innerW - dividerThicknessMm - 20f)) structuralTargets.add(t2)
        }

        // Midpoints of existing bays (bisecting an existing bay)
        val bays = getBays()
        for (bay in bays) {
            if (bay.widthMm > dividerThicknessMm * 2f + 50f) {
                val bayMid = bay.startXMm + (bay.widthMm - dividerThicknessMm) * 0.5f
                if (bayMid in 20f..(innerW - dividerThicknessMm - 20f)) {
                    structuralTargets.add(bayMid)
                }
            }
        }

        for (target in structuralTargets) {
            val dist = kotlin.math.abs(rawOffsetXMm - target)
            if (dist <= snapThresholdMm && dist < bestDist) {
                bestDist = dist
                bestMatch = target
            }
        }

        return bestMatch ?: rawOffsetXMm
    }

    fun withAddedDivider(
        offsetXMm: Float? = null,
        targetYMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val innerW = interiorWidthMm
        val innerH = interiorHeightMm
        val thickness = effectivePanelThicknessMm
        val targetOffset = offsetXMm ?: run {
            val bays = getBays()
            val widest = bays.maxByOrNull { it.widthMm } ?: bays.first()
            widest.startXMm + (widest.widthMm - thickness) * 0.5f
        }
        val clampedOffset = targetOffset.coerceIn(20f, (innerW - thickness - 20f).coerceAtLeast(20f))
        val snappedOffset = if (offsetXMm != null && snapThresholdMm > 0f) {
            findSnapDividerOffset(
                currentDividerId = null,
                rawOffsetXMm = clampedOffset,
                dividerThicknessMm = thickness,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedOffset
        }
        val targetBay = findBayAtX(snappedOffset)

        val (startY, endY) = if (targetYMm != null) {
            findVerticalCompartmentRange(targetBay.index, targetYMm)
        } else {
            0f to innerH
        }

        val clampedStartY = startY.coerceIn(0f, innerH)
        val clampedEndY = endY.coerceIn(clampedStartY + 10f, innerH)

        val newDivider = VerticalDividerSpec(
            id = "div_${System.currentTimeMillis()}_${dividers.size + 1}",
            offsetXMm = snappedOffset,
            thicknessMm = thickness,
            startYMm = clampedStartY,
            endYMm = if (clampedEndY < innerH - 1f) clampedEndY else null,
        )
        return copy(dividers = dividers + newDivider)
    }

    fun withMovedDivider(
        id: String,
        newOffsetXMm: Float,
        targetYMm: Float? = null,
        maintainCompartmentIfPossible: Boolean = true,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val div = dividers.firstOrNull { it.id == id } ?: return this
        val innerW = interiorWidthMm
        val innerH = interiorHeightMm
        val thickness = div.thicknessMm
        val clampedOffset = newOffsetXMm.coerceIn(10f, (innerW - thickness - 10f).coerceAtLeast(10f))
        val snappedOffset = if (snapThresholdMm > 0f) {
            findSnapDividerOffset(
                currentDividerId = id,
                rawOffsetXMm = clampedOffset,
                dividerThicknessMm = thickness,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedOffset
        }
        val targetBay = findBayAtX(snappedOffset)

        val (newStartY, newEndY) = if (targetYMm != null) {
            findVerticalCompartmentRange(targetBay.index, targetYMm)
        } else if (maintainCompartmentIfPossible && (div.startYMm > 10f || (div.endYMm != null && div.endYMm < innerH - 10f))) {
            val currentMidY = (div.startYMm + div.effectiveEndY(innerH)) * 0.5f
            findVerticalCompartmentRange(targetBay.index, currentMidY)
        } else {
            0f to innerH
        }

        val clampedStartY = newStartY.coerceIn(0f, innerH)
        val clampedEndY = newEndY.coerceIn(clampedStartY + 10f, innerH)

        return withUpdatedDivider(id) {
            it.copy(
                offsetXMm = snappedOffset,
                startYMm = clampedStartY,
                endYMm = if (clampedEndY < innerH - 1f) clampedEndY else null,
            )
        }
    }

    fun withMovedShelf(
        id: String,
        newElevationYMm: Float,
        targetXMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val shelf = shelves.firstOrNull { it.id == id } ?: return this
        val innerH = interiorHeightMm
        val thickness = shelf.thicknessMm
        val clampedElev = newElevationYMm.coerceIn(10f, (innerH - thickness - 10f).coerceAtLeast(10f))
        val newBayIndex = if (targetXMm != null) {
            findBayAtX(targetXMm).index
        } else {
            shelf.bayIndex
        }
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = id,
                rawElevationYMm = clampedElev,
                targetBayIndex = newBayIndex,
                shelfThicknessMm = thickness,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedElev
        }

        return withUpdatedShelf(id) {
            it.copy(
                elevationYMm = snappedElev,
                bayIndex = newBayIndex.coerceIn(0, getBays().lastIndex),
            )
        }
    }

    fun withAddedShelf(
        bayIndex: Int? = null,
        elevationYMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val innerH = interiorHeightMm
        val thickness = effectivePanelThicknessMm
        val bays = getBays()
        val targetBay = (bayIndex ?: 0).coerceIn(0, bays.lastIndex)
        val rawElev = (elevationYMm ?: (innerH * 0.5f)).coerceIn(20f, (innerH - thickness - 20f).coerceAtLeast(20f))
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = null,
                rawElevationYMm = rawElev,
                targetBayIndex = targetBay,
                shelfThicknessMm = thickness,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            rawElev
        }
        val newShelf = HorizontalShelfSpec(
            id = "shelf_${System.currentTimeMillis()}_${shelves.size + 1}",
            bayIndex = targetBay,
            elevationYMm = snappedElev,
            thicknessMm = thickness,
        )
        return copy(shelves = shelves + newShelf)
    }

    fun withAddedDrawer(
        bayIndex: Int? = null,
        elevationYMm: Float? = null,
        heightMm: Float = 200f,
        snapThresholdMm: Float = 28f,
        frontType: DrawerFrontType = DrawerFrontType.HANDLE,
    ): ClosetContainerSpec {
        val innerH = interiorHeightMm
        val bays = getBays()
        val targetBay = (bayIndex ?: 0).coerceIn(0, bays.lastIndex)
        val defaultElev = run {
            val bayDrawers = drawers.filter { it.bayIndex == targetBay }
            if (bayDrawers.isEmpty()) 0f
            else {
                val highest = bayDrawers.maxByOrNull { it.elevationYMm + it.heightMm }!!
                highest.elevationYMm + highest.heightMm
            }
        }
        val rawElev = (elevationYMm ?: defaultElev).coerceIn(0f, (innerH - heightMm).coerceAtLeast(0f))
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = null,
                rawElevationYMm = rawElev,
                targetBayIndex = targetBay,
                shelfThicknessMm = heightMm,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            rawElev
        }
        val newDrawer = DrawerSpec(
            id = "drawer_${System.currentTimeMillis()}_${drawers.size + 1}",
            bayIndex = targetBay,
            elevationYMm = snappedElev,
            heightMm = heightMm,
            frontType = frontType,
        )
        return copy(drawers = drawers + newDrawer)
    }

    fun withMovedDrawer(
        id: String,
        newElevationYMm: Float,
        targetXMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val drawer = drawers.firstOrNull { it.id == id } ?: return this
        val innerH = interiorHeightMm
        val drawerH = drawer.heightMm
        val clampedElev = newElevationYMm.coerceIn(0f, (innerH - drawerH).coerceAtLeast(0f))
        val newBayIndex = if (targetXMm != null) {
            findBayAtX(targetXMm).index
        } else {
            drawer.bayIndex
        }
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = id,
                rawElevationYMm = clampedElev,
                targetBayIndex = newBayIndex,
                shelfThicknessMm = drawerH,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedElev
        }

        return withUpdatedDrawer(id) {
            it.copy(
                elevationYMm = snappedElev,
                bayIndex = newBayIndex.coerceIn(0, getBays().lastIndex),
            )
        }
    }

    fun withUpdatedDivider(id: String, transform: (VerticalDividerSpec) -> VerticalDividerSpec): ClosetContainerSpec {
        return copy(dividers = dividers.map { if (it.id == id) transform(it) else it })
    }

    fun withUpdatedShelf(id: String, transform: (HorizontalShelfSpec) -> HorizontalShelfSpec): ClosetContainerSpec {
        return copy(shelves = shelves.map { if (it.id == id) transform(it) else it })
    }

    fun withUpdatedDrawer(id: String, transform: (DrawerSpec) -> DrawerSpec): ClosetContainerSpec {
        return copy(drawers = drawers.map { if (it.id == id) transform(it) else it })
    }

    fun withAddedClothesRail(
        bayIndex: Int? = null,
        elevationYMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val innerH = interiorHeightMm
        val bays = getBays()
        val targetBay = (bayIndex ?: 0).coerceIn(0, bays.lastIndex)
        val defaultElev = run {
            val (compY0, compY1) = findVerticalCompartmentRange(targetBay, innerH * 0.5f)
            (compY0 + compY1) * 0.5f
        }
        val rawElev = (elevationYMm ?: defaultElev).coerceIn(25f, (innerH - 25f).coerceAtLeast(25f))
        val snappedElev = if (elevationYMm != null && snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = null,
                rawElevationYMm = rawElev,
                targetBayIndex = targetBay,
                shelfThicknessMm = 25f,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            rawElev
        }
        val newRail = ClothesRailSpec(
            id = "rail_${System.currentTimeMillis()}_${clothesRails.size + 1}",
            bayIndex = targetBay,
            elevationYMm = snappedElev,
        )
        return copy(clothesRails = clothesRails + newRail)
    }

    fun withMovedClothesRail(
        id: String,
        newElevationYMm: Float,
        targetXMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val rail = clothesRails.firstOrNull { it.id == id } ?: return this
        val innerH = interiorHeightMm
        val clampedElev = newElevationYMm.coerceIn(25f, (innerH - 25f).coerceAtLeast(25f))
        val newBayIndex = if (targetXMm != null) {
            findBayAtX(targetXMm).index
        } else {
            rail.bayIndex
        }
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = id,
                rawElevationYMm = clampedElev,
                targetBayIndex = newBayIndex,
                shelfThicknessMm = rail.diameterMm,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedElev
        }

        return withUpdatedClothesRail(id) {
            it.copy(
                elevationYMm = snappedElev,
                bayIndex = newBayIndex.coerceIn(0, getBays().lastIndex),
            )
        }
    }

    fun withUpdatedClothesRail(id: String, transform: (ClothesRailSpec) -> ClothesRailSpec): ClosetContainerSpec {
        return copy(clothesRails = clothesRails.map { if (it.id == id) transform(it) else it })
    }

    fun findSnapPoleOffset(
        currentPoleId: String?,
        rawOffsetXMm: Float,
        targetBayIndex: Int? = null,
        snapThresholdMm: Float = 28f,
    ): Float {
        if (snapThresholdMm <= 0f) return rawOffsetXMm
        val bays = getBays()
        val targetBay = bays.getOrElse(targetBayIndex ?: 0) { bays.last() }
        val existingTargets = mutableListOf<Float>()

        // Priority 1: Snap to center of target bay
        existingTargets.add((targetBay.startXMm + targetBay.endXMm) * 0.5f)

        // Priority 2: Other vertical poles in other bays/compartments
        for (other in verticalPoles) {
            if (other.id != currentPoleId) {
                val otherBay = bays.getOrElse(other.bayIndex) { bays.last() }
                existingTargets.add(other.effectiveX(otherBay.startXMm, otherBay.endXMm))
            }
        }

        // Priority 3: Equal thirds of the bay
        val bayW = targetBay.widthMm
        if (bayW > 150f) {
            existingTargets.add(targetBay.startXMm + bayW / 3f)
            existingTargets.add(targetBay.startXMm + bayW * 2f / 3f)
        }

        var bestMatch: Float? = null
        var bestDist = Float.MAX_VALUE

        for (target in existingTargets) {
            val dist = kotlin.math.abs(rawOffsetXMm - target)
            if (dist <= snapThresholdMm && dist < bestDist) {
                bestDist = dist
                bestMatch = target
            }
        }
        return bestMatch ?: rawOffsetXMm
    }

    fun withAddedVerticalPole(
        bayIndex: Int? = null,
        offsetXMm: Float? = null,
        targetYMm: Float? = null,
        diameterMm: Float = 25f,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val innerH = interiorHeightMm
        val bays = getBays()
        val targetBay = if (offsetXMm != null) findBayAtX(offsetXMm) else bays.getOrElse(bayIndex ?: 0) { bays.last() }
        val radius = diameterMm * 0.5f
        val (compY0, compY1) = findVerticalCompartmentRange(targetBay.index, targetYMm ?: (innerH * 0.5f))
        val defaultX = (targetBay.startXMm + targetBay.endXMm) * 0.5f
        val rawX = (offsetXMm ?: defaultX).coerceIn(
            targetBay.startXMm + radius + 10f,
            (targetBay.endXMm - radius - 10f).coerceAtLeast(targetBay.startXMm + radius + 10f)
        )
        val snappedX = if (offsetXMm != null && snapThresholdMm > 0f) {
            findSnapPoleOffset(
                currentPoleId = null,
                rawOffsetXMm = rawX,
                targetBayIndex = targetBay.index,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            rawX
        }
        val newPole = VerticalPoleSpec(
            id = "pole_${System.currentTimeMillis()}_${verticalPoles.size + 1}",
            bayIndex = targetBay.index,
            offsetXMm = snappedX,
            diameterMm = diameterMm,
            bottomElevationYMm = compY0,
            topElevationYMm = compY1,
        )
        return copy(verticalPoles = verticalPoles + newPole)
    }

    fun withMovedVerticalPole(
        id: String,
        newOffsetXMm: Float,
        targetYMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val pole = verticalPoles.firstOrNull { it.id == id } ?: return this
        val bays = getBays()
        val targetBay = findBayAtX(newOffsetXMm)
        val radius = pole.diameterMm * 0.5f
        val clampedX = newOffsetXMm.coerceIn(
            targetBay.startXMm + radius + 10f,
            (targetBay.endXMm - radius - 10f).coerceAtLeast(targetBay.startXMm + radius + 10f)
        )
        val snappedX = if (snapThresholdMm > 0f) {
            findSnapPoleOffset(
                currentPoleId = id,
                rawOffsetXMm = clampedX,
                targetBayIndex = targetBay.index,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedX
        }

        val (newY0, newY1) = if (targetYMm != null) {
            findVerticalCompartmentRange(targetBay.index, targetYMm)
        } else {
            (pole.bottomElevationYMm ?: 0f) to (pole.topElevationYMm ?: interiorHeightMm)
        }

        return withUpdatedVerticalPole(id) {
            it.copy(
                bayIndex = targetBay.index,
                offsetXMm = snappedX,
                bottomElevationYMm = newY0,
                topElevationYMm = newY1,
            )
        }
    }

    fun withUpdatedVerticalPole(id: String, transform: (VerticalPoleSpec) -> VerticalPoleSpec): ClosetContainerSpec {
        return copy(verticalPoles = verticalPoles.map { if (it.id == id) transform(it) else it })
    }

    fun withAddedShoeRack(
        bayIndex: Int? = null,
        elevationYMm: Float? = null,
        inclinationAngleDeg: Float = 18f,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val innerH = interiorHeightMm
        val bays = getBays()
        val targetBay = (bayIndex ?: 0).coerceIn(0, bays.lastIndex)
        val defaultElev = run {
            val bayRacks = shoeRacks.filter { it.bayIndex == targetBay }
            if (bayRacks.isEmpty()) 0f
            else {
                val highest = bayRacks.maxByOrNull { it.elevationYMm }!!
                highest.elevationYMm + highest.totalVerticalSpanMm(interiorDepthMm) + 30f
            }
        }
        val rawElev = (elevationYMm ?: defaultElev).coerceIn(0f, (innerH - 100f).coerceAtLeast(0f))
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = null,
                rawElevationYMm = rawElev,
                targetBayIndex = targetBay,
                shelfThicknessMm = 18f,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            rawElev
        }
        val newRack = ShoeRackSpec(
            id = "shoerack_${System.currentTimeMillis()}_${shoeRacks.size + 1}",
            bayIndex = targetBay,
            elevationYMm = snappedElev,
            inclinationAngleDeg = inclinationAngleDeg,
        )
        return copy(shoeRacks = shoeRacks + newRack)
    }

    fun withMovedShoeRack(
        id: String,
        newElevationYMm: Float,
        targetXMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val rack = shoeRacks.firstOrNull { it.id == id } ?: return this
        val innerH = interiorHeightMm
        val maxElev = rack.maxElevation(innerH, interiorDepthMm)
        val clampedElev = newElevationYMm.coerceIn(0f, maxElev)
        val newBayIndex = if (targetXMm != null) {
            findBayAtX(targetXMm).index
        } else {
            rack.bayIndex
        }
        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = id,
                rawElevationYMm = clampedElev,
                targetBayIndex = newBayIndex,
                shelfThicknessMm = rack.thicknessMm,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            clampedElev
        }

        return withUpdatedShoeRack(id) {
            it.copy(
                elevationYMm = snappedElev,
                bayIndex = newBayIndex.coerceIn(0, getBays().lastIndex),
            )
        }
    }

    fun withUpdatedShoeRack(id: String, transform: (ShoeRackSpec) -> ShoeRackSpec): ClosetContainerSpec {
        return copy(shoeRacks = shoeRacks.map { if (it.id == id) transform(it) else it })
    }

    fun withAddedCabinetDoors(
        bayIndex: Int? = null,
        targetYMm: Float? = null,
        doorCount: Int = 2,
        heightMm: Float? = null,
        elevationYMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val innerH = interiorHeightMm
        val bays = getBays()
        val targetBay = (bayIndex ?: 0).coerceIn(0, bays.lastIndex)
        val (spanY0, spanY1) = if (elevationYMm != null && heightMm != null) {
            elevationYMm to (elevationYMm + heightMm)
        } else {
            findDoorsPlacementSpan(targetBay, targetYMm ?: (innerH * 0.5f))
        }
        val computedH = (heightMm ?: (spanY1 - spanY0)).coerceIn(100f, innerH)
        val computedElev = (elevationYMm ?: spanY0).coerceIn(0f, (innerH - computedH).coerceAtLeast(0f))

        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = "doors_new",
                rawElevationYMm = computedElev,
                targetBayIndex = targetBay,
                shelfThicknessMm = computedH,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            computedElev
        }
        val clampedElev = snappedElev.coerceIn(0f, (innerH - computedH).coerceAtLeast(0f))
        val newDoors = CabinetDoorsSpec(
            id = "doors_${System.currentTimeMillis()}_${cabinetDoors.size + 1}",
            bayIndex = targetBay,
            doorCount = doorCount.coerceIn(1, 2),
            elevationYMm = clampedElev,
            heightMm = computedH,
        )
        return copy(cabinetDoors = cabinetDoors + newDoors)
    }

    fun withMovedCabinetDoors(
        id: String,
        newElevationYMm: Float,
        targetXMm: Float? = null,
        snapThresholdMm: Float = 28f,
    ): ClosetContainerSpec {
        val doors = cabinetDoors.firstOrNull { it.id == id } ?: return this
        val innerH = interiorHeightMm
        val newBayIndex = if (targetXMm != null) {
            findBayAtX(targetXMm).index
        } else {
            doors.bayIndex
        }

        val (finalElev, finalHeight) = if (newBayIndex != doors.bayIndex) {
            val (spanY0, spanY1) = findDoorsPlacementSpan(newBayIndex, newElevationYMm)
            val adaptedH = (spanY1 - spanY0).coerceIn(100f, innerH)
            spanY0 to adaptedH
        } else {
            val comps = getBayCompartments(newBayIndex)
            if (comps.size > 1) {
                val (spanY0, spanY1) = findDoorsPlacementSpan(newBayIndex, newElevationYMm)
                val targetSpanH = spanY1 - spanY0
                if (comps.any { kotlin.math.abs(doors.heightMm - it.heightMm) < 50f } ||
                    kotlin.math.abs(doors.heightMm - targetSpanH) < 60f ||
                    kotlin.math.abs(newElevationYMm - spanY0) < 60f
                ) {
                    spanY0 to targetSpanH.coerceIn(100f, innerH)
                } else {
                    val clampedElev = newElevationYMm.coerceIn(0f, (innerH - doors.heightMm).coerceAtLeast(0f))
                    clampedElev to doors.heightMm
                }
            } else {
                val clampedElev = newElevationYMm.coerceIn(0f, (innerH - doors.heightMm).coerceAtLeast(0f))
                clampedElev to doors.heightMm
            }
        }

        val snappedElev = if (snapThresholdMm > 0f) {
            findSnapElevation(
                currentShelfId = id,
                rawElevationYMm = finalElev,
                targetBayIndex = newBayIndex,
                shelfThicknessMm = finalHeight,
                snapThresholdMm = snapThresholdMm,
            )
        } else {
            finalElev
        }

        return withUpdatedCabinetDoors(id) {
            it.copy(
                elevationYMm = snappedElev.coerceIn(0f, (innerH - finalHeight).coerceAtLeast(0f)),
                heightMm = finalHeight,
                bayIndex = newBayIndex.coerceIn(0, getBays().lastIndex),
            )
        }
    }

    fun withUpdatedCabinetDoors(id: String, transform: (CabinetDoorsSpec) -> CabinetDoorsSpec): ClosetContainerSpec {
        return copy(cabinetDoors = cabinetDoors.map { if (it.id == id) transform(it) else it })
    }

    fun withRemovedPartition(id: String): ClosetContainerSpec {
        val remainingDividers = dividers.filterNot { it.id == id }
        val remainingShelves = shelves.filterNot { it.id == id }
        val remainingDrawers = drawers.filterNot { it.id == id }
        val remainingRails = clothesRails.filterNot { it.id == id }
        val remainingPoles = verticalPoles.filterNot { it.id == id }
        val remainingRacks = shoeRacks.filterNot { it.id == id }
        val remainingDoors = cabinetDoors.filterNot { it.id == id }
        val maxBayIdx = remainingDividers.size
        val reclampedShelves = remainingShelves.map {
            if (it.bayIndex > maxBayIdx) it.copy(bayIndex = maxBayIdx) else it
        }
        val reclampedDrawers = remainingDrawers.map {
            if (it.bayIndex > maxBayIdx) it.copy(bayIndex = maxBayIdx) else it
        }
        val reclampedRails = remainingRails.map {
            if (it.bayIndex > maxBayIdx) it.copy(bayIndex = maxBayIdx) else it
        }
        val reclampedPoles = remainingPoles.map {
            if (it.bayIndex > maxBayIdx) it.copy(bayIndex = maxBayIdx) else it
        }
        val reclampedRacks = remainingRacks.map {
            if (it.bayIndex > maxBayIdx) it.copy(bayIndex = maxBayIdx) else it
        }
        val reclampedDoors = remainingDoors.map {
            if (it.bayIndex > maxBayIdx) it.copy(bayIndex = maxBayIdx) else it
        }
        return copy(
            dividers = remainingDividers,
            shelves = reclampedShelves,
            drawers = reclampedDrawers,
            clothesRails = reclampedRails,
            verticalPoles = reclampedPoles,
            shoeRacks = reclampedRacks,
            cabinetDoors = reclampedDoors,
        )
    }

    fun withMaterialTexture(textureId: String?): ClosetContainerSpec = copy(materialTextureId = textureId)
    fun withGrainDirection(horizontal: Boolean): ClosetContainerSpec = copy(isGrainHorizontal = horizontal)
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
    val wallTextureId: String? = null,
    val floorTextureId: String? = null,
    val defaultMelamineTextureId: String? = null,
    val defaultGrainHorizontal: Boolean = false,
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
     * Also keeps corner-fused containers anchored to their fused corner vertex!
     */
    fun clampContainer(container: ClosetContainerSpec): ClosetContainerSpec {
        val maxW = wallLengthMm(container.wall).coerceAtLeast(0f)
        val maxH = clearHeightMm.coerceAtLeast(0f)
        val maxD = maxContainerDepthForWall(container.wall).coerceAtLeast(0f)

        val clampedW = when {
            container.fuseStartCorner && container.fuseEndCorner -> maxW
            else -> container.widthMm.coerceIn(0f, maxW)
        }
        val clampedH = container.heightMm.coerceIn(0f, maxH)
        val clampedD = container.depthMm.coerceIn(0f, maxD)
        val maxOff = (maxW - clampedW).coerceAtLeast(0f)
        val clampedOff = when {
            container.fuseStartCorner -> 0f
            container.fuseEndCorner -> maxOff
            else -> container.offsetMm.coerceIn(0f, maxOff)
        }

        val innerW = (clampedW - container.effectivePanelThicknessMm * 2f).coerceAtLeast(0f)
        val innerH = (clampedH - container.effectivePlinthHeightMm - container.effectivePanelThicknessMm * 2f).coerceAtLeast(0f)
        val clampedDividers = container.dividers.map { div ->
            val maxOffset = (innerW - div.thicknessMm).coerceAtLeast(0f)
            val clampedStart = div.startYMm.coerceIn(0f, (innerH - 10f).coerceAtLeast(0f))
            val clampedEnd = div.endYMm?.coerceIn(clampedStart + 10f, innerH)
            div.copy(
                offsetXMm = div.offsetXMm.coerceIn(0f, maxOffset),
                startYMm = clampedStart,
                endYMm = if (clampedEnd != null && clampedEnd < innerH - 1f) clampedEnd else null,
            )
        }
        val maxBayIdx = clampedDividers.size
        val clampedShelves = container.shelves.map { shelf ->
            val maxElev = (innerH - shelf.thicknessMm).coerceAtLeast(0f)
            shelf.copy(
                bayIndex = shelf.bayIndex.coerceIn(0, maxBayIdx),
                elevationYMm = shelf.elevationYMm.coerceIn(0f, maxElev),
            )
        }
        val clampedDrawers = container.drawers.map { drawer ->
            val maxElev = (innerH - drawer.heightMm).coerceAtLeast(0f)
            drawer.copy(
                bayIndex = drawer.bayIndex.coerceIn(0, maxBayIdx),
                elevationYMm = drawer.elevationYMm.coerceIn(0f, maxElev),
            )
        }
        val clampedRails = container.clothesRails.map { rail ->
            val maxElev = (innerH - rail.diameterMm).coerceAtLeast(0f)
            rail.copy(
                bayIndex = rail.bayIndex.coerceIn(0, maxBayIdx),
                elevationYMm = rail.elevationYMm.coerceIn(0f, maxElev),
            )
        }
        val clampedPoles = container.verticalPoles.map { pole ->
            val radius = pole.diameterMm * 0.5f
            val clampedBay = pole.bayIndex.coerceIn(0, maxBayIdx)
            val bay = container.getBays().getOrElse(clampedBay) { container.getBays().last() }
            val minX = bay.startXMm + radius
            val maxX = (bay.endXMm - radius).coerceAtLeast(minX)
            val clampedX = pole.offsetXMm?.coerceIn(minX, maxX)
            val clampedY0 = pole.bottomElevationYMm?.coerceIn(0f, (innerH - 50f).coerceAtLeast(0f))
            val clampedY1 = pole.topElevationYMm?.coerceIn((clampedY0 ?: 0f) + 50f, innerH)
            pole.copy(
                bayIndex = clampedBay,
                offsetXMm = clampedX,
                bottomElevationYMm = clampedY0,
                topElevationYMm = clampedY1,
            )
        }
        val clampedRacks = container.shoeRacks.map { rack ->
            val maxElev = rack.maxElevation(innerH, container.interiorDepthMm)
            rack.copy(
                bayIndex = rack.bayIndex.coerceIn(0, maxBayIdx),
                elevationYMm = rack.elevationYMm.coerceIn(0f, maxElev),
            )
        }
        val clampedDoors = container.cabinetDoors.map { doors ->
            val clampedH = doors.heightMm.coerceIn(100f, innerH.coerceAtLeast(100f))
            val maxElev = (innerH - clampedH).coerceAtLeast(0f)
            doors.copy(
                bayIndex = doors.bayIndex.coerceIn(0, maxBayIdx),
                elevationYMm = doors.elevationYMm.coerceIn(0f, maxElev),
                heightMm = clampedH,
                doorCount = doors.doorCount.coerceIn(1, 2),
            )
        }

        return container.copy(
            widthMm = clampedW,
            heightMm = clampedH,
            depthMm = clampedD,
            offsetMm = clampedOff,
            dividers = clampedDividers,
            shelves = clampedShelves,
            drawers = clampedDrawers,
            clothesRails = clampedRails,
            verticalPoles = clampedPoles,
            shoeRacks = clampedRacks,
            cabinetDoors = clampedDoors,
        )
    }

    /** Interior angle (in degrees) at the given [corner]. */
    fun cornerAngleDeg(corner: RoomCorner): Float = when (corner) {
        RoomCorner.NW -> angleACDeg
        RoomCorner.NE -> angleABDeg
        RoomCorner.SE -> angleBEDeg
        RoomCorner.SW -> angleCDDeg
    }

    /**
     * Detects all corners where two containers on adjacent walls intersect, overlap, or are
     * already fused in an L-corner joint.
     */
    fun detectCornerIntersections(): List<CornerIntersectionCandidate> {
        if (containers.size < 2) return emptyList()
        val results = mutableListOf<CornerIntersectionCandidate>()

        for (corner in RoomCorner.values()) {
            val incLen = wallLengthMm(corner.incomingWall).coerceAtLeast(0f)
            val incoming = containers
                .filter { it.wall == corner.incomingWall }
                .map { clampContainer(it) }
                .minByOrNull { (incLen - (it.offsetMm + it.widthMm)).coerceAtLeast(0f) }
                ?: continue

            val outgoing = containers
                .filter { it.wall == corner.outgoingWall }
                .map { clampContainer(it) }
                .minByOrNull { it.offsetMm.coerceAtLeast(0f) }
                ?: continue

            val thetaRad = Math.toRadians(cornerAngleDeg(corner).coerceIn(15f, 165f).toDouble()).toFloat()
            val sinT = sin(thetaRad).coerceAtLeast(0.15f)
            val cosT = cos(thetaRad)
            val dInc = incoming.depthMm.coerceAtLeast(350f)
            val dOut = outgoing.depthMm.coerceAtLeast(350f)

            // Exact geometric reach of each container's depth into the adjacent wall at angle theta:
            val reachIncFromCorner = ((dOut + dInc * cosT) / sinT).coerceAtLeast(180f)
            val reachOutFromCorner = ((dInc + dOut * cosT) / sinT).coerceAtLeast(180f)

            val distIncToCorner = (incLen - (incoming.offsetMm + incoming.widthMm)).coerceAtLeast(0f)
            val distOutToCorner = outgoing.offsetMm.coerceAtLeast(0f)

            val isAlreadyFused = incoming.fuseEndCorner && outgoing.fuseStartCorner
            // If both containers enter the corner collision zone (plus 120mm snap margin), offer/maintain fusion
            val isIntersectingOrClose = distIncToCorner <= reachIncFromCorner + 120f &&
                distOutToCorner <= reachOutFromCorner + 120f

            if (isAlreadyFused || isIntersectingOrClose) {
                results += CornerIntersectionCandidate(
                    corner = corner,
                    incomingContainer = incoming,
                    outgoingContainer = outgoing,
                    isFused = isAlreadyFused,
                )
            }
        }
        return results
    }

    /**
     * Fuses (or unfuses) the two containers meeting at [corner].
     * When fused:
     * - Snaps the incoming container to the end of its wall (`fuseEndCorner = true`)
     * - Snaps the outgoing container to the start of its wall (`fuseStartCorner = true`)
     * - Unifies their height so the top roof and bottom floor meet seamlessly at the corner,
     *   while strictly preserving each container's independent `widthMm` and `depthMm`!
     */
    fun withCornerFusion(corner: RoomCorner, fuse: Boolean = true): RoomSpec {
        val candidate = detectCornerIntersections().firstOrNull { it.corner == corner } ?: return this
        val incId = candidate.incomingContainer.id
        val outId = candidate.outgoingContainer.id
        val unifiedH = candidate.incomingContainer.heightMm
        val incWallLen = wallLengthMm(corner.incomingWall).coerceAtLeast(0f)

        return copy(
            containers = containers.map { item ->
                when (item.id) {
                    incId -> if (fuse) {
                        val minW = (candidate.outgoingContainer.depthMm + 250f).coerceAtMost(incWallLen)
                        val newW = item.widthMm.coerceAtLeast(minW).coerceAtMost(incWallLen)
                        clampContainer(
                            item.copy(
                                fuseEndCorner = true,
                                widthMm = newW,
                                offsetMm = (incWallLen - newW).coerceAtLeast(0f),
                                heightMm = unifiedH,
                            ),
                        )
                    } else {
                        clampContainer(item.copy(fuseEndCorner = false))
                    }
                    outId -> if (fuse) {
                        val outWallLen = wallLengthMm(corner.outgoingWall).coerceAtLeast(0f)
                        val minW = (candidate.incomingContainer.depthMm + 250f).coerceAtMost(outWallLen)
                        val newW = item.widthMm.coerceAtLeast(minW).coerceAtMost(outWallLen)
                        clampContainer(
                            item.copy(
                                fuseStartCorner = true,
                                widthMm = newW,
                                offsetMm = 0f,
                                heightMm = unifiedH,
                            ),
                        )
                    } else {
                        clampContainer(item.copy(fuseStartCorner = false))
                    }
                    else -> clampContainer(item)
                }
            },
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
            materialTextureId = defaultMelamineTextureId,
            isGrainHorizontal = defaultGrainHorizontal,
        )
        val clamped = clampContainer(raw)
        return copy(containers = containers + clamped)
    }

    /** Updates an existing container by [id], clamping its bounds to the room and syncing fused corner height. */
    fun withUpdatedContainer(id: String, transform: (ClosetContainerSpec) -> ClosetContainerSpec): RoomSpec {
        val updatedList = containers.map { item ->
            if (item.id == id) clampContainer(transform(item)) else clampContainer(item)
        }
        val edited = updatedList.firstOrNull { it.id == id } ?: return copy(containers = updatedList)
        // If the edited container is corner-fused with a neighbor, keep their heights synchronized
        val synced = updatedList.map { other ->
            if (other.id == edited.id) {
                other
            } else {
                val sharesEndCorner = edited.fuseEndCorner && other.fuseStartCorner && other.wall == edited.wall.next()
                val sharesStartCorner = edited.fuseStartCorner && other.fuseEndCorner && other.wall == edited.wall.prev()
                if (sharesEndCorner || sharesStartCorner) {
                    clampContainer(other.copy(heightMm = edited.heightMm))
                } else {
                    other
                }
            }
        }
        return copy(containers = synced)
    }

    /** Moves a closet along its current wall without resizing it or moving its neighbors. */
    fun withMovedContainer(id: String, targetOffsetMm: Float): RoomSpec {
        val moving = containers.firstOrNull { it.id == id } ?: return this
        val detached = moving.copy(fuseStartCorner = false, fuseEndCorner = false)
        val maxOffset = (wallLengthMm(detached.wall) - detached.widthMm).coerceAtLeast(0f)
        val requestedOffset = targetOffsetMm.coerceIn(0f, maxOffset)
        val movingRight = requestedOffset >= moving.offsetMm
        var allowedOffset = requestedOffset

        containers.asSequence()
            .filter { it.id != id && it.wall == detached.wall }
            .sortedBy { it.offsetMm }
            .forEach { other ->
                val otherStart = other.offsetMm
                val otherEnd = other.offsetMm + other.widthMm
                val overlaps = if (movingRight) {
                    otherStart >= moving.offsetMm + moving.widthMm && allowedOffset + detached.widthMm > otherStart
                } else {
                    otherEnd <= moving.offsetMm && allowedOffset < otherEnd
                }
                val targetOverlaps = allowedOffset < otherEnd && allowedOffset + detached.widthMm > otherStart
                if (overlaps || targetOverlaps) {
                    allowedOffset = if (movingRight) {
                        minOf(allowedOffset, (otherStart - detached.widthMm).coerceAtLeast(0f))
                    } else {
                        maxOf(allowedOffset, otherEnd.coerceAtMost(maxOffset))
                    }
                }
            }

        return copy(containers = containers.map { container ->
            if (container.id == id) clampContainer(detached.copy(offsetMm = allowedOffset)) else container
        })
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

    fun withWallTexture(textureId: String?): RoomSpec = copy(wallTextureId = textureId)
    fun withFloorTexture(textureId: String?): RoomSpec = copy(floorTextureId = textureId)
    fun withDefaultMelamineTexture(textureId: String?): RoomSpec = copy(defaultMelamineTextureId = textureId)
    fun withDefaultGrainHorizontal(horizontal: Boolean): RoomSpec =
        copy(
            defaultGrainHorizontal = horizontal,
            containers = containers.map { it.copy(isGrainHorizontal = horizontal) },
        )
    fun withAppliedMelamineToAllContainers(textureId: String?, horizontal: Boolean): RoomSpec =
        copy(
            defaultMelamineTextureId = textureId,
            defaultGrainHorizontal = horizontal,
            containers = containers.map { it.copy(materialTextureId = textureId, isGrainHorizontal = horizontal) },
        )

    /**
     * Performs a 3D ray-box intersection test (in millimetres) against all placed [containers],
     * returning the `id` of the closest hit container, or `null` if the ray hits empty room space.
     */
    fun pickContainerByRayMm(
        rayOriginX: Float,
        rayOriginY: Float,
        rayOriginZ: Float,
        rayDirX: Float,
        rayDirY: Float,
        rayDirZ: Float,
    ): String? {
        if (containers.isEmpty()) return null
        val nw = vertexNW
        val ne = vertexNE
        val se = vertexSE
        val sw = vertexSW

        var bestId: String? = null
        var bestT = Float.MAX_VALUE

        for (raw in containers) {
            val c = clampContainer(raw)
            val (pStart, pEnd) = when (c.wall) {
                WallIndex.NORTH -> nw to ne
                WallIndex.EAST -> ne to se
                WallIndex.SOUTH -> se to sw
                WallIndex.WEST -> sw to nw
            }
            val dx = pEnd.x - pStart.x
            val dz = pEnd.z - pStart.z
            val wallLen = sqrt(dx * dx + dz * dz)
            if (wallLen < 10f) continue

            val ux = dx / wallLen
            val uz = dz / wallLen
            val nx = -uz
            val nz = ux

            // Transform ray into container's wall-aligned local coordinate system (s, y, d)
            val relX = rayOriginX - pStart.x
            val relZ = rayOriginZ - pStart.z
            val origS = relX * ux + relZ * uz
            val origY = rayOriginY
            val origD = relX * nx + relZ * nz

            val dirS = rayDirX * ux + rayDirZ * uz
            val dirY = rayDirY
            val dirD = rayDirX * nx + rayDirZ * nz

            val minS = c.offsetMm
            val maxS = c.offsetMm + c.widthMm
            val minY = 0f
            val maxY = c.heightMm
            val minD = 0f
            val maxD = c.depthMm.coerceAtLeast(350f) + 12f

            var tMin = 0f
            var tMax = Float.MAX_VALUE

            fun clipSlab(orig: Float, dir: Float, bMin: Float, bMax: Float): Boolean {
                if (abs(dir) < 1e-6f) {
                    return orig in bMin..bMax
                }
                val inv = 1f / dir
                var t1 = (bMin - orig) * inv
                var t2 = (bMax - orig) * inv
                if (t1 > t2) {
                    val tmp = t1
                    t1 = t2
                    t2 = tmp
                }
                if (t1 > tMin) tMin = t1
                if (t2 < tMax) tMax = t2
                return tMin <= tMax
            }

            if (clipSlab(origS, dirS, minS, maxS) &&
                clipSlab(origY, dirY, minY, maxY) &&
                clipSlab(origD, dirD, minD, maxD)
            ) {
                val hitT = if (tMin >= 0f) tMin else tMax
                if (hitT >= 0f && hitT < bestT) {
                    bestT = hitT
                    bestId = c.id
                }
            }
        }

        return bestId
    }

    /**
     * Performs a 3D ray-box intersection test (in millimetres) against all interior partitions
     * (vertical dividers and horizontal shelves) of all placed containers.
     * Returns [PartitionHit] if hit, or null if no partition intersects the ray.
     */
    fun pickPartitionByRayMm(
        rayOriginX: Float,
        rayOriginY: Float,
        rayOriginZ: Float,
        rayDirX: Float,
        rayDirY: Float,
        rayDirZ: Float,
        includeDoors: Boolean = true,
    ): PartitionHit? {
        if (containers.isEmpty()) return null
        val nw = vertexNW
        val ne = vertexNE
        val se = vertexSE
        val sw = vertexSW

        var bestHit: PartitionHit? = null
        var bestT = Float.MAX_VALUE

        for (raw in containers) {
            val c = clampContainer(raw)
            if (c.dividers.isEmpty() && c.shelves.isEmpty() && c.drawers.isEmpty() && c.clothesRails.isEmpty() && c.verticalPoles.isEmpty() && c.shoeRacks.isEmpty() && (c.cabinetDoors.isEmpty() || !includeDoors)) continue

            val (pStart, pEnd) = when (c.wall) {
                WallIndex.NORTH -> nw to ne
                WallIndex.EAST -> ne to se
                WallIndex.SOUTH -> se to sw
                WallIndex.WEST -> sw to nw
            }
            val dx = pEnd.x - pStart.x
            val dz = pEnd.z - pStart.z
            val wallLen = sqrt(dx * dx + dz * dz)
            if (wallLen < 10f) continue

            val ux = dx / wallLen
            val uz = dz / wallLen
            val nx = -uz
            val nz = ux

            val relX = rayOriginX - pStart.x
            val relZ = rayOriginZ - pStart.z
            val origS = relX * ux + relZ * uz
            val origY = rayOriginY
            val origD = relX * nx + relZ * nz

            val dirS = rayDirX * ux + rayDirZ * uz
            val dirY = rayDirY
            val dirD = rayDirX * nx + rayDirZ * nz

            val t = c.effectivePanelThicknessMm
            val plinthH = c.effectivePlinthHeightMm
            val backT = c.backThicknessMm.coerceIn(8f, 18f)
            val wallGap = 4f
            val totalH = c.heightMm
            val totalD = c.depthMm.coerceAtLeast(350f)
            val s0Inner = c.offsetMm + t

            fun intersectAABB(minS: Float, maxS: Float, minY: Float, maxY: Float, minD: Float, maxD: Float): Float? {
                var tMin = 0f
                var tMax = Float.MAX_VALUE

                fun clipSlab(orig: Float, dir: Float, bMin: Float, bMax: Float): Boolean {
                    if (abs(dir) < 1e-6f) {
                        return orig in bMin..bMax
                    }
                    val inv = 1f / dir
                    var t1 = (bMin - orig) * inv
                    var t2 = (bMax - orig) * inv
                    if (t1 > t2) {
                        val tmp = t1
                        t1 = t2
                        t2 = tmp
                    }
                    if (t1 > tMin) tMin = t1
                    if (t2 < tMax) tMax = t2
                    return tMin <= tMax
                }

                if (clipSlab(origS, dirS, minS, maxS) &&
                    clipSlab(origY, dirY, minY, maxY) &&
                    clipSlab(origD, dirD, minD, maxD)
                ) {
                    val hitT = if (tMin >= 0f) tMin else tMax
                    if (hitT >= 0f) return hitT
                }
                return null
            }

            // Test vertical dividers (expanded by 45mm in S for comfortable tap selection on touchscreen)
            for (div in c.dividers) {
                val divS0 = s0Inner + div.offsetXMm - 45f
                val divS1 = s0Inner + div.offsetXMm + div.thicknessMm + 45f
                val divY0 = plinthH + t + div.effectiveStartY() - 25f
                val divY1 = plinthH + t + div.effectiveEndY(c.interiorHeightMm) + 25f
                val divD0 = 0f
                val divD1 = wallGap + backT + div.effectiveDepth(c.interiorDepthMm) + 30f

                val hitT = intersectAABB(divS0, divS1, divY0, divY1, divD0, divD1)
                if (hitT != null && hitT < bestT) {
                    bestT = hitT
                    bestHit = PartitionHit(c.id, div.id, isVertical = true, hitDistance = hitT)
                }
            }

            // Test horizontal shelves (expanded by 45mm in Y for comfortable tap selection on touchscreen)
            val bays = c.getBays()
            for (shelf in c.shelves) {
                val bay = bays.getOrElse(shelf.bayIndex) { bays.last() }
                val shelfS0 = s0Inner + bay.startXMm - 20f
                val shelfS1 = s0Inner + bay.endXMm + 20f
                val shelfY0 = plinthH + t + shelf.elevationYMm - 70f
                val shelfY1 = plinthH + t + shelf.elevationYMm + shelf.thicknessMm + 70f
                val shelfD0 = 0f
                val shelfD1 = wallGap + backT + shelf.effectiveDepth(c.interiorDepthMm) + 30f

                val hitT = intersectAABB(shelfS0, shelfS1, shelfY0, shelfY1, shelfD0, shelfD1)
                if (hitT != null && hitT < bestT) {
                    bestT = hitT
                    bestHit = PartitionHit(c.id, shelf.id, isVertical = false, hitDistance = hitT)
                }
            }

            // Test drawers (expanded by 30mm for comfortable tap selection)
            for (drawer in c.drawers) {
                val bay = bays.getOrElse(drawer.bayIndex) { bays.last() }
                val drawerS0 = s0Inner + bay.startXMm - 20f
                val drawerS1 = s0Inner + bay.endXMm + 20f
                val drawerY0 = plinthH + t + drawer.elevationYMm - 20f
                val drawerY1 = plinthH + t + drawer.elevationYMm + drawer.heightMm + 20f
                val drawerD0 = 0f
                val drawerD1 = wallGap + backT + drawer.effectiveDepth(c.interiorDepthMm) + 30f

                val hitT = intersectAABB(drawerS0, drawerS1, drawerY0, drawerY1, drawerD0, drawerD1)
                if (hitT != null && hitT < bestT) {
                    bestT = hitT
                    bestHit = PartitionHit(c.id, drawer.id, isVertical = false, hitDistance = hitT)
                }
            }

            // Test clothes rails (expanded in Y and D for easy touch picking)
            for (rail in c.clothesRails) {
                val bay = bays.getOrElse(rail.bayIndex) { bays.last() }
                val railS0 = s0Inner + bay.startXMm - 30f
                val railS1 = s0Inner + bay.endXMm + 30f
                val railY0 = plinthH + t + rail.elevationYMm - 60f
                val railY1 = plinthH + t + rail.elevationYMm + 60f
                val railDepth = rail.effectiveDepth(c.interiorDepthMm)
                val railD0 = (wallGap + backT + railDepth - 100f).coerceAtLeast(0f)
                val railD1 = wallGap + backT + railDepth + 100f

                val hitT = intersectAABB(railS0, railS1, railY0, railY1, railD0, railD1)
                if (hitT != null && hitT < bestT) {
                    bestT = hitT
                    bestHit = PartitionHit(c.id, rail.id, isVertical = false, hitDistance = hitT)
                }
            }

            // Test vertical poles (expanded in S and D for easy touch picking)
            for (pole in c.verticalPoles) {
                val bay = bays.getOrElse(pole.bayIndex) { bays.last() }
                val poleX = pole.effectiveX(bay.startXMm, bay.endXMm)
                val (compY0, compY1) = pole.effectiveYRange(0f, c.interiorHeightMm)
                val poleS0 = s0Inner + poleX - 45f
                val poleS1 = s0Inner + poleX + 45f
                val poleY0 = plinthH + t + compY0 - 15f
                val poleY1 = plinthH + t + compY1 + 15f
                val poleDepth = pole.effectiveDepth(c.interiorDepthMm)
                val poleD0 = (wallGap + backT + poleDepth - 100f).coerceAtLeast(0f)
                val poleD1 = wallGap + backT + poleDepth + 100f

                val hitT = intersectAABB(poleS0, poleS1, poleY0, poleY1, poleD0, poleD1)
                if (hitT != null && hitT < bestT) {
                    bestT = hitT
                    bestHit = PartitionHit(c.id, pole.id, isVertical = true, hitDistance = hitT)
                }
            }

            // Test shoe racks (expanded in Y and D for easy touch picking)
            for (rack in c.shoeRacks) {
                val bay = bays.getOrElse(rack.bayIndex) { bays.last() }
                val rackS0 = s0Inner + bay.startXMm - 20f
                val rackS1 = s0Inner + bay.endXMm + 20f
                val rackY0 = plinthH + t + rack.elevationYMm - 20f
                val rackY1 = plinthH + t + rack.elevationYMm + rack.totalVerticalSpanMm(c.interiorDepthMm) + 20f
                val rackD0 = 0f
                val rackD1 = wallGap + backT + rack.effectiveDepth(c.interiorDepthMm) + 30f

                val hitT = intersectAABB(rackS0, rackS1, rackY0, rackY1, rackD0, rackD1)
                if (hitT != null && hitT < bestT) {
                    bestT = hitT
                    bestHit = PartitionHit(c.id, rack.id, isVertical = false, hitDistance = hitT)
                }
            }

            // Test cabinet doors (when includeDoors is true)
            if (includeDoors) {
                for (doors in c.cabinetDoors) {
                    val bay = bays.getOrElse(doors.bayIndex) { bays.last() }
                    val doorsS0 = s0Inner + bay.startXMm - 10f
                    val doorsS1 = s0Inner + bay.endXMm + 10f
                    val doorsY0 = plinthH + t + doors.elevationYMm - 10f
                    val doorsY1 = plinthH + t + doors.elevationYMm + doors.heightMm + 10f
                    val totalD = c.depthMm
                    val doorsD0 = (wallGap + totalD - doors.thicknessMm - 20f).coerceAtLeast(0f)
                    val doorsD1 = wallGap + totalD + 40f

                    val hitT = intersectAABB(doorsS0, doorsS1, doorsY0, doorsY1, doorsD0, doorsD1)
                    if (hitT != null && hitT < bestT) {
                        bestT = hitT
                        bestHit = PartitionHit(c.id, doors.id, isVertical = false, hitDistance = hitT)
                    }
                }
            }
        }

        return bestHit
    }
}

@Serializable
data class PartitionHit(
    val containerId: String,
    val partitionId: String,
    val isVertical: Boolean,
    val hitDistance: Float,
)

@Serializable
enum class ClosetDimensionType {
    OVERALL_WIDTH,
    OVERALL_HEIGHT,
    BAY_WIDTH,
    DIVIDER_HEIGHT,
    COMPARTMENT_HEIGHT,
}

@Serializable
data class ClosetDimensionCallout(
    val type: ClosetDimensionType,
    val valueMm: Float,
    val startXLocalMm: Float,
    val startYLocalMm: Float,
    val startZLocalMm: Float,
    val endXLocalMm: Float,
    val endYLocalMm: Float,
    val endZLocalMm: Float,
    val bayIndex: Int? = null,
)

/**
 * Computes all dimension callout segments for [this] container in its local coordinates.
 * Segments correspond to:
 * - Overall exterior width (top front edge)
 * - Overall exterior height (right front edge)
 * - Clear height of vertical dividers
 * - Clear width of each bay (bottom shelf)
 * - Clear vertical height of each compartment within divided bays
 */
fun ClosetContainerSpec.getDimensionCallouts(): List<ClosetDimensionCallout> {
    val callouts = mutableListOf<ClosetDimensionCallout>()
    val t = effectivePanelThicknessMm
    val plinth = effectivePlinthHeightMm
    val w = widthMm
    val h = heightMm
    val d = depthMm

    if (w <= 10f || h <= 10f) return emptyList()

    // 1. Overall Width (top front edge)
    callouts.add(
        ClosetDimensionCallout(
            type = ClosetDimensionType.OVERALL_WIDTH,
            valueMm = w,
            startXLocalMm = 0f,
            startYLocalMm = h,
            startZLocalMm = d,
            endXLocalMm = w,
            endYLocalMm = h,
            endZLocalMm = d,
        )
    )

    // 2. Overall Height (right front edge)
    callouts.add(
        ClosetDimensionCallout(
            type = ClosetDimensionType.OVERALL_HEIGHT,
            valueMm = h,
            startXLocalMm = w,
            startYLocalMm = 0f,
            startZLocalMm = d,
            endXLocalMm = w,
            endYLocalMm = h,
            endZLocalMm = d,
        )
    )

    // 3. Dividers Height
    val sortedDividers = dividers.sortedBy { it.offsetXMm }
    val dividerFloorY = plinth + t
    val dividerTopY = h - t
    val dividerHeight = (dividerTopY - dividerFloorY).coerceAtLeast(0f)
    for (div in sortedDividers) {
        val divCenterX = t + div.offsetXMm + div.thicknessMm * 0.5f
        callouts.add(
            ClosetDimensionCallout(
                type = ClosetDimensionType.DIVIDER_HEIGHT,
                valueMm = dividerHeight,
                startXLocalMm = divCenterX,
                startYLocalMm = dividerFloorY,
                startZLocalMm = d,
                endXLocalMm = divCenterX,
                endYLocalMm = dividerTopY,
                endZLocalMm = d,
            )
        )
    }

    // 4. Bay Clear Widths and Compartment Heights
    val bays = getBays()
    for (bay in bays) {
        val bayStartX = t + bay.startXMm
        val bayEndX = t + bay.endXMm
        val floorY = plinth + t

        // Clear bay width along bottom shelf
        if (bay.widthMm > 10f) {
            callouts.add(
                ClosetDimensionCallout(
                    type = ClosetDimensionType.BAY_WIDTH,
                    valueMm = bay.widthMm,
                    startXLocalMm = bayStartX,
                    startYLocalMm = floorY,
                    startZLocalMm = d,
                    endXLocalMm = bayEndX,
                    endYLocalMm = floorY,
                    endZLocalMm = d,
                    bayIndex = bay.index,
                )
            )
        }

        // Vertical compartments within this bay
        val compartments = getBayCompartments(bay.index)
        if (compartments.size > 1) {
            val calloutX = bayStartX + 20f.coerceAtMost(bay.widthMm * 0.15f)
            for (comp in compartments) {
                val compBottomY = floorY + comp.bottomYMm
                val compTopY = floorY + comp.topYMm
                val compHeight = comp.topYMm - comp.bottomYMm
                if (compHeight > 10f) {
                    callouts.add(
                        ClosetDimensionCallout(
                            type = ClosetDimensionType.COMPARTMENT_HEIGHT,
                            valueMm = compHeight,
                            startXLocalMm = calloutX,
                            startYLocalMm = compBottomY,
                            startZLocalMm = d,
                            endXLocalMm = calloutX,
                            endYLocalMm = compTopY,
                            endZLocalMm = d,
                            bayIndex = bay.index,
                        )
                    )
                }
            }
        }
    }

    return callouts
}

/**
 * Transforms a local container point (xLocalMm, yLocalMm, zLocalMm) into room world coordinates (Vec3Mm).
 */
fun RoomSpec.containerLocalToWorldMm(
    container: ClosetContainerSpec,
    xLocalMm: Float,
    yLocalMm: Float,
    zLocalMm: Float = container.depthMm,
): Vec3Mm {
    val (pStart, pEnd) = when (container.wall) {
        WallIndex.NORTH -> vertexNW to vertexNE
        WallIndex.EAST -> vertexNE to vertexSE
        WallIndex.SOUTH -> vertexSE to vertexSW
        WallIndex.WEST -> vertexSW to vertexNW
    }
    val dx = pEnd.x - pStart.x
    val dz = pEnd.z - pStart.z
    val wallLen = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(10f)
    val ux = dx / wallLen
    val uz = dz / wallLen
    val nx = -uz
    val nz = ux

    val wallGap = 4f
    val s = container.offsetMm + xLocalMm
    val d = wallGap + zLocalMm

    val worldXMm = pStart.x + s * ux + d * nx
    val worldYMm = yLocalMm
    val worldZMm = pStart.z + s * uz + d * nz

    return Vec3Mm(worldXMm, worldYMm, worldZMm)
}
