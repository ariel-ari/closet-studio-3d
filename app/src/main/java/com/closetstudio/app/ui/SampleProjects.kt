package com.closetstudio.app.ui

import com.closetstudio.core.model.ClosetProject
import com.closetstudio.core.model.ModuleKind
import com.closetstudio.core.model.ModuleSpec
import com.closetstudio.core.model.RoomSpec

/**
 * Starting point for M1: a 2.4 m wall with two hanging bays, one shelf bay and a
 * filler that absorbs the leftover. Real projects will come from storage.
 */
fun sampleProject(): ClosetProject = ClosetProject(
    name = "Sample closet",
    room = RoomSpec(clearWidthMm = 2400f, clearHeightMm = 2400f, clearDepthMm = 600f),
    modules = listOf(
        ModuleSpec(id = "bay-hang-a", widthMm = 600f, heightMm = 2400f, hasHangingRod = true),
        ModuleSpec(id = "bay-shelves", widthMm = 900f, heightMm = 2400f, shelfCount = 5),
        ModuleSpec(id = "bay-hang-b", widthMm = 600f, heightMm = 2400f, shelfCount = 1, hasHangingRod = true),
        ModuleSpec(id = "filler-end", kind = ModuleKind.FILLER, widthMm = 100f, heightMm = 2400f),
    ),
)
