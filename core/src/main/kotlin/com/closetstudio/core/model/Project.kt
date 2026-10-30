package com.closetstudio.core.model

import kotlinx.serialization.Serializable

/**
 * Room opening where the closet goes. All dimensions in millimetres; the render
 * layer is the only place that converts to metres.
 */
@Serializable
data class RoomSpec(
    val clearWidthMm: Float = 2400f,
    val clearHeightMm: Float = 2400f,
    val clearDepthMm: Float = 600f,
) {
    init {
        require(clearWidthMm > 0f) { "room width must be positive" }
        require(clearHeightMm > 0f) { "room height must be positive" }
        require(clearDepthMm > 0f) { "room depth must be positive" }
    }
}

/** Board stock and cabinet construction parameters. */
@Serializable
data class PanelSpec(
    val thicknessMm: Float = 18f,
    val backThicknessMm: Float = 4f,
    val toeKickHeightMm: Float = 100f,
    val toeKickInsetMm: Float = 50f,
) {
    init {
        require(thicknessMm > 0f) { "panel thickness must be positive" }
        require(backThicknessMm > 0f) { "back thickness must be positive" }
        require(toeKickHeightMm >= 0f) { "toe kick height cannot be negative" }
    }
}

/**
 * Colours are packed ARGB values, so they round-trip through JSON as plain
 * integers and convert directly to Filament colours at the render boundary.
 */
@Serializable
data class FinishSpec(
    val carcassColor: Long = 0xFFF0EDE7,
    val frontColor: Long = 0xFFE3DED4,
    val backColor: Long = 0xFFFAF8F5,
    val toeKickColor: Long = 0xFFB9B2A6,
    /** Physical size in millimetres of one texture tile. */
    val uvScaleMm: Float = 600f,
)

@Serializable
enum class ModuleKind {
    /** Structural box that holds shelves or a rod. */
    CARCASS,
    /** Blank infill panel that absorbs leftover width at the end of a run. */
    FILLER,
}

@Serializable
data class ModuleSpec(
    val id: String,
    val kind: ModuleKind = ModuleKind.CARCASS,
    val widthMm: Float,
    val heightMm: Float,
    val shelfCount: Int = 0,
    val hasHangingRod: Boolean = false,
) {
    init {
        require(widthMm >= 0f) { "module width cannot be negative" }
        require(heightMm > 0f) { "module height must be positive" }
        require(shelfCount >= 0) { "shelf count cannot be negative" }
    }
}

@Serializable
data class ClosetProject(
    val name: String = "Untitled closet",
    val room: RoomSpec = RoomSpec(),
    val panels: PanelSpec = PanelSpec(),
    val finish: FinishSpec = FinishSpec(),
    val modules: List<ModuleSpec> = emptyList(),
    val schemaVersion: Int = 1,
)
