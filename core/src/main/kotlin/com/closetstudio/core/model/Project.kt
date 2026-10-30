package com.closetstudio.core.model

import kotlinx.serialization.Serializable

enum class WallIndex(val degrees: Float, val label: String) {
    NORTH(0f, "Pared Norte (Frente)"),
    EAST(90f, "Pared Este (Derecha)"),
    SOUTH(180f, "Pared Sur (Fondo / Puerta)"),
    WEST(270f, "Pared Oeste (Izquierda)");

    fun next(): WallIndex = values()[(ordinal + 1) % values().size]
    fun prev(): WallIndex = values()[(ordinal - 1 + values().size) % values().size]
}

@Serializable
data class DoorSpec(
    val wall: WallIndex = WallIndex.SOUTH,
    val offsetMm: Float = 1350f,
    val widthMm: Float = 900f,
    val heightMm: Float = 2100f,
    val frameThicknessMm: Float = 40f,
) {
    init {
        require(offsetMm >= 0f) { "door offset must be non-negative" }
        require(widthMm > 0f) { "door width must be positive" }
        require(heightMm > 0f) { "door height must be positive" }
    }
}

/**
 * Architectural specification of a 4-walled enclosed room.
 *
 * All coordinates in millimetres:
 * - clearWidthMm: X dimension (West to East)
 * - clearHeightMm: Y dimension (Floor to Ceiling)
 * - clearDepthMm: Z dimension (North to South)
 */
@Serializable
data class RoomSpec(
    val clearWidthMm: Float = 3600f,
    val clearHeightMm: Float = 2600f,
    val clearDepthMm: Float = 3600f,
    val door: DoorSpec = DoorSpec(),
) {
    init {
        require(clearWidthMm >= 500f) { "Room width must be at least 500 mm, got $clearWidthMm" }
        require(clearHeightMm >= 1000f) { "Room height must be at least 1000 mm, got $clearHeightMm" }
        require(clearDepthMm >= 300f) { "Room depth must be at least 300 mm, got $clearDepthMm" }
    }

    /** Maximum offset allowed for the door on the specified wall. */
    fun maxDoorOffset(): Float {
        val wallLength = if (door.wall == WallIndex.NORTH || door.wall == WallIndex.SOUTH) {
            clearWidthMm
        } else {
            clearDepthMm
        }
        return (wallLength - door.widthMm).coerceAtLeast(0f)
    }
}
