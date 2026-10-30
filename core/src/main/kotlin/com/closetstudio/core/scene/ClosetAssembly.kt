package com.closetstudio.core.scene

import com.closetstudio.core.geometry.ClosetInterior
import com.closetstudio.core.geometry.ClosetShell
import com.closetstudio.core.geometry.Part
import com.closetstudio.core.geometry.Surface
import com.closetstudio.core.layout.LayoutResult
import com.closetstudio.core.layout.LayoutSolver
import com.closetstudio.core.model.ClosetProject

/** Everything the renderer needs for one project, in the millimetre domain. */
data class Assembly(
    val parts: List<Part>,
    val layout: LayoutResult,
)

/**
 * Turns a project into renderable parts. Pure: the same project always produces
 * the same meshes, so it can be asserted in unit tests and memoised by the UI on
 * the project value.
 */
object ClosetAssembly {

    fun build(project: ClosetProject): Assembly {
        val shell = ClosetShell.build(
            room = project.room,
            panels = project.panels,
            uvScaleMm = project.finish.uvScaleMm,
        )
        val layout = LayoutSolver.solve(project.room.clearWidthMm, project.modules)
        val interior = ClosetInterior.build(
            room = project.room,
            panels = project.panels,
            layout = layout,
            modules = project.modules,
            uvScaleMm = project.finish.uvScaleMm,
        )

        val parts = buildList {
            addAll(shell.parts)
            if (interior != null) add(Part(Surface.CARCASS, interior))
        }
        return Assembly(parts = parts, layout = layout)
    }
}
