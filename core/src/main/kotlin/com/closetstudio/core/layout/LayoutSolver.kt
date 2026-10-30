package com.closetstudio.core.layout

import com.closetstudio.core.model.ModuleKind
import com.closetstudio.core.model.ModuleSpec

/** A module as the solver sees it: intent only, no position. */
data class ModuleRequest(
    val id: String,
    val kind: ModuleKind,
    val requestedWidthMm: Float,
)

/** A module resolved to its final slot along the run. */
data class Placement(
    val id: String,
    val kind: ModuleKind,
    val startXMm: Float,
    val widthMm: Float,
) {
    val endXMm: Float get() = startXMm + widthMm
}

data class LayoutResult(
    val placements: List<Placement>,
    val usedWidthMm: Float,
    val remainingWidthMm: Float,
    val overflowMm: Float,
    val squeezedFillers: Boolean,
) {
    val isOverflowing: Boolean get() = overflowMm > EPSILON
}

private const val EPSILON = 0.01f

/**
 * Places modules left to right inside [interiorWidthMm].
 *
 * Fillers absorb whatever space the carcasses leave, which is how a real run is
 * installed: the millimetres never divide evenly, so the last panel is cut to fit.
 * When there is not enough room, fillers collapse to zero and the remaining
 * overlap is reported instead of being silently clipped.
 *
 * Pure and deterministic: same input, same placements, no mutation, so it is
 * cheap to test and safe to call from a Compose recomposition.
 */
object LayoutSolver {

    fun solve(interiorWidthMm: Float, requests: List<ModuleRequest>): LayoutResult {
        require(interiorWidthMm > 0f) { "interior width must be positive, got $interiorWidthMm" }

        val carcasses = requests.filter { it.kind == ModuleKind.CARCASS }
        val fillers = requests.filter { it.kind == ModuleKind.FILLER }
        val fixedWidth = carcasses.sumOf { it.requestedWidthMm.toDouble() }.toFloat()
        val available = interiorWidthMm - fixedWidth

        val widths = HashMap<String, Float>(requests.size)
        var squeezed = false

        if (fillers.isEmpty()) {
            requests.forEach { widths[it.id] = it.requestedWidthMm }
        } else if (available <= 0f) {
            squeezed = true
            requests.forEach { widths[it.id] = it.requestedWidthMm }
            fillers.forEach { widths[it.id] = 0f }
        } else {
            val weights = fillers.map { it.requestedWidthMm.coerceAtLeast(0f) }
            val totalWeight = weights.sum()
            var distributed = 0f
            fillers.forEachIndexed { index, filler ->
                val share = if (totalWeight <= 0f) {
                    // No preference: split evenly.
                    if (index == fillers.lastIndex) available - distributed else available / fillers.size
                } else {
                    if (index == fillers.lastIndex) {
                        available - distributed
                    } else {
                        available * (weights[index] / totalWeight)
                    }
                }
                distributed += share
                widths[filler.id] = share.coerceAtLeast(0f)
            }
            requests.forEach { widths.putIfAbsent(it.id, it.requestedWidthMm) }
        }

        var cursor = 0f
        val placements = ArrayList<Placement>(requests.size)
        for (request in requests) {
            val width = widths[request.id] ?: request.requestedWidthMm
            placements += Placement(request.id, request.kind, cursor, width)
            cursor += width
        }

        val used = cursor
        // Only carcasses that had to push past the wall count as overflow.
        val overflow = (used - interiorWidthMm).coerceAtLeast(0f)
        val remaining = if (fillers.isEmpty()) (interiorWidthMm - used).coerceAtLeast(0f) else 0f

        return LayoutResult(
            placements = placements,
            usedWidthMm = used,
            remainingWidthMm = remaining,
            overflowMm = overflow,
            squeezedFillers = squeezed,
        )
    }

    @JvmName("solveModules")
    fun solve(interiorWidthMm: Float, modules: List<ModuleSpec>): LayoutResult = solve(
        interiorWidthMm,
        modules.map { ModuleRequest(it.id, it.kind, it.widthMm) },
    )
}
