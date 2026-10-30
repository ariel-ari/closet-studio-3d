package com.closetstudio.core.geometry

import com.closetstudio.core.layout.LayoutResult
import com.closetstudio.core.model.ModuleKind
import com.closetstudio.core.model.ModuleSpec
import com.closetstudio.core.model.PanelSpec
import com.closetstudio.core.model.RoomSpec

/**
 * Interior of a solved run: one vertical divider per carcass plus its shelves.
 *
 * The divider of a module is drawn on its right edge and belongs to that module,
 * so a module's usable bay is exactly `[start + thickness, end - thickness]`.
 * The first bay borrows the shell's left wall and the last one its right wall,
 * which is why no extra end panels are emitted.
 */
object ClosetInterior {

    fun build(
        room: RoomSpec,
        panels: PanelSpec,
        layout: LayoutResult,
        modules: List<ModuleSpec>,
        uvScaleMm: Float = 600f,
    ): MeshData? {
        val t = panels.thicknessMm
        val depth = room.clearDepthMm
        val floor = panels.toeKickHeightMm + t
        val ceiling = room.clearHeightMm - t
        require(ceiling - floor > 2f * t) { "not enough vertical room for shelves" }

        val byId = modules.associateBy { it.id }
        val builder = MeshBuilder(uvScaleMm)
        var emitted = false

        for (placement in layout.placements) {
            if (placement.kind != ModuleKind.CARCASS) continue
            val spec = byId[placement.id] ?: continue

            // Right divider, shared with the next bay.
            builder.addBox(
                Vec3(placement.endXMm - t, floor, 0f),
                Vec3(placement.endXMm, ceiling, depth),
            )
            emitted = true

            val bayStart = placement.startXMm + t
            val bayEnd = placement.endXMm - t
            if (bayEnd - bayStart <= t) continue

            for (index in 1..spec.shelfCount) {
                val y = floor + (ceiling - floor) * index / (spec.shelfCount + 1)
                builder.addBox(
                    Vec3(bayStart, y, 0f),
                    Vec3(bayEnd, y + t, depth - 10f),
                )
            }
        }

        return if (emitted) builder.build() else null
    }
}
