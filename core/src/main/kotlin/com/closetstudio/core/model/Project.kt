package com.closetstudio.core.model

import kotlinx.serialization.Serializable

/**
 * Dimensions and architectural boundary of a room.
 *
 * All coordinates in millimetres:
 * - clearWidthMm: Total opening width (X)
 * - clearHeightMm: Ceiling height from finished floor (Y)
 * - clearDepthMm: Usable depth of the room or closet niche (Z)
 */
@Serializable
data class RoomSpec(
    val clearWidthMm: Float = 2400f,
    val clearHeightMm: Float = 2400f,
    val clearDepthMm: Float = 600f,
) {
    init {
        require(clearWidthMm > 0f) { "Room width must be positive, got $clearWidthMm" }
        require(clearHeightMm > 0f) { "Room height must be positive, got $clearHeightMm" }
        require(clearDepthMm > 0f) { "Room depth must be positive, got $clearDepthMm" }
    }
}
